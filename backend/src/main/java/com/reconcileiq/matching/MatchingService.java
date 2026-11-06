package com.reconcileiq.matching;

import com.reconcileiq.ingestion.*;
import com.reconcileiq.ratecard.RateCardService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

@Service
public class MatchingService {

    private final JdbcTemplate jdbcTemplate;
    private final RateCardService rateCardService;
    private final OrderRepository orderRepository;
    private final SettlementRepository settlementRepository;
    private final CodRemittanceRepository codRepository;
    private final FreightInvoiceRepository freightRepository;
    private final OrderReconciliationRepository reconRepository;
    private final ReviewQueueRepository reviewQueueRepository;

    private static final BigDecimal TOLERANCE = BigDecimal.valueOf(10.00);

    // Settlement window days by channel type
    private static final Map<String, Integer> SETTLEMENT_WINDOWS = Map.of(
        "COD", 7,
        "MARKETPLACE", 10,
        "WEBSITE", 10,
        "QUICK_COMMERCE", 3
    );

    public MatchingService(JdbcTemplate jdbcTemplate,
                           RateCardService rateCardService,
                           OrderRepository orderRepository,
                           SettlementRepository settlementRepository,
                           CodRemittanceRepository codRepository,
                           FreightInvoiceRepository freightRepository,
                           OrderReconciliationRepository reconRepository,
                           ReviewQueueRepository reviewQueueRepository) {
        this.jdbcTemplate = jdbcTemplate;
        this.rateCardService = rateCardService;
        this.orderRepository = orderRepository;
        this.settlementRepository = settlementRepository;
        this.codRepository = codRepository;
        this.freightRepository = freightRepository;
        this.reconRepository = reconRepository;
        this.reviewQueueRepository = reviewQueueRepository;
    }

    @Transactional
    public void runMatching() {
        // Idempotent: recompute from base feeds using INSERT ... ON CONFLICT
        String sql = """
            INSERT INTO order_reconciliation (
                order_id, due_date, expected_settlement, expected_freight,
                received_settlement, received_freight, settlement_diff, freight_diff,
                total_tcs, total_tds, reconciliation_status, is_matched, matched_at
            )
            SELECT
                o.order_id,
                o.order_date + (CASE
                    WHEN o.payment_type = 'COD' THEN 7
                    WHEN o.channel_code ILIKE 'marketplace%' THEN 10
                    WHEN o.channel_code ILIKE 'website%' THEN 10
                    WHEN o.channel_code ILIKE 'quick%' THEN 3
                    ELSE 10 END)::int AS due_date,
                COALESCE(rc.expected_settlement, 0) AS expected_settlement,
                COALESCE(rc.expected_freight, 0) AS expected_freight,
                COALESCE(agg.received_settlement, 0) AS received_settlement,
                COALESCE(agg.received_freight, 0) AS received_freight,
                COALESCE(agg.received_settlement, 0) - COALESCE(rc.expected_settlement, 0) AS settlement_diff,
                COALESCE(agg.received_freight, 0) - COALESCE(rc.expected_freight, 0) AS freight_diff,
                COALESCE(agg.total_tcs, 0) AS total_tcs,
                COALESCE(agg.total_tds, 0) AS total_tds,
                CASE
                    WHEN COALESCE(agg.received_settlement, 0) = 0 AND CURRENT_DATE > (o.order_date + (CASE
                        WHEN o.payment_type = 'COD' THEN 7
                        WHEN o.channel_code ILIKE 'marketplace%' THEN 10
                        WHEN o.channel_code ILIKE 'website%' THEN 10
                        WHEN o.channel_code ILIKE 'quick%' THEN 3
                        ELSE 10 END)::int) THEN 'UNPAID_PAST_DUE'
                    WHEN COALESCE(agg.received_settlement, 0) < COALESCE(rc.expected_settlement, 0)
                         AND ABS(COALESCE(agg.received_settlement, 0) - COALESCE(rc.expected_settlement, 0)) > 10 THEN 'SHORT_PAID'
                    WHEN COALESCE(agg.received_freight, 0) > COALESCE(rc.expected_freight, 0)
                         AND ABS(COALESCE(agg.received_freight, 0) - COALESCE(rc.expected_freight, 0)) > 10 THEN 'FREIGHT_OVERCHARGED'
                    ELSE 'SETTLED'
                END AS reconciliation_status,
                true AS is_matched,
                NOW() AS matched_at
            FROM orders o
            LEFT JOIN (
                -- Expected amounts per order (computed via rate cards in Java, but here we use a simplified approach)
                SELECT order_id,
                    (order_value - (order_value * commission_pct / 100) - fixed_fee - GREATEST(order_value * collection_fee_pct / 100, min_collection_fee)) AS expected_settlement,
                    0 AS expected_freight
                FROM orders o
                LEFT JOIN channel_rate_cards crc ON crc.channel_code = o.channel_code AND crc.effective_from <= o.order_date
                WHERE crc.id IS NOT NULL
                ORDER BY crc.effective_from DESC
            ) rc ON rc.order_id = o.order_id
            LEFT JOIN (
                -- Aggregated received amounts per order
                SELECT
                    order_id,
                    SUM(net_amount) AS received_settlement,
                    0 AS received_freight,
                    SUM(tcs_amount) AS total_tcs,
                    SUM(tds_amount) AS total_tds
                FROM (
                    SELECT DISTINCT ON (reference_no, net_amount, settlement_date)
                        order_id, net_amount, tcs_amount, tds_amount
                    FROM settlements
                ) s
                GROUP BY order_id
            ) agg ON agg.order_id = o.order_id
            ON CONFLICT (order_id) DO UPDATE SET
                due_date = EXCLUDED.due_date,
                expected_settlement = EXCLUDED.expected_settlement,
                expected_freight = EXCLUDED.expected_freight,
                received_settlement = EXCLUDED.received_settlement,
                received_freight = EXCLUDED.received_freight,
                settlement_diff = EXCLUDED.settlement_diff,
                freight_diff = EXCLUDED.freight_diff,
                total_tcs = EXCLUDED.total_tcs,
                total_tds = EXCLUDED.total_tds,
                reconciliation_status = EXCLUDED.reconciliation_status,
                is_matched = EXCLUDED.is_matched,
                matched_at = EXCLUDED.matched_at
            WHERE order_reconciliation.matched_at IS DISTINCT FROM EXCLUDED.matched_at
               OR order_reconciliation.reconciliation_status IS DISTINCT FROM EXCLUDED.reconciliation_status
               OR order_reconciliation.received_settlement IS DISTINCT FROM EXCLUDED.received_settlement
               OR order_reconciliation.received_freight IS DISTINCT FROM EXCLUDED.received_freight
            """;
        jdbcTemplate.execute(sql);
    }

    @Transactional
    public void populateReviewQueue(Long batchId, String fileType, Long sourceId, List<Map<String, String>> rows) {
        for (Map<String, String> row : rows) {
            String orderId = row.get("orderId");
            if (orderId == null || orderId.trim().isEmpty()) {
                // Try to find a match by other fields
                ReviewQueue item = new ReviewQueue();
                item.setBatchId(batchId);
                item.setFileType(fileType);
                item.setSourceId(sourceId);
                item.setRawDataJson(row.toString());
                item.setConfidence(BigDecimal.valueOf(0.3));
                item.setReason("Missing order ID - needs manual review");
                item.setStatus("PENDING");
                reviewQueueRepository.save(item);
            }
        }
    }
}