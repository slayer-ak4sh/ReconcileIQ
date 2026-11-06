package com.reconcileiq.mapping;

import com.reconcileiq.ingestion.CanonicalField;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class RuleBasedSuggesterTest {

    @Autowired
    private RuleBasedSuggester suggester;

    @Test
    void testExactSynonymMatchForOrders() {
        List<String> headers = List.of("order_id", "channel", "order_date", "order_value", "payment_type", "delivery_status");
        List<Map<String, String>> sampleRows = List.of(
                Map.of("order_id", "ORD-001", "channel", "Website", "order_date", "2024-01-15", "order_value", "1499", "payment_type", "COD", "delivery_status", "Delivered")
        );

        List<MappingSuggester.Suggestion> suggestions = suggester.suggest(headers, sampleRows);

        assertEquals(6, suggestions.size());
        assertEquals("orderId", findSuggestion(suggestions, "order_id").canonicalField);
        assertEquals("channelCode", findSuggestion(suggestions, "channel").canonicalField);
        assertEquals("orderDate", findSuggestion(suggestions, "order_date").canonicalField);
        assertEquals("orderValue", findSuggestion(suggestions, "order_value").canonicalField);
        assertEquals("paymentType", findSuggestion(suggestions, "payment_type").canonicalField);
        assertEquals("deliveryStatus", findSuggestion(suggestions, "delivery_status").canonicalField);

        // All should have high confidence (1.0 for exact match)
        for (MappingSuggester.Suggestion s : suggestions) {
            assertEquals(1.0, s.confidence, 0.01, "Header: " + s.header);
        }
    }

    @Test
    void testExactSynonymMatchForSettlements() {
        List<String> headers = List.of("reference_no", "settlement_date", "gross_amount", "commission", "fixed_fee", "collection_fee", "tcs_amount", "tds_amount", "net_amount");
        List<Map<String, String>> sampleRows = List.of(
                Map.of("reference_no", "REF-001", "settlement_date", "2024-01-20", "gross_amount", "1499", "commission", "89.94", "fixed_fee", "10", "collection_fee", "20", "tcs_amount", "15", "tds_amount", "5", "net_amount", "1359.06")
        );

        List<MappingSuggester.Suggestion> suggestions = suggester.suggest(headers, sampleRows);

        assertEquals(9, suggestions.size());
        assertEquals("referenceNo", findSuggestion(suggestions, "reference_no").canonicalField);
        assertEquals("settlementDate", findSuggestion(suggestions, "settlement_date").canonicalField);
        assertEquals("grossAmount", findSuggestion(suggestions, "gross_amount").canonicalField);
        assertEquals("commission", findSuggestion(suggestions, "commission").canonicalField);
        assertEquals("fixedFee", findSuggestion(suggestions, "fixed_fee").canonicalField);
        assertEquals("collectionFee", findSuggestion(suggestions, "collection_fee").canonicalField);
        assertEquals("tcsAmount", findSuggestion(suggestions, "tcs_amount").canonicalField);
        assertEquals("tdsAmount", findSuggestion(suggestions, "tds_amount").canonicalField);
        // net_amount is in synonyms for netAmount
        assertEquals("netAmount", findSuggestion(suggestions, "net_amount").canonicalField);

        for (MappingSuggester.Suggestion s : suggestions) {
            assertEquals(1.0, s.confidence, 0.01, "Header: " + s.header);
        }
    }

    @Test
    void testFuzzyMatchWithJaroWinkler() {
        // Headers with slight variations
        List<String> headers = List.of("OrderID", "Chanel", "OrderDate", "Ordervalue", "PayType", "DelStatus");
        List<Map<String, String>> sampleRows = List.of(
                Map.of("OrderID", "ORD-001", "Chanel", "Website", "OrderDate", "2024-01-15", "Ordervalue", "1499", "PayType", "COD", "DelStatus", "Delivered")
        );

        List<MappingSuggester.Suggestion> suggestions = suggester.suggest(headers, sampleRows);

        assertEquals(6, suggestions.size());
        // Should still match with high confidence due to Jaro-Winkler
        assertEquals("orderId", findSuggestion(suggestions, "OrderID").canonicalField);
        assertEquals("channelCode", findSuggestion(suggestions, "Chanel").canonicalField);
        assertEquals("orderDate", findSuggestion(suggestions, "OrderDate").canonicalField);
        assertEquals("orderValue", findSuggestion(suggestions, "Ordervalue").canonicalField);
        assertEquals("paymentType", findSuggestion(suggestions, "PayType").canonicalField);
        assertEquals("deliveryStatus", findSuggestion(suggestions, "DelStatus").canonicalField);

        for (MappingSuggester.Suggestion s : suggestions) {
            assertTrue(s.confidence > 0.8, "Header: " + s.header + " confidence: " + s.confidence);
        }
    }

    @Test
    void testDataTypeInference() {
        // Header with no synonym match but numeric data
        List<String> headers = List.of("TotalAmount", "CustName", "PhoneNum", "BuyerEmail");
        List<Map<String, String>> sampleRows = List.of(
                Map.of("TotalAmount", "1499.50", "CustName", "John Doe", "PhoneNum", "9876543210", "BuyerEmail", "john@example.com"),
                Map.of("TotalAmount", "2398.00", "CustName", "Jane Smith", "PhoneNum", "8765432109", "BuyerEmail", "jane@example.com")
        );

        List<MappingSuggester.Suggestion> suggestions = suggester.suggest(headers, sampleRows);

        assertEquals(4, suggestions.size());
        // TotalAmount should be inferred as numeric -> first numeric canonical field (grossAmount)
        assertEquals("grossAmount", findSuggestion(suggestions, "TotalAmount").canonicalField);
        // CustName should match customerName via fuzzy
        assertEquals("customerName", findSuggestion(suggestions, "CustName").canonicalField);
        // PhoneNum should be inferred as phone -> customerPhone
        assertEquals("customerPhone", findSuggestion(suggestions, "PhoneNum").canonicalField);
        // BuyerEmail should be inferred as email -> customerName
        assertEquals("customerName", findSuggestion(suggestions, "BuyerEmail").canonicalField);
    }

    private MappingSuggester.Suggestion findSuggestion(List<MappingSuggester.Suggestion> suggestions, String header) {
        return suggestions.stream()
                .filter(s -> s.header.equals(header))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No suggestion for header: " + header));
    }
}