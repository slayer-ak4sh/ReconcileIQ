package com.reconcileiq.matching;

import com.reconcileiq.ReconcileIqApplication;
import com.reconcileiq.auth.User;
import com.reconcileiq.auth.UserRepository;
import com.reconcileiq.ingestion.*;
import com.reconcileiq.matching.OrderReconciliation;
import com.reconcileiq.matching.OrderReconciliationRepository;
import com.reconcileiq.mapping.ColumnMapping;
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

import java.io.BufferedReader;
import java.io.FileReader;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
@SpringBootTest(classes = ReconcileIqApplication.class)
@ActiveProfiles("test")
class MatchingAnswerKeyTest {

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

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private IngestionService ingestionService;

    @Autowired
    private SourceRepository sourceRepository;

    @Autowired
    private ColumnMappingRepository mappingRepository;

    @Autowired
    private OrderReconciliationRepository reconRepository;

    @Autowired
    private MatchingService matchingService;

    private Long sourceId;

    @BeforeEach
    void setup() throws Exception {
        reconRepository.deleteAll();
        userRepository.deleteAll();
        mappingRepository.deleteAll();
        sourceRepository.deleteAll();

        // Seed sources
        Source website = new Source("Website", "WEBSITE", "CHANNEL", "Website", null);
        website = sourceRepository.save(website);
        sourceId = website.getId();

        Source mp1 = new Source("Marketplace 1", "MP1", "CHANNEL", "Marketplace 1", null);
        sourceRepository.save(mp1);

        // Seed active column mappings for Website
        saveMapping(website.getId(), "orders",
                "{\"orderId\": \"OrderID\", \"channelCode\": \"ChannelCode\", \"orderDate\": \"OrderDate\", \"orderValue\": \"OrderValue\", \"paymentType\": \"PaymentType\", \"deliveryStatus\": \"DeliveryStatus\", \"customerName\": \"CustomerName\", \"customerPhone\": \"CustomerPhone\", \"customerAddress\": \"CustomerAddress\", \"expectedAmountOverride\": \"expected_amount_override\"}");
        saveMapping(website.getId(), "settlement",
                "{\"orderId\": \"OrderID\", \"referenceNo\": \"ReferenceNo\", \"settlementDate\": \"SettleDate\", \"grossAmount\": \"GrossAmt\", \"commission\": \"Commission\", \"fixedFee\": \"FixedFee\", \"collectionFee\": \"CollFee\", \"tcsAmount\": \"TCS\", \"tdsAmount\": \"TDS\", \"netAmount\": \"NetAmt\"}");
        saveMapping(website.getId(), "cod_remittance",
                "{\"orderId\": \"OrderID\", \"courierCode\": \"CourierCode\", \"utrNo\": \"UTRNo\", \"remittanceDate\": \"RemitDate\", \"codCollected\": \"CODCollected\", \"feeDeducted\": \"FeeDeducted\", \"netRemitted\": \"NetRemitted\"}");
        saveMapping(website.getId(), "freight_invoice",
                "{\"orderId\": \"OrderID\", \"courierCode\": \"CourierCode\", \"invoiceNo\": \"InvoiceNo\", \"invoiceDate\": \"InvoiceDate\", \"billedWeight\": \"BilledWeight\", \"freightCharge\": \"FreightCharge\", \"codFee\": \"CODFee\", \"returnFee\": \"ReturnFee\", \"totalFreight\": \"TotalFreight\"}");

        // Create users
        User exec = new User();
        exec.setEmail("exec@demo.com");
        exec.setPasswordHash(passwordEncoder.encode("ExecPassword123!"));
        exec.setRole("FINANCE_EXECUTIVE");
        exec.setFullName("Finance Executive");
        exec.setCreatedAt(Instant.now());
        userRepository.save(exec);

        // Ingest demo files from scripts/output
        ingestFile("orders_demo.csv", "orders");
        ingestFile("settlements_demo.csv", "settlement");
        ingestFile("cod_remittances_demo.csv", "cod_remittance");
        ingestFile("freight_invoices_demo.csv", "freight_invoice");

        // Run matching
        matchingService.runMatching(LocalDate.of(2026, 11, 6));
    }

    private void saveMapping(Long sourceId, String fileType, String json) {
        ColumnMapping cm = new ColumnMapping();
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
            org.springframework.mock.web.MockMultipartFile mf = new org.springframework.mock.web.MockMultipartFile("file", fileName, "text/csv", is.readAllBytes());
            ingestionService.ingestFullFile(mf, sourceId, fileType);
        }
    }

    @Test
    void testAnswerKeyMatching() throws Exception {
        long total = reconRepository.count();
        System.out.println("Reconciliations after matching: " + total);

        // Load expected outcomes from demo CSV
        List<Map<String, String>> expected = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader("../scripts/output/expected_outcomes_demo.csv"))) {
            String line = br.readLine(); // header
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",", -1);
                Map<String, String> row = new HashMap<>();
                row.put("order_id", parts[0].trim());
                row.put("expected", parts[5].trim());
                row.put("received", parts[6].trim());
                row.put("status", parts[8].trim());
                row.put("money_at_risk", parts[9].trim());
                expected.add(row);
            }
        }

        // Compare every reconciliation to expected outcomes
        int mismatches = 0;
        List<String> mismatchRows = new ArrayList<>();
        List<OrderReconciliation> allRecon = new ArrayList<>(reconRepository.findAll());

        for (OrderReconciliation recon : allRecon) {
            String orderId = recon.getOrderId();
            Optional<Map<String, String>> expectedRow = expected.stream()
                    .filter(r -> r.get("order_id").equals(orderId))
                    .findFirst();

            if (expectedRow.isPresent()) {
                Map<String, String> exp = expectedRow.get();
                String actualStatus = recon.getReconciliationStatus();
                String expectedStatus = exp.get("status");
                String actualExpected = recon.getExpectedSettlement() != null ? recon.getExpectedSettlement().stripTrailingZeros().toPlainString() : "0";
                String expectedExpected = new BigDecimal(exp.get("expected")).stripTrailingZeros().toPlainString();
                String actualReceived = recon.getReceivedSettlement() != null ? recon.getReceivedSettlement().stripTrailingZeros().toPlainString() : "0";
                String expectedReceived = new BigDecimal(exp.get("received")).stripTrailingZeros().toPlainString();

                String actualMoneyAtRisk = "0";
                if ("UNPAID_PAST_DUE".equals(actualStatus) || "SHORT_PAID".equals(actualStatus)) {
                    if (recon.getSettlementDiff() != null) {
                        actualMoneyAtRisk = recon.getSettlementDiff().abs().stripTrailingZeros().toPlainString();
                    }
                }
                String expectedMoneyAtRisk = new BigDecimal(exp.get("money_at_risk")).stripTrailingZeros().toPlainString();

                boolean statusMatch = actualStatus.equals(expectedStatus);
                boolean expectedMatch = actualExpected.equals(expectedExpected);
                boolean receivedMatch = actualReceived.equals(expectedReceived);
                boolean moneyAtRiskMatch = actualMoneyAtRisk.equals(expectedMoneyAtRisk);

                if (!statusMatch || !expectedMatch || !receivedMatch || !moneyAtRiskMatch) {
                    mismatches++;
                    mismatchRows.add(String.format(
                            "order=%s actualStatus=%s expStatus=%s actualExpected=%s expExpected=%s actualReceived=%s expReceived=%s actualMar=%s expMar=%s",
                            orderId, actualStatus, expectedStatus, actualExpected, expectedExpected,
                            actualReceived, expectedReceived, actualMoneyAtRisk, expectedMoneyAtRisk
                    ));
                }
            }
        }

        System.out.println("compared " + total + ", mismatches " + mismatches);
        for (String m : mismatchRows) {
            System.out.println("  MISMATCH: " + m);
        }

        assertEquals(0, mismatches, "Expected 0 mismatches but got " + mismatches);
    }
}