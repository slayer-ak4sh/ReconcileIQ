package com.reconcileiq.dashboard;

import com.reconcileiq.matching.OrderReconciliationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DashboardService {

    private final OrderReconciliationRepository reconRepository;

    public DashboardService(OrderReconciliationRepository reconRepository) {
        this.reconRepository = reconRepository;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getSummary(LocalDate asOfDate) {
        if (asOfDate == null) asOfDate = LocalDate.now();

        List<com.reconcileiq.matching.OrderReconciliation> all = reconRepository.findAll();
        long totalOrders = all.size();
        long matched = all.stream().filter(r -> Boolean.TRUE.equals(r.getIsMatched())).count();
        double autoMatchRate = totalOrders > 0 ? (double) matched / totalOrders * 100 : 0;

        final LocalDate finalDate = asOfDate;
        BigDecimal moneyAtRisk = all.stream()
                .filter(r -> ("SHORT_PAID".equals(r.getReconciliationStatus())
                           || "UNPAID_PAST_DUE".equals(r.getReconciliationStatus()))
                        && r.getDueDate() != null && !r.getDueDate().isAfter(finalDate))
                .map(r -> r.getSettlementDiff() != null ? r.getSettlementDiff().abs() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, Long> byStatus = new LinkedHashMap<>();
        for (com.reconcileiq.matching.OrderReconciliation r : all) {
            byStatus.merge(r.getReconciliationStatus(), 1L, Long::sum);
        }

        List<Map<String, Object>> buckets = getMoneyAtRiskByPartnerAndAge(asOfDate);
        Map<String, Object> byPartner = new LinkedHashMap<>();
        for (Map<String, Object> b : buckets) {
            byPartner.put((String) b.get("partnerCode"), b);
        }
        Map<String, Object> byAgeBucket = getAgeBucketSummary(buckets);

        return Map.of(
                "totalOrders",   totalOrders,
                "autoMatched",   matched,
                "autoMatchRate", BigDecimal.valueOf(autoMatchRate).setScale(2, RoundingMode.HALF_UP),
                "moneyAtRisk",   moneyAtRisk,
                "totalLeakage",  moneyAtRisk,
                "byStatus",      byStatus,
                "byPartner",     byPartner,
                "byAgeBucket",   byAgeBucket
        );
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getMoneyAtRiskByPartnerAndAge(LocalDate asOfDate) {
        if (asOfDate == null) asOfDate = LocalDate.now();

        // Use JPA to stay H2-compatible in tests
        final LocalDate finalDate = asOfDate;
        List<com.reconcileiq.matching.OrderReconciliation> atRisk = reconRepository.findAll().stream()
                .filter(r -> ("SHORT_PAID".equals(r.getReconciliationStatus())
                           || "UNPAID_PAST_DUE".equals(r.getReconciliationStatus()))
                        && r.getDueDate() != null && !r.getDueDate().isAfter(finalDate))
                .collect(java.util.stream.Collectors.toList());

        // Group by order_id prefix as a proxy for partner (real impl would join orders)
        Map<String, List<com.reconcileiq.matching.OrderReconciliation>> byPartnerMap = new java.util.LinkedHashMap<>();
        for (com.reconcileiq.matching.OrderReconciliation r : atRisk) {
            String key = r.getOrderId();
            byPartnerMap.computeIfAbsent(key, k -> new java.util.ArrayList<>()).add(r);
        }

        List<Map<String, Object>> result = new java.util.ArrayList<>();
        for (Map.Entry<String, List<com.reconcileiq.matching.OrderReconciliation>> entry : byPartnerMap.entrySet()) {
            BigDecimal b0to7 = BigDecimal.ZERO, b8to15 = BigDecimal.ZERO,
                       b16to30 = BigDecimal.ZERO, b31plus = BigDecimal.ZERO, total = BigDecimal.ZERO;
            for (com.reconcileiq.matching.OrderReconciliation r : entry.getValue()) {
                long days = java.time.temporal.ChronoUnit.DAYS.between(r.getDueDate(), finalDate);
                BigDecimal amt = r.getSettlementDiff() != null ? r.getSettlementDiff().abs() : BigDecimal.ZERO;
                total = total.add(amt);
                if (days <= 7)       b0to7   = b0to7.add(amt);
                else if (days <= 15) b8to15  = b8to15.add(amt);
                else if (days <= 30) b16to30 = b16to30.add(amt);
                else                 b31plus = b31plus.add(amt);
            }
            Map<String, Object> row = new java.util.LinkedHashMap<>();
            row.put("partnerCode", entry.getKey());
            row.put("partnerType", "CHANNEL");
            row.put("bucket0to7",   b0to7);
            row.put("bucket8to15",  b8to15);
            row.put("bucket16to30", b16to30);
            row.put("bucket31plus", b31plus);
            row.put("total",        total);
            result.add(row);
        }
        result.sort((a, b) -> ((BigDecimal) b.get("total")).compareTo((BigDecimal) a.get("total")));
        return result;
    }

    public Map<String, Object> getAgeBucketSummary(List<Map<String, Object>> buckets) {
        Map<String, Object> summary = new LinkedHashMap<>();
        BigDecimal b0to7 = BigDecimal.ZERO;
        BigDecimal b8to15 = BigDecimal.ZERO;
        BigDecimal b16to30 = BigDecimal.ZERO;
        BigDecimal b31plus = BigDecimal.ZERO;
        for (Map<String, Object> bucket : buckets) {
            b0to7 = b0to7.add((BigDecimal) bucket.get("bucket0to7"));
            b8to15 = b8to15.add((BigDecimal) bucket.get("bucket8to15"));
            b16to30 = b16to30.add((BigDecimal) bucket.get("bucket16to30"));
            b31plus = b31plus.add((BigDecimal) bucket.get("bucket31plus"));
        }
        summary.put("0to7", b0to7);
        summary.put("8to15", b8to15);
        summary.put("16to30", b16to30);
        summary.put("31plus", b31plus);
        return summary;
    }
}