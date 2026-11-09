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

        // Run ANALYZE on staging tables for query optimization (PostgreSQL-specific)
        // In test environment with H2, this will be ignored by catching the exception
        try {
            jdbcTemplate.execute("ANALYZE settlements");
            jdbcTemplate.execute("ANALYZE cod_remittances");
            jdbcTemplate.execute("ANALYZE freight_invoices");
        } catch (Exception e) {
            // H2 doesn't support ANALYZE - ignore in tests
        }

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
                "SELECT COALESCE(SUM(CASE WHEN reconciliation_status IN ('UNPAID_PAST_DUE', 'SHORT_PAID') THEN ABS(settlement_diff) ELSE 0 END), 0) FROM order_reconciliation",
                BigDecimal.class);

        // Calculate auto match rate
        Long autoMatched = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM order_reconciliation WHERE is_matched = true", Long.class);
        if (autoMatched == null) autoMatched = 0L;
        double autoMatchRate = totalOrders > 0 ? (double) autoMatched / totalOrders * 100 : 0;

        return new MatchingRunSummary(
                totalOrders,
                autoMatched,
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
        // Embed asOfDate as a safe literal — avoids JDBC parameter issues in execute()
        String asOfLiteral = "'" + asOfDate.toString() + "'::date";

        String sqlTemplate = """
            INSERT INTO order_reconciliation (
                order_id, due_date, expected_settlement, expected_freight,
                received_settlement, received_freight, settlement_diff, freight_diff,
                total_tcs, total_tds, reconciliation_status, is_matched, matched_at
            )
            WITH
            latest_channel_rc AS (
                SELECT DISTINCT ON (channel_code)
                    channel_code, commission_pct, fixed_fee, collection_fee_pct, min_collection_fee
                FROM channel_rate_cards
                ORDER BY channel_code, effective_from DESC
            ),
            latest_courier_rc AS (
                SELECT DISTINCT ON (courier_code)
                    id AS rc_id, courier_code, cod_fee_pct, return_fee
                FROM courier_rate_cards
                ORDER BY courier_code, effective_from DESC
            ),
            order_courier AS (
                SELECT DISTINCT ON (o.order_id) o.order_id, s.courier_code
                FROM orders o
                LEFT JOIN sources s ON s.channel_code = o.channel_code
                ORDER BY o.order_id, s.id
            ),
            order_slab AS (
                SELECT oc.order_id,
                       COALESCE(
                           (SELECT cws.rate
                            FROM courier_weight_slabs cws
                            JOIN latest_courier_rc lcr ON lcr.rc_id = cws.rate_card_id
                            WHERE lcr.courier_code = oc.courier_code
                              AND cws.min_weight_kg <= COALESCE(
                                    (SELECT AVG(fi.billed_weight) FROM freight_invoices fi WHERE fi.order_id = oc.order_id), 0
                                  )
                            ORDER BY cws.min_weight_kg DESC
                            LIMIT 1), 0
                       ) AS slab_rate
                FROM order_courier oc
            ),
            agg_settlement AS (
                SELECT order_id,
                       SUM(net_amount)  AS received_settlement,
                       SUM(tcs_amount)  AS total_tcs,
                       SUM(tds_amount)  AS total_tds
                FROM (
                    SELECT DISTINCT ON (order_id, reference_no, net_amount, settlement_date)
                        order_id, net_amount, tcs_amount, tds_amount
                    FROM settlements
                    ORDER BY order_id, reference_no, net_amount, settlement_date
                ) deduped
                GROUP BY order_id
            ),
            agg_cod AS (
                SELECT order_id, SUM(net_remitted) AS received_cod
                FROM cod_remittances
                GROUP BY order_id
            ),
            agg_freight AS (
                SELECT DISTINCT ON (order_id)
                    order_id, total_freight AS received_freight
                FROM freight_invoices
                ORDER BY order_id, invoice_date DESC
            )
            SELECT
                o.order_id,
                (o.order_date + CASE
                    WHEN o.payment_type = 'COD'        THEN INTERVAL '7 days'
                    WHEN o.channel_code ILIKE 'quick%' THEN INTERVAL '3 days'
                    ELSE                                    INTERVAL '10 days'
                END)::date AS due_date,
                COALESCE(o.expected_amount_override,
                    ROUND(o.order_value
                        - ROUND(COALESCE(crc.commission_pct, 0) * o.order_value / 100, 2)
                        - COALESCE(crc.fixed_fee, 0)
                        - GREATEST(
                            ROUND(COALESCE(crc.collection_fee_pct, 0) * o.order_value / 100, 2),
                            COALESCE(crc.min_collection_fee, 0)
                          ), 2)
                ) AS expected_settlement,
                COALESCE(fw.slab_rate, 0)
                    + CASE WHEN o.payment_type = 'COD'
                           THEN COALESCE(crc2.cod_fee_pct, 0) * COALESCE(fw.slab_rate, 0) / 100
                           ELSE 0 END
                    + CASE WHEN o.delivery_status = 'RETURNED_TO_WAREHOUSE'
                           THEN COALESCE(crc2.return_fee, 0)
                           ELSE 0 END AS expected_freight,
                COALESCE(agg.received_settlement, 0) + COALESCE(cod_agg.received_cod, 0) AS received_settlement,
                COALESCE(freight_agg.received_freight, 0) AS received_freight,
                (COALESCE(agg.received_settlement, 0) + COALESCE(cod_agg.received_cod, 0))
                    - COALESCE(o.expected_amount_override,
                        ROUND(o.order_value
                            - ROUND(COALESCE(crc.commission_pct, 0) * o.order_value / 100, 2)
                            - COALESCE(crc.fixed_fee, 0)
                            - GREATEST(
                                ROUND(COALESCE(crc.collection_fee_pct, 0) * o.order_value / 100, 2),
                                COALESCE(crc.min_collection_fee, 0)
                              ), 2)
                      ) AS settlement_diff,
                COALESCE(freight_agg.received_freight, 0)
                    - (COALESCE(fw.slab_rate, 0)
                        + CASE WHEN o.payment_type = 'COD'
                               THEN COALESCE(crc2.cod_fee_pct, 0) * COALESCE(fw.slab_rate, 0) / 100
                               ELSE 0 END
                        + CASE WHEN o.delivery_status = 'RETURNED_TO_WAREHOUSE'
                               THEN COALESCE(crc2.return_fee, 0)
                               ELSE 0 END) AS freight_diff,
                COALESCE(agg.total_tcs, 0) AS total_tcs,
                COALESCE(agg.total_tds, 0) AS total_tds,
                CASE
                    WHEN (COALESCE(agg.received_settlement, 0) + COALESCE(cod_agg.received_cod, 0)) = 0
                         AND AS_OF_DATE > (o.order_date + CASE
                             WHEN o.payment_type = 'COD'        THEN INTERVAL '7 days'
                             WHEN o.channel_code ILIKE 'quick%' THEN INTERVAL '3 days'
                             ELSE INTERVAL '10 days' END)::date
                    THEN 'UNPAID_PAST_DUE'
                    WHEN ABS(
                            (COALESCE(agg.received_settlement, 0) + COALESCE(cod_agg.received_cod, 0))
                            - COALESCE(o.expected_amount_override,
                                ROUND(o.order_value
                                    - ROUND(COALESCE(crc.commission_pct, 0) * o.order_value / 100, 2)
                                    - COALESCE(crc.fixed_fee, 0)
                                    - GREATEST(
                                        ROUND(COALESCE(crc.collection_fee_pct, 0) * o.order_value / 100, 2),
                                        COALESCE(crc.min_collection_fee, 0)
                                      ), 2)
                              )
                         ) > 10.00
                         AND NOT (
                             (COALESCE(agg.received_settlement, 0) + COALESCE(cod_agg.received_cod, 0)) = 0
                             AND AS_OF_DATE > (o.order_date + CASE
                                 WHEN o.payment_type = 'COD'        THEN INTERVAL '7 days'
                                 WHEN o.channel_code ILIKE 'quick%' THEN INTERVAL '3 days'
                                 ELSE INTERVAL '10 days' END)::date
                         )
                         AND (COALESCE(agg.received_settlement, 0) + COALESCE(cod_agg.received_cod, 0))
                             < COALESCE(o.expected_amount_override,
                                 ROUND(o.order_value
                                     - ROUND(COALESCE(crc.commission_pct, 0) * o.order_value / 100, 2)
                                     - COALESCE(crc.fixed_fee, 0)
                                     - GREATEST(
                                         ROUND(COALESCE(crc.collection_fee_pct, 0) * o.order_value / 100, 2),
                                         COALESCE(crc.min_collection_fee, 0)
                                       ), 2)
                               )
                    THEN 'SHORT_PAID'
                    WHEN COALESCE(freight_agg.received_freight, 0)
                             > (COALESCE(fw.slab_rate, 0)
                                 + CASE WHEN o.payment_type = 'COD'
                                        THEN COALESCE(crc2.cod_fee_pct, 0) * COALESCE(fw.slab_rate, 0) / 100
                                        ELSE 0 END
                                 + CASE WHEN o.delivery_status = 'RETURNED_TO_WAREHOUSE'
                                        THEN COALESCE(crc2.return_fee, 0)
                                        ELSE 0 END)
                         AND ABS(COALESCE(freight_agg.received_freight, 0)
                                 - (COALESCE(fw.slab_rate, 0)
                                     + CASE WHEN o.payment_type = 'COD'
                                            THEN COALESCE(crc2.cod_fee_pct, 0) * COALESCE(fw.slab_rate, 0) / 100
                                            ELSE 0 END
                                     + CASE WHEN o.delivery_status = 'RETURNED_TO_WAREHOUSE'
                                            THEN COALESCE(crc2.return_fee, 0)
                                            ELSE 0 END)) > 10.00
                    THEN 'FREIGHT_OVERCHARGED'
                    ELSE 'SETTLED'
                END AS reconciliation_status,
                true AS is_matched,
                NOW() AS matched_at
            FROM orders o
            LEFT JOIN latest_channel_rc  crc  ON crc.channel_code  = o.channel_code
            LEFT JOIN order_courier      oc2  ON oc2.order_id       = o.order_id
            LEFT JOIN latest_courier_rc  crc2 ON crc2.courier_code  = oc2.courier_code
            LEFT JOIN order_slab         fw   ON fw.order_id        = o.order_id
            LEFT JOIN agg_settlement     agg  ON agg.order_id       = o.order_id
            LEFT JOIN agg_cod            cod_agg      ON cod_agg.order_id      = o.order_id
            LEFT JOIN agg_freight        freight_agg  ON freight_agg.order_id  = o.order_id
            ON CONFLICT (order_id) DO UPDATE SET
                due_date               = EXCLUDED.due_date,
                expected_settlement    = EXCLUDED.expected_settlement,
                expected_freight       = EXCLUDED.expected_freight,
                received_settlement    = EXCLUDED.received_settlement,
                received_freight       = EXCLUDED.received_freight,
                settlement_diff        = EXCLUDED.settlement_diff,
                freight_diff           = EXCLUDED.freight_diff,
                total_tcs              = EXCLUDED.total_tcs,
                total_tds              = EXCLUDED.total_tds,
                reconciliation_status  = EXCLUDED.reconciliation_status,
                is_matched             = EXCLUDED.is_matched,
                matched_at             = EXCLUDED.matched_at
            """;

        // Replace the AS_OF_DATE placeholder with the safe literal (no format specifiers involved)
        return sqlTemplate.replace("AS_OF_DATE", asOfLiteral);
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
        public final long autoMatched;
        public final long unpaidPastDue;
        public final long shortPaid;
        public final long freightOvercharged;
        public final long settled;
        public final long pending;
        public final BigDecimal moneyAtRisk;
        public final double autoMatchRate;
        public final long durationMs;

        public MatchingRunSummary(long totalOrders, long autoMatched, long unpaidPastDue, long shortPaid,
                                  long freightOvercharged, long settled, long pending,
                                  BigDecimal moneyAtRisk, double autoMatchRate, long durationMs) {
            this.totalOrders = totalOrders;
            this.autoMatched = autoMatched;
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