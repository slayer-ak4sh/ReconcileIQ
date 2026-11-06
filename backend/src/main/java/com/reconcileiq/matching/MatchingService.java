package com.reconcileiq.matching;

import com.reconcileiq.ingestion.*;
import com.reconcileiq.ratecard.RateCardService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
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
    private static final BigDecimal SCALE = BigDecimal.valueOf(100); // for 2dp rounding

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
    public MatchingRunSummary runMatching(LocalDate asOfDate) {
        long startTime = System.currentTimeMillis();

        // Run ANALYZE on staging tables for query optimization
        jdbcTemplate.execute("ANALYZE settlements");
        jdbcTemplate.execute("ANALYZE cod_remittances");
        jdbcTemplate.execute("ANALYZE freight_invoices");

        // The set-based matching query
        String sql = buildMatchingSql(asOfDate);
        jdbcTemplate.execute(sql);

        // Get summary statistics
        long durationMs = System.currentTimeMillis() - startTime;

        // Count orders processed
        Long totalOrders = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM orders", Long.class);

        // Count per status
        Map<String, Long> statusCounts = jdbcTemplate.query(
                "SELECT reconciliation_status, COUNT(*) FROM order_reconciliation GROUP BY reconciliation_status",
                rs -> {
                    Map<String, Long> map = new java.util.HashMap<>();
                    while (rs.next()) {
                        map.put(rs.getString(1), rs.getLong(2));
                    }
                    return map;
                });

        // Calculate money at risk (only for UNPAID_PAST_DUE and SHORT_PAID)
        BigDecimal moneyAtRisk = jdbcTemplate.queryForObject(
                "SELECT COALESCE(SUM(settlement_diff), 0) FROM order_reconciliation " +
                "WHERE reconciliation_status IN ('UNPAID_PAST_DUE', 'SHORT_PAID') " +
                "AND settlement_diff > 0",
                BigDecimal.class);

        // Calculate auto match rate
        Long matched = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM order_reconciliation WHERE is_matched = true", Long.class);
        double autoMatchRate = totalOrders > 0 ? (double) matched / totalOrders * 100 : 0;

        return new MatchingRunSummary(
                totalOrders,
                statusCounts.getOrDefault("UNPAID_PAST_DUE", 0L),
                statusCounts.getOrDefault("SHORT_PAID", 0L),
                statusCounts.getOrDefault("FREIGHT_OVERCHARGED", 0L),
                statusCounts.getOrDefault("SETTLED", 0L),
                statusCounts.getOrDefault("PENDING", 0L),
                moneyAtRisk != null ? moneyAtRisk : BigDecimal.ZERO,
                autoMatchRate,
                durationMs
        );
    }

    @Transactional
    public void runMatching() {
        runMatching(LocalDate.now());
    }

    private String buildMatchingSql(LocalDate asOfDate) {
        return """
            INSERT INTO order_reconciliation (
                order_id, due_date, expected_settlement, expected_freight,
                received_settlement, received_freight, settlement_diff, freight_diff,
                total_tcs, total_tds, reconciliation_status, is_matched, matched_at
            )
            SELECT
                o.order_id,
                -- Due date: delivery_date + settlement window
                (o.order_date + CASE
                    WHEN o.payment_type = 'COD' THEN 7
                    WHEN o.channel_code ILIKE 'marketplace%' THEN 10
                    WHEN o.channel_code ILIKE 'website%' THEN 10
                    WHEN o.channel_code ILIKE 'quick%' THEN 3
                    ELSE 10 END) AS due_date,
                -- Expected settlement: use override from orders table if available, else compute from rate card
                -- Rounded HALF_UP to 2dp
                ROUND(COALESCE(o.expected_amount_override, COALESCE(crc.commission_pct, 0) * o.order_value / 100), 2) AS commission_amount,
                COALESCE(crc.fixed_fee, 0) AS fixed_fee_amount,
                GREATEST(ROUND(COALESCE(crc.collection_fee_pct, 0) * o.order_value / 100, 2),
                         COALESCE(crc.min_collection_fee, 0)) AS collection_fee_amount,
                ROUND(COALESCE(o.expected_amount_override,
                    (SELECT oo.expected_amount_override FROM orders oo WHERE oo.order_id = o.order_id)),
                    o.order_value
                    - ROUND(COALESCE(crc.commission_pct, 0) * o.order_value / 100, 2)
                    - COALESCE(crc.fixed_fee, 0)
                    - GREATEST(ROUND(COALESCE(crc.collection_fee_pct, 0) * o.order_value / 100, 2),
                             COALESCE(crc.min_collection_fee, 0)), 2)) AS expected_settlement,
                -- Expected freight: weight slab rate + COD fee (if COD) + return fee (if returned)
                COALESCE(fw.slab_rate, 0) +
                CASE WHEN o.payment_type = 'COD' THEN COALESCE(crc2.cod_fee_pct, 0) * fw.slab_rate / 100 ELSE 0 END +
                CASE WHEN o.delivery_status = 'RETURNED_TO_WAREHOUSE' THEN COALESCE(crc2.return_fee, 0) ELSE 0 END AS expected_freight,
                -- Received settlement: sum of settlement net_amount + COD net_remitted (deduped)
                COALESCE(agg.received_settlement, 0) + COALESCE(cod_agg.received_cod, 0) AS received_settlement,
                -- Received freight: latest freight invoice total_freight
                COALESCE(freight_agg.received_freight, 0) AS received_freight,
                -- Settlement diff = received - expected
                (COALESCE(agg.received_settlement, 0) + COALESCE(cod_agg.received_cod, 0))
                - ROUND(o.order_value
                    - ROUND(COALESCE(crc.commission_pct, 0) * o.order_value / 100, 2)
                    - COALESCE(crc.fixed_fee, 0)
                    - GREATEST(ROUND(COALESCE(crc.collection_fee_pct, 0) * o.order_value / 100, 2),
                             COALESCE(crc.min_collection_fee, 0)), 2) AS settlement_diff,
                -- Freight diff = received - expected
                COALESCE(freight_agg.received_freight, 0)
                - (COALESCE(fw.slab_rate, 0) +
                   CASE WHEN o.payment_type = 'COD' THEN COALESCE(crc2.cod_fee_pct, 0) * fw.slab_rate / 100 ELSE 0 END +
                   CASE WHEN o.delivery_status = 'RETURNED_TO_WAREHOUSE' THEN COALESCE(crc2.return_fee, 0) ELSE 0 END) AS freight_diff,
                COALESCE(agg.total_tcs, 0) AS total_tcs,
                COALESCE(agg.total_tds, 0) AS total_tds,
                -- Status precedence (one per order)
                CASE
                    -- 1. UNPAID_PAST_DUE: received = 0 AND asOfDate > due_date
                    WHEN (COALESCE(agg.received_settlement, 0) + COALESCE(cod_agg.received_cod, 0)) = 0
                         AND ? > (o.order_date + CASE
                             WHEN o.payment_type = 'COD' THEN 7
                             WHEN o.channel_code ILIKE 'marketplace%' THEN 10
                             WHEN o.channel_code ILIKE 'website%' THEN 10
                             WHEN o.channel_code ILIKE 'quick%' THEN 3
                             ELSE 10 END)
                    THEN 'UNPAID_PAST_DUE'
                    -- 2. SHORT_PAID: received < expected AND |diff| > tolerance
                    -- For returns (negative expected), received more negative than expected = over-deduction = SHORT_PAID
                    WHEN (COALESCE(agg.received_settlement, 0) + COALESCE(cod_agg.received_cod, 0))
                         < ROUND(o.order_value
                             - ROUND(COALESCE(crc.commission_pct, 0) * o.order_value / 100, 2)
                             - COALESCE(crc.fixed_fee, 0)
                             - GREATEST(ROUND(COALESCE(crc.collection_fee_pct, 0) * o.order_value / 100, 2),
                                      COALESCE(crc.min_collection_fee, 0)), 2)
                         AND ABS((COALESCE(agg.received_settlement, 0) + COALESCE(cod_agg.received_cod, 0))
                             - ROUND(o.order_value
                                 - ROUND(COALESCE(crc.commission_pct, 0) * o.order_value / 100, 2)
                                 - COALESCE(crc.fixed_fee, 0)
                                 - GREATEST(ROUND(COALESCE(crc.collection_fee_pct, 0) * o.order_value / 100, 2),
                                          COALESCE(crc.min_collection_fee, 0)), 2)) > 10.00
                    THEN 'SHORT_PAID'
                    -- 3. FREIGHT_OVERCHARGED: received_freight > expected_freight AND |diff| > tolerance
                    WHEN COALESCE(freight_agg.received_freight, 0)
                         > (COALESCE(fw.slab_rate, 0) +
                            CASE WHEN o.payment_type = 'COD' THEN COALESCE(crc2.cod_fee_pct, 0) * fw.slab_rate / 100 ELSE 0 END +
                            CASE WHEN o.delivery_status = 'RETURNED_TO_WAREHOUSE' THEN COALESCE(crc2.return_fee, 0) ELSE 0 END)
                         AND ABS(COALESCE(freight_agg.received_freight, 0)
                             - (COALESCE(fw.slab_rate, 0) +
                                CASE WHEN o.payment_type = 'COD' THEN COALESCE(crc2.cod_fee_pct, 0) * fw.slab_rate / 100 ELSE 0 END +
                                CASE WHEN o.delivery_status = 'RETURNED_TO_WAREHOUSE' THEN COALESCE(crc2.return_fee, 0) ELSE 0 END)) > 10.00
                    THEN 'FREIGHT_OVERCHARGED'
                    -- 4. SETTLED (all others including PENDING - not yet due and not paid)
                    ELSE 'SETTLED'
                END AS reconciliation_status,
                true AS is_matched,
                NOW() AS matched_at
            FROM orders o
            LEFT JOIN (
                -- Channel rate card for settlement calculation (latest effective)
                SELECT DISTINCT ON (crc.channel_code)
                    crc.channel_code, crc.commission_pct, crc.fixed_fee,
                    crc.collection_fee_pct, crc.min_collection_fee
                FROM channel_rate_cards crc
                WHERE crc.effective_from <= (SELECT MAX(order_date) FROM orders)
                ORDER BY crc.channel_code, crc.effective_from DESC
            ) crc ON crc.channel_code = o.channel_code
            LEFT JOIN (
                -- Courier rate card for freight calculation
                SELECT DISTINCT ON (c.courier_code)
                    c.courier_code, c.cod_fee_pct, c.return_fee
                FROM courier_rate_cards c
                WHERE c.effective_from <= (SELECT MAX(order_date) FROM orders)
                ORDER BY c.courier_code, c.effective_from DESC
            ) crc2 ON crc2.courier_code = (
                SELECT s.courier_code FROM sources s WHERE s.channel_code = o.channel_code LIMIT 1
            )
            LEFT JOIN (
                -- Weight slab rate lookup
                SELECT cws.rate_card_id,
                    FIRST_VALUE(cws.rate) OVER (PARTITION BY cws.rate_card_id ORDER BY cws.min_weight_kg) AS slab_rate
                FROM courier_weight_slabs cws
                WHERE cws.min_weight_kg <= (SELECT AVG(fi.billed_weight) FROM freight_invoices fi WHERE fi.order_id = o.order_id)
                ORDER BY cws.min_weight_kg DESC
                LIMIT 1
            ) fw ON fw.rate_card_id = (SELECT c.id FROM courier_rate_cards c WHERE c.courier_code = crc2.courier_code)
            LEFT JOIN (
                -- Aggregated settlement received (deduped by order_id, reference_no, net_amount, settlement_date)
                SELECT
                    order_id,
                    SUM(net_amount) AS received_settlement,
                    SUM(tcs_amount) AS total_tcs,
                    SUM(tds_amount) AS total_tds
                FROM (
                    SELECT DISTINCT ON (order_id, reference_no, net_amount, settlement_date)
                        order_id, net_amount, tcs_amount, tds_amount
                    FROM settlements
                    ORDER BY order_id, reference_no, net_amount, settlement_date
                ) s
                GROUP BY order_id
            ) agg ON agg.order_id = o.order_id
            LEFT JOIN (
                -- Aggregated COD remittance received
                SELECT order_id, SUM(net_remitted) AS received_cod
                FROM cod_remittances
                GROUP BY order_id
            ) cod_agg ON cod_agg.order_id = o.order_id
            LEFT JOIN (
                -- Latest freight invoice per order
                SELECT DISTINCT ON (order_id)
                    order_id, total_freight AS received_freight
                FROM freight_invoices
                ORDER BY order_id, invoice_date DESC
            ) freight_agg ON freight_agg.order_id = o.order_id
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
    }

    @Transactional
    public void populateReviewQueue(Long batchId, String fileType, Long sourceId, List<Map<String, String>> rows) {
        for (Map<String, String> row : rows) {
            String orderId = row.get("orderId");
            if (orderId == null || orderId.trim().isEmpty()) {
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

    public static class MatchingRunSummary {
        public final long totalOrders;
        public final long unpaidPastDue;
        public final long shortPaid;
        public final long freightOvercharged;
        public final long settled;
        public final long pending;
        public final BigDecimal moneyAtRisk;
        public final double autoMatchRate;
        public final long durationMs;

        public MatchingRunSummary(long totalOrders, long unpaidPastDue, long shortPaid,
                                  long freightOvercharged, long settled, long pending,
                                  BigDecimal moneyAtRisk, double autoMatchRate, long durationMs) {
            this.totalOrders = totalOrders;
            this.unpaidPastDue = unpaidPastDue;
            this.shortPaid = shortPaid;
            this.freightOvercharged = freightOvercharged;
            this.settled = settled;
            this.pending = pending;
            this.moneyAtRisk = moneyAtRisk;
            this.autoMatchRate = autoMatchRate;
            this.durationMs = durationMs;
        }
    }
}