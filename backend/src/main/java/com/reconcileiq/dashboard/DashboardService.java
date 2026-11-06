package com.reconcileiq.dashboard;

import com.reconcileiq.matching.OrderReconciliation;
import com.reconcileiq.matching.OrderReconciliationRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class DashboardService {

    private final OrderReconciliationRepository reconRepository;

    public DashboardService(OrderReconciliationRepository reconRepository) {
        this.reconRepository = reconRepository;
    }

    public MoneyAtRiskSummary getMoneyAtRiskSummary() {
        List<OrderReconciliation> all = reconRepository.findAll();
        LocalDate today = LocalDate.now();

        MoneyAtRiskSummary summary = new MoneyAtRiskSummary();
        summary.totalMoneyAtRisk = BigDecimal.ZERO;
        summary.byPartner = new HashMap<>();
        summary.byAgeBucket = new HashMap<>();

        for (OrderReconciliation r : all) {
            if (r.getReconciliationStatus() == null) continue;

            BigDecimal mar = BigDecimal.ZERO;
            if ("SHORT_PAID".equals(r.getReconciliationStatus()) || "UNPAID_PAST_DUE".equals(r.getReconciliationStatus())) {
                mar = r.getExpectedSettlement().subtract(r.getReceivedSettlement());
                if (mar.compareTo(BigDecimal.ZERO) > 0) {
                    summary.totalMoneyAtRisk = summary.totalMoneyAtRisk.add(mar);
                } else {
                    mar = BigDecimal.ZERO;
                }
            }

            if (mar.compareTo(BigDecimal.ZERO) > 0) {
                // By partner (need channel_code from orders - simplified)
                String partner = "UNKNOWN"; // Would need to join with orders
                summary.byPartner.merge(partner, mar, BigDecimal::add);

                // By age bucket
                if (r.getDueDate() != null) {
                    long daysPastDue = ChronoUnit.DAYS.between(r.getDueDate(), today);
                    String bucket;
                    if (daysPastDue <= 7) bucket = "0-7";
                    else if (daysPastDue <= 15) bucket = "8-15";
                    else if (daysPastDue <= 30) bucket = "16-30";
                    else bucket = "31+";
                    summary.byAgeBucket.merge(bucket, mar, BigDecimal::add);
                }
            }
        }

        return summary;
    }

    public PartnerLeakageSummary getPartnerLeakageSummary() {
        List<OrderReconciliation> all = reconRepository.findAll();
        Map<String, PartnerLeakageSummary.PartnerLeakage> leakageMap = new HashMap<>();

        for (OrderReconciliation r : all) {
            if (r.getReconciliationStatus() == null) continue;

            String partner = "UNKNOWN"; // Would need to join with orders

            PartnerLeakageSummary.PartnerLeakage leakage = leakageMap.computeIfAbsent(partner,
                    k -> new PartnerLeakageSummary.PartnerLeakage(partner, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO));

            if ("SHORT_PAID".equals(r.getReconciliationStatus()) || "UNPAID_PAST_DUE".equals(r.getReconciliationStatus())) {
                BigDecimal shortfall = r.getExpectedSettlement().subtract(r.getReceivedSettlement());
                if (shortfall.compareTo(BigDecimal.ZERO) > 0) {
                    leakage.totalShortfall = leakage.totalShortfall.add(shortfall);
                }
            }
            if ("FREIGHT_OVERCHARGED".equals(r.getReconciliationStatus())) {
                BigDecimal overcharge = r.getReceivedFreight().subtract(r.getExpectedFreight());
                if (overcharge.compareTo(BigDecimal.ZERO) > 0) {
                    leakage.freightOvercharge = leakage.freightOvercharge.add(overcharge);
                }
            }
            leakage.totalTcs = leakage.totalTcs.add(r.getTotalTcs());
            leakage.totalTds = leakage.totalTds.add(r.getTotalTds());
        }

        return new PartnerLeakageSummary(new java.util.ArrayList<>(leakageMap.values()));
    }

    public static class MoneyAtRiskSummary {
        public BigDecimal totalMoneyAtRisk;
        public Map<String, BigDecimal> byPartner;
        public Map<String, BigDecimal> byAgeBucket;
    }

    public static class PartnerLeakageSummary {
        public List<PartnerLeakage> partners;

        public PartnerLeakageSummary(List<PartnerLeakage> partners) {
            this.partners = partners;
        }

        public static class PartnerLeakage {
            public String partnerCode;
            public BigDecimal totalShortfall;
            public BigDecimal freightOvercharge;
            public BigDecimal totalTcs;
            public BigDecimal totalTds;

            public PartnerLeakage(String partnerCode, BigDecimal totalShortfall, BigDecimal freightOvercharge, BigDecimal totalTcs) {
                this.partnerCode = partnerCode;
                this.totalShortfall = totalShortfall;
                this.freightOvercharge = freightOvercharge;
                this.totalTcs = totalTcs;
                this.totalTds = BigDecimal.ZERO;
            }
        }
    }
}