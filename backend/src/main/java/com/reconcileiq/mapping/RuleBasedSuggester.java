package com.reconcileiq.mapping;

import com.reconcileiq.ingestion.CanonicalField;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class RuleBasedSuggester implements MappingSuggester {

    // Synonym dictionary: canonical field -> list of common partner header variations
    // Order matters - more specific terms first
    private static final Map<String, List<String>> SYNONYMS = new LinkedHashMap<>() {{
        // Orders
        put("orderId", List.of("order_id", "orderid", "orderno", "order_no", "order number", "id", "ref", "reference", "transaction_id"));
        put("channelCode", List.of("channel", "channel_code", "source", "platform", "marketplace", "site"));
        put("orderDate", List.of("order_date", "orderdate", "date", "created_at", "created", "purchase_date", "order_time"));
        put("orderValue", List.of("order_value", "ordervalue", "amount", "total", "order_total", "value", "grand_total", "price"));
        put("paymentType", List.of("payment_type", "paymenttype", "payment_method", "pay_type", "method", "paymode"));
        put("deliveryStatus", List.of("delivery_status", "deliverystatus", "status", "order_status", "state", "shipment_status"));
        put("customerName", List.of("customer_name", "customername", "name", "buyer_name", "buyer", "customer"));
        put("customerPhone", List.of("customer_phone", "customerphone", "phone", "mobile", "contact", "phone_number", "buyer_phone"));
        put("customerAddress", List.of("customer_address", "customeraddress", "address", "shipping_address", "bill_to", "buyer_address"));

        // Settlements - specific to settlements
        put("referenceNo", List.of("reference_no", "referenceno", "ref_no", "refno", "settlement_ref", "txn_id", "transaction_id"));
        put("settlementDate", List.of("settlement_date", "settlementdate", "settled_date", "settled", "credit_date", "payout_date"));
        put("grossAmount", List.of("gross_amount", "grossamount", "gross", "total_amount", "invoice_amount", "bill_amount"));
        put("commission", List.of("commission", "comm", "fee", "platform_fee", "marketplace_fee", "commission_amount"));
        put("fixedFee", List.of("fixed_fee", "fixedfee", "fixed_charge", "per_order_fee", "processing_fee"));
        put("collectionFee", List.of("collection_fee", "collectionfee", "collection_charge", "payment_gateway_fee", "pg_fee", "gateway_fee"));
        put("tcsAmount", List.of("tcs_amount", "tcsamount", "tcs", "tax_collected", "gst_tcs"));
        put("tdsAmount", List.of("tds_amount", "tdsamount", "tds", "tax_deducted", "income_tax_tds"));
        put("netAmount", List.of("net_amount", "netamount", "net", "settled_amount", "payout", "credit_amount"));

        // COD Remittances
        put("courierCode", List.of("courier_code", "couriercode", "courier", "carrier", "shipping_partner", "logistics"));
        put("utrNo", List.of("utr_no", "utrno", "utr", "bank_ref", "transaction_ref"));
        put("remittanceDate", List.of("remittance_date", "remittancedate", "credited_date", "credit_date", "settlement_date", "date"));
        put("codCollected", List.of("cod_collected", "codcollected", "cod_amount", "cash_collected", "collected_amount"));
        put("feeDeducted", List.of("fee_deducted", "feededucted", "deduction", "charges", "courier_fee", "fee"));
        put("netRemitted", List.of("net_remitted", "netremitted", "remitted", "received_amount", "payout_amount"));

        // Freight Invoices
        put("invoiceNo", List.of("invoice_no", "invoiceno", "invoice", "bill_no", "billno", "document_no", "freight_bill"));
        put("invoiceDate", List.of("invoice_date", "invoicedate", "bill_date", "billing_date", "date"));
        put("billedWeight", List.of("billed_weight", "billedweight", "weight", "chargeable_weight", "bill_weight", "kg"));
        put("freightCharge", List.of("freight_charge", "freightcharge", "freight", "shipping_charge", "delivery_charge", "transport"));
        put("codFee", List.of("cod_fee", "codfee", "cod_charge", "cod_handling"));
        put("returnFee", List.of("return_fee", "returnfee", "return_charge", "rto_fee", "reverse_logistics"));
        put("totalFreight", List.of("total_freight", "totalfreight", "total", "amount", "grand_total", "freight_total"));

        // Warehouse Scans
        put("waybillNo", List.of("waybill_no", "waybillno", "waybill", "awb", "awb_no", "tracking_no", "tracking"));
        put("scanDate", List.of("scan_date", "scandate", "received_date", "delivery_date", "warehouse_date", "date"));
        put("warehouseLocation", List.of("warehouse_location", "warehouselocation", "warehouse", "location", "branch", "godown"));
        put("conditionStatus", List.of("condition_status", "conditionstatus", "condition", "status", "state", "quality"));
    }};

    @Override
    public List<Suggestion> suggest(List<String> headers, List<Map<String, String>> sampleRows) {
        List<Suggestion> suggestions = new ArrayList<>();

        for (String header : headers) {
            String normalizedHeader = normalize(header);
            Suggestion best = findBestMatch(normalizedHeader, header, sampleRows);
            suggestions.add(best);
        }

        return suggestions;
    }

    private String normalize(String s) {
        return s.toLowerCase()
                .replaceAll("[^a-z0-9]", "_")
                .replaceAll("_+", "_")
                .replaceAll("^_|_$", "");
    }

    private Suggestion findBestMatch(String normalizedHeader, String originalHeader, List<Map<String, String>> sampleRows) {
        double bestScore = 0.0;
        String bestField = null;
        String bestReason = "";

        // 1. Exact synonym match
        for (Map.Entry<String, List<String>> entry : SYNONYMS.entrySet()) {
            String canonicalKey = entry.getKey();
            List<String> synonyms = entry.getValue();

            if (synonyms.stream().anyMatch(s -> normalize(s).equals(normalizedHeader))) {
                return new Suggestion(originalHeader, canonicalKey, 1.0, "Exact header match in synonym dictionary");
            }
        }

        // 2. Jaro-Winkler similarity against synonyms
        for (Map.Entry<String, List<String>> entry : SYNONYMS.entrySet()) {
            String canonicalKey = entry.getKey();
            for (String synonym : entry.getValue()) {
                double score = jaroWinkler(normalizedHeader, normalize(synonym));
                if (score > bestScore) {
                    bestScore = score;
                    bestField = canonicalKey;
                    bestReason = "Fuzzy match to '" + synonym + "' (Jaro-Winkler: " + String.format("%.2f", score) + ")";
                }
            }
        }

        // 3. Data-type sniffing on sample values
        if (sampleRows != null && !sampleRows.isEmpty() && bestScore < 0.85) {
            String inferredType = inferTypeFromSamples(sampleRows, normalizedHeader);
            String typeMatch = findByType(inferredType);
            if (typeMatch != null) {
                double typeScore = 0.7;
                if (typeScore > bestScore) {
                    bestScore = typeScore;
                    bestField = typeMatch;
                    bestReason = "Data type inference: " + inferredType;
                }
            }
        }

        if (bestField == null || bestScore < 0.5) {
            return new Suggestion(originalHeader, "", 0.0, "No confident match found");
        }

        return new Suggestion(originalHeader, bestField, Math.min(bestScore, 0.99), bestReason);
    }

    private String inferTypeFromSamples(List<Map<String, String>> sampleRows, String header) {
        int nonEmpty = 0, numeric = 0, dateLike = 0, emailLike = 0, phoneLike = 0;
        for (Map<String, String> row : sampleRows) {
            String val = row.get(header);
            if (val != null && !val.trim().isEmpty()) {
                nonEmpty++;
                if (val.matches("^[0-9]+(\\.[0-9]+)?$")) numeric++;
                else if (val.matches("^\\d{4}-\\d{2}-\\d{2}") || val.matches("^\\d{2}[-/]\\d{2}[-/]\\d{4}")) dateLike++;
                else if (val.contains("@")) emailLike++;
                else if (val.matches("^[+]?[0-9\\-\\s]{10,}$")) phoneLike++;
            }
        }
        if (nonEmpty == 0) return "unknown";
        if (numeric * 1.0 / nonEmpty > 0.7) return "numeric";
        if (dateLike * 1.0 / nonEmpty > 0.7) return "date";
        if (emailLike * 1.0 / nonEmpty > 0.5) return "email";
        if (phoneLike * 1.0 / nonEmpty > 0.5) return "phone";
        return "string";
    }

    private String findByType(String type) {
        return switch (type) {
            case "numeric" -> "grossAmount"; // default numeric field
            case "date" -> "orderDate";
            case "email" -> "customerName"; // emails usually in customer context
            case "phone" -> "customerPhone";
            default -> null;
        };
    }

    // Jaro-Winkler implementation
    private double jaroWinkler(String s1, String s2) {
        double jaro = jaro(s1, s2);
        if (jaro < 0.7) return jaro;

        int prefix = 0;
        int minLen = Math.min(Math.min(s1.length(), s2.length()), 4);
        for (int i = 0; i < minLen; i++) {
            if (s1.charAt(i) == s2.charAt(i)) prefix++;
            else break;
        }
        return jaro + 0.1 * prefix * (1 - jaro);
    }

    private double jaro(String s1, String s2) {
        if (s1.isEmpty() || s2.isEmpty()) return 0.0;
        int matchWindow = Math.max(s1.length(), s2.length()) / 2 - 1;
        if (matchWindow < 0) matchWindow = 0;

        boolean[] s1Matches = new boolean[s1.length()];
        boolean[] s2Matches = new boolean[s2.length()];
        int matches = 0, transpositions = 0;

        for (int i = 0; i < s1.length(); i++) {
            int start = Math.max(0, i - matchWindow);
            int end = Math.min(i + matchWindow + 1, s2.length());
            for (int j = start; j < end; j++) {
                if (!s2Matches[j] && s1.charAt(i) == s2.charAt(j)) {
                    s1Matches[i] = s2Matches[j] = true;
                    matches++;
                    break;
                }
            }
        }

        if (matches == 0) return 0.0;

        int k = 0;
        for (int i = 0; i < s1.length(); i++) {
            if (s1Matches[i]) {
                while (!s2Matches[k]) k++;
                if (s1.charAt(i) != s2.charAt(k)) transpositions++;
                k++;
            }
        }

        double m = matches;
        return (m / s1.length() + m / s2.length() + (m - transpositions / 2.0) / m) / 3.0;
    }
}