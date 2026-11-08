package com.reconcileiq.dashboard;

import com.reconcileiq.matching.OrderReconciliationRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;

@Service
public class DashboardService {

    private final OrderReconciliationRepository reconRepository;
    private final JdbcTemplate jdbcTemplate;

    public DashboardService(OrderReconciliationRepository reconRepository, JdbcTemplate jdbcTemplate) {
        this.reconRepository = reconRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getSummary(LocalDate asOfDate) {
        if (asOfDate == null) asOfDate = LocalDate.now();

        Long totalOrders = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM order_reconciliation", Long.class);
        Long matched = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM order_reconciliation WHERE is_matched = true", Long.class);
        if (totalOrders == null) totalOrders = 0L;
        if (matched == null) matched = 0L;

        double autoMatchRate = totalOrders > 0 ? (double) matched / totalOrders * 100 : 0;

        BigDecimal moneyAtRisk = jdbcTemplate.queryForObject(
                "SELECT COALESCE(SUM(ABS(settlement_diff)), 0) FROM order_reconciliation " +
                "WHERE reconciliation_status IN ('SHORT_PAID','UNPAID_PAST_DUE') AND due_date <= ?",
                BigDecimal.class, asOfDate);
        if (moneyAtRisk == null) moneyAtRisk = BigDecimal.ZERO;

        Map<String, Long> byStatus = jdbcTemplate.query(
                "SELECT reconciliation_status, COUNT(*) FROM order_reconciliation GROUP BY reconciliation_status",
                rs -> {
                    Map<String, Long> map = new LinkedHashMap<>();
                    while (rs.next()) map.put(rs.getString(1), rs.getLong(2));
                    return map;
                });
        if (byStatus == null) byStatus = Map.of();

        return Map.of(
                "totalOrders", totalOrders,
                "autoMatched", matched,
                "autoMatchRate", BigDecimal.valueOf(autoMatchRate).setScale(2, RoundingMode.HALF_UP),
                "moneyAtRisk", moneyAtRisk,
                "totalLeakage", moneyAtRisk,
                "byStatus", byStatus
        );
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getMoneyAtRiskByPartnerAndAge(LocalDate asOfDate) {
        if (asOfDate == null) asOfDate = LocalDate.now();

        String sql = """
            SELECT
                COALESCE(o.channel_code, r.order_id) AS partner_code,
                'CHANNEL' AS partner_type,
                COALESCE(SUM(CASE WHEN (? - r.due_date) BETWEEN 0 AND 7 THEN ABS(r.settlement_diff) ELSE 0 END), 0) AS bucket0to7,
                COALESCE(SUM(CASE WHEN (? - r.due_date) BETWEEN 8 AND 15 THEN ABS(r.settlement_diff) ELSE 0 END), 0) AS bucket8to15,
                COALESCE(SUM(CASE WHEN (? - r.due_date) BETWEEN 16 AND 30 THEN ABS(r.settlement_diff) ELSE 0 END), 0) AS bucket16to30,
                COALESCE(SUM(CASE WHEN (? - r.due_date) > 30 THEN ABS(r.settlement_diff) ELSE 0 END), 0) AS bucket31plus,
                COALESCE(SUM(ABS(r.settlement_diff)), 0) AS total
            FROM order_reconciliation r
            LEFT JOIN orders o ON o.order_id = r.order_id
            WHERE r.reconciliation_status IN ('SHORT_PAID', 'UNPAID_PAST_DUE')
              AND r.due_date <= ?
            GROUP BY COALESCE(o.channel_code, r.order_id)
            ORDER BY total DESC
            """;

        final LocalDate finalDate = asOfDate;
        return jdbcTemplate.query(sql,
                (rs, rowNum) -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("partnerCode", rs.getString("partner_code"));
                    row.put("partnerType", rs.getString("partner_type"));
                    row.put("bucket0to7", rs.getBigDecimal("bucket0to7"));
                    row.put("bucket8to15", rs.getBigDecimal("bucket8to15"));
                    row.put("bucket16to30", rs.getBigDecimal("bucket16to30"));
                    row.put("bucket31plus", rs.getBigDecimal("bucket31plus"));
                    row.put("total", rs.getBigDecimal("total"));
                    return row;
                },
                finalDate, finalDate, finalDate, finalDate, finalDate);
    }
}