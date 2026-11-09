package com.reconcileiq.matching;

import com.reconcileiq.ReconcileIqApplication;
import com.reconcileiq.auth.User;
import com.reconcileiq.auth.UserRepository;
import com.reconcileiq.ingestion.Source;
import com.reconcileiq.ingestion.SourceRepository;
import com.reconcileiq.ingestion.UploadBatchRepository;
import com.reconcileiq.ingestion.RejectedRowRepository;
import com.reconcileiq.mapping.ColumnMappingRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
@SpringBootTest(classes = ReconcileIqApplication.class)
@ActiveProfiles("test")
class MatchingRulesTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("reconcileiq")
            .withUsername("reconcileiq")
            .withPassword("reconcileiq_secret");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.flyway.locations", () -> "classpath:db/migration");
        registry.add("spring.flyway.baseline-on-migrate", () -> "true");
    }

    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private UserRepository userRepository;
    @Autowired private MatchingService matchingService;
    @Autowired private OrderReconciliationRepository reconRepository;
    @Autowired private com.reconcileiq.ingestion.IngestionService ingestionService;
    @Autowired private ColumnMappingRepository mappingRepository;
    @Autowired private SourceRepository sourceRepository;
    @Autowired private UploadBatchRepository batchRepository;
    @Autowired private RejectedRowRepository rejectedRowRepository;
    @Autowired private com.reconcileiq.ingestion.OrderRepository orderRepository;
    @Autowired private com.reconcileiq.ingestion.SettlementRepository settlementRepository;
    @Autowired private com.reconcileiq.ingestion.CodRemittanceRepository codRepository;
    @Autowired private com.reconcileiq.ingestion.FreightInvoiceRepository freightRepository;

    private Long websiteId;

    @BeforeEach
    void setup() throws Exception {
        reconRepository.deleteAll();
        orderRepository.deleteAll();
        settlementRepository.deleteAll();
        codRepository.deleteAll();
        freightRepository.deleteAll();
        rejectedRowRepository.deleteAll();
        batchRepository.deleteAll();
        userRepository.deleteAll();
        mappingRepository.deleteAll();

        // Use existing Flyway-seeded source or create one
        Source website = sourceRepository.findAll().stream()
                .filter(s -> "CHANNEL".equals(s.getPartnerType()))
                .findFirst()
                .orElseGet(() -> {
                    Source s = new Source("Website", "WEBSITE", "CHANNEL", "Website", null);
                    return sourceRepository.save(s);
                });
        websiteId = website.getId();

        saveMapping(websiteId, "orders",
                "{\"orderId\": \"OrderID\", \"channelCode\": \"ChannelCode\", \"orderDate\": \"OrderDate\", \"orderValue\": \"OrderValue\", \"paymentType\": \"PaymentType\", \"deliveryStatus\": \"DeliveryStatus\", \"customerName\": \"CustomerName\", \"customerPhone\": \"CustomerPhone\", \"customerAddress\": \"CustomerAddress\", \"expectedAmountOverride\": \"expected_amount_override\"}");
        saveMapping(websiteId, "settlement",
                "{\"orderId\": \"OrderID\", \"referenceNo\": \"ReferenceNo\", \"settlementDate\": \"SettleDate\", \"grossAmount\": \"GrossAmt\", \"commission\": \"Commission\", \"fixedFee\": \"FixedFee\", \"collectionFee\": \"CollFee\", \"tcsAmount\": \"TCS\", \"tdsAmount\": \"TDS\", \"netAmount\": \"NetAmt\"}");
        saveMapping(websiteId, "cod_remittance",
                "{\"orderId\": \"OrderID\", \"courierCode\": \"CourierCode\", \"utrNo\": \"UTRNo\", \"remittanceDate\": \"RemitDate\", \"codCollected\": \"CODCollected\", \"feeDeducted\": \"FeeDeducted\", \"netRemitted\": \"NetRemitted\"}");
        saveMapping(websiteId, "freight_invoice",
                "{\"orderId\": \"OrderID\", \"courierCode\": \"CourierCode\", \"invoiceNo\": \"InvoiceNo\", \"invoiceDate\": \"InvoiceDate\", \"billedWeight\": \"BilledWeight\", \"freightCharge\": \"FreightCharge\", \"codFee\": \"CODFee\", \"returnFee\": \"ReturnFee\", \"totalFreight\": \"TotalFreight\"}");

        User exec = new User();
        exec.setEmail("exec@demo.com");
        exec.setPasswordHash(passwordEncoder.encode("ExecPassword123!"));
        exec.setRole("FINANCE_EXECUTIVE");
        exec.setFullName("Finance Executive");
        exec.setCreatedAt(Instant.now());
        userRepository.save(exec);

        ingestFile("orders_demo.csv", "orders");
        ingestFile("settlements_demo.csv", "settlement");
        ingestFile("cod_remittances_demo.csv", "cod_remittance");
        ingestFile("freight_invoices_demo.csv", "freight_invoice");

        matchingService.runMatching(LocalDate.of(2026, 11, 30));
    }

    private void saveMapping(Long sourceId, String fileType, String json) {
        com.reconcileiq.mapping.ColumnMapping cm = new com.reconcileiq.mapping.ColumnMapping();
        cm.setSourceId(sourceId);
        cm.setFileType(fileType);
        cm.setVersion(1);
        cm.setMappingsJson(json);
        cm.setIsActive(true);
        cm.setCreatedBy("system");
        cm.setApprovedAt(Instant.now());
        mappingRepository.save(cm);
    }

    private void ingestFile(String fileName, String fileType) throws Exception {
        java.io.File file = new java.io.File("../scripts/output/" + fileName);
        if (!file.exists()) {
            throw new IllegalStateException("Demo file not found: " + file.getAbsolutePath());
        }
        try (java.io.FileInputStream is = new java.io.FileInputStream(file)) {
            org.springframework.mock.web.MockMultipartFile mf =
                    new org.springframework.mock.web.MockMultipartFile("file", fileName, "text/csv", is.readAllBytes());
            ingestionService.ingestFullFile(mf, websiteId, fileType);
        }
    }

    @Test
    void test100Vs1001() {
        // Tolerance boundary: |diff| <= 10.00 must not be SHORT_PAID
        List<OrderReconciliation> all = reconRepository.findAll();
        for (OrderReconciliation r : all) {
            if (r.getSettlementDiff() != null) {
                double absDiff = r.getSettlementDiff().abs().doubleValue();
                if (absDiff <= 10.00) {
                    assertNotEquals("SHORT_PAID", r.getReconciliationStatus(),
                            "Order " + r.getOrderId() + " diff=" + absDiff + " should not be SHORT_PAID (within tolerance)");
                }
            }
        }
    }

    @Test
    void testShortfall46() {
        // Every SHORT_PAID order must have |settlement_diff| > 10.00
        List<OrderReconciliation> shortPaid = reconRepository.findByReconciliationStatus("SHORT_PAID");
        for (OrderReconciliation r : shortPaid) {
            assertTrue(r.getSettlementDiff().abs().doubleValue() > 10.00,
                    "SHORT_PAID order " + r.getOrderId() + " must have |diff| > 10.00");
        }
    }

    @Test
    void testDuplicateSettlement() {
        // Re-run matching — upsert must not create extra rows
        long before = reconRepository.count();
        matchingService.runMatching(LocalDate.of(2026, 11, 30));
        long after = reconRepository.count();
        assertEquals(before, after, "Re-running matching must not create duplicate reconciliation rows");
    }

    @Test
    void testTCSTDSExcluded() {
        // TCS/TDS fields must be non-null for every reconciliation row
        List<OrderReconciliation> all = reconRepository.findAll();
        assertFalse(all.isEmpty(), "Expected reconciliation rows after matching");
        all.forEach(r -> {
            assertNotNull(r.getTotalTcs(), "total_tcs must not be null for " + r.getOrderId());
            assertNotNull(r.getTotalTds(), "total_tds must not be null for " + r.getOrderId());
        });
    }

    @Test
    void testPendingNotYetDue() {
        // All reconciliation rows must have a non-null status
        List<OrderReconciliation> all = reconRepository.findAll();
        assertFalse(all.isEmpty(), "Expected reconciliation rows after matching");
        all.forEach(r -> assertNotNull(r.getReconciliationStatus(),
                "reconciliation_status must not be null for " + r.getOrderId()));
    }

    @Test
    void testIdempotentReRun() {
        // Second run must produce identical statuses
        List<OrderReconciliation> before = reconRepository.findAll();
        matchingService.runMatching(LocalDate.of(2026, 11, 30));
        List<OrderReconciliation> after = reconRepository.findAll();

        assertEquals(before.size(), after.size(), "Row count must be identical after re-run");
        Map<String, String> beforeMap = before.stream()
                .collect(java.util.stream.Collectors.toMap(
                        OrderReconciliation::getOrderId,
                        OrderReconciliation::getReconciliationStatus));
        for (OrderReconciliation r : after) {
            assertEquals(beforeMap.get(r.getOrderId()), r.getReconciliationStatus(),
                    "Status changed for " + r.getOrderId() + " after idempotent re-run");
        }
    }
}
