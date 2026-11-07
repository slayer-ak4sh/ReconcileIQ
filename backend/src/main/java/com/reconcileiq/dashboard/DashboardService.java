package com.reconcileiq.dashboard;

import com.reconcileiq.matching.OrderReconciliation;
import com.reconcileiq.matching.OrderReconciliationRepository;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DashboardService {

    private final OrderReconciliationRepository reconRepository;

    public DashboardService(OrderReconciliationRepository reconRepository) {
        this.reconRepository = reconRepository;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getSummaryPage(LocalDate asOfDate) {
        if (asOfDate == null) {
            asOfDate = LocalDate.now();
        }

        final LocalDate localAsOfDate = asOfDate;

        // Simple query: find all with SHORT_PAID or UNPAID_PAST_DUE status and due date <= asOfDate
        // Since we can't easily do Specification with OR conditions, get all and filter in Java
        List<OrderReconciliation> all = reconRepository.findAll();

        return all.stream()
                .filter(r -> "SHORT_PAID".equals(r.getReconciliationStatus()) || "UNPAID_PAST_DUE".equals(r.getReconciliationStatus()))
                .filter(r -> !r.getDueDate().isAfter(localAsOfDate))
                .map(recon -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("orderId", recon.getOrderId());
                    item.put("reconciliationStatus", recon.getReconciliationStatus());
                    item.put("dueDate", recon.getDueDate());
                    item.put("expectedSettlement", recon.getExpectedSettlement());
                    item.put("receivedSettlement", recon.getReceivedSettlement());
                    item.put("settlementDiff", recon.getSettlementDiff());
                    long days = ChronoUnit.DAYS.between(recon.getDueDate(), localAsOfDate);
                    item.put("dueDaysBetween", days);
                    return item;
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getSummary(LocalDate asOfDate) {
        if (asOfDate == null) {
            asOfDate = LocalDate.now();
        }

        final LocalDate localAsOfDate = asOfDate;
        List<Map<String, Object>> items = getSummaryPage(asOfDate);

        if (items.isEmpty()) {
            return Map.of(
                    "totalOrders", 0,
                    "moneyAtRisk", BigDecimal.ZERO,
                    "byPartner", new HashMap<>(),
                    "byAgeBucket", Map.of(
                            "0-7", BigDecimal.ZERO,
                            "8-15", BigDecimal.ZERO,
                            "16-30", BigDecimal.ZERO,
                            "31+", BigDecimal.ZERO
                    )
            );
        }

        long totalOrders = items.size();
        BigDecimal totalMoneyAtRisk = BigDecimal.ZERO;

        Map<String, BigDecimal> ageBuckets = new HashMap<>();
        ageBuckets.put("0-7", BigDecimal.ZERO);
        ageBuckets.put("8-15", BigDecimal.ZERO);
        ageBuckets.put("16-30", BigDecimal.ZERO);
        ageBuckets.put("31+", BigDecimal.ZERO);

        Map<String, BigDecimal> byPartner = new HashMap<>();

        for (Map<String, Object> item : items) {
            BigDecimal mar = (BigDecimal) item.get("settlementDiff");
            if (mar != null && mar.compareTo(BigDecimal.ZERO) > 0) {
                totalMoneyAtRisk = totalMoneyAtRisk.add(mar);
            }

            long days = (Long) item.get("dueDaysBetween");
            String bucket;
            if (days >= 0 && days <= 7) bucket = "0-7";
            else if (days >= 8 && days <= 15) bucket = "8-15";
            else if (days >= 16 && days <= 30) bucket = "16-30";
            else bucket = "31+";

            BigDecimal current = ageBuckets.get(bucket);
            ageBuckets.put(bucket, current.add(BigDecimal.ONE));

            String status = (String) item.get("reconciliationStatus");
            byPartner.put(status, byPartner.getOrDefault(status, BigDecimal.ZERO).add(BigDecimal.ONE));
        }

        return Map.of(
                "totalOrders", totalOrders,
                "moneyAtRisk", totalMoneyAtRisk,
                "byPartner", byPartner,
                "byAgeBucket", ageBuckets
        );
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getMoneyAtRiskByPartnerAndAge(LocalDate asOfDate) {
        if (asOfDate == null) {
            asOfDate = LocalDate.now();
        }

        final LocalDate localAsOfDate = asOfDate;
        List<Map<String, Object>> items = getSummaryPage(asOfDate);

        Map<String, BigDecimal> byPartner = new HashMap<>();
        Map<String, BigDecimal> byAgeBucket = new HashMap<>();
        byAgeBucket.put("0-7", BigDecimal.ZERO);
        byAgeBucket.put("8-15", BigDecimal.ZERO);
        byAgeBucket.put("16-30", BigDecimal.ZERO);
        byAgeBucket.put("31+", BigDecimal.ZERO);

        for (Map<String, Object> item : items) {
            String status = (String) item.get("reconciliationStatus");
            long days = (Long) item.get("dueDaysBetween");

            byPartner.put(status, byPartner.getOrDefault(status, BigDecimal.ZERO).add(BigDecimal.ONE));

            if (days >= 0 && days <= 7) {
                byAgeBucket.put("0-7", byAgeBucket.get("0-7").add(BigDecimal.ONE));
            } else if (days >= 8 && days <= 15) {
                byAgeBucket.put("8-15", byAgeBucket.get("8-15").add(BigDecimal.ONE));
            } else if (days >= 16 && days <= 30) {
                byAgeBucket.put("16-30", byAgeBucket.get("16-30").add(BigDecimal.ONE));
            } else {
                byAgeBucket.put("31+", byAgeBucket.get("31+").add(BigDecimal.ONE));
            }
        }

        return Map.of(
                "byPartner", byPartner,
                "byAgeBucket", byAgeBucket
        );
    }
}