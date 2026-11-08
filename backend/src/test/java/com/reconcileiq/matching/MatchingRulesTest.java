package com.reconcileiq.matching;

import com.reconcileiq.ReconcileIqApplication;
import com.reconcileiq.auth.User;
import com.reconcileiq.auth.UserRepository;
import com.reconcileiq.ingestion.Source;
import com.reconcileiq.ingestion.SourceRepository;
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

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MatchingService matchingService;

    @Autowired
    private com.reconcileiq.ingestion.IngestionService ingestionService;

    @Autowired
    private ColumnMappingRepository mappingRepository;

    @Autowired
    private SourceRepository sourceRepository;

    private Long websiteId;

    @BeforeEach
    void setup() throws Exception {
        // Seed sources for Website channel
        Source website = new Source("Website", "WEBSITE", "CHANNEL", "Website", null);
        website = sourceRepository.save(website);
        websiteId = website.getId();

        // Seed column mappings
        saveMapping(websiteId, "orders",
                "{\"orderId\": \"OrderID\", \"channelCode\": \"ChannelCode\", \"orderDate\": \"OrderDate\", \"orderValue\": \"OrderValue\", \"paymentType\": \"PaymentType\", \"deliveryStatus\": \"DeliveryStatus\", \"customerName\": \"CustomerName\", \"customerPhone\": \"CustomerPhone\", \"customerAddress\": \"CustomerAddress\", \"expectedAmountOverride\": \"expected_amount_override\"}");

        // Create users
        User exec = new User();
        exec.setEmail("exec@demo.com");
        exec.setPasswordHash(passwordEncoder.encode("ExecPassword123!"));
        exec.setRole("FINANCE_EXECUTIVE");
        exec.setFullName("Finance Executive");
        exec.setCreatedAt(Instant.now());
        userRepository.save(exec);

        // Login to get token for API calls if needed
        // Ingest demo files from scripts/output
        ingestFile("orders_demo.csv", "orders");
        ingestFile("settlements_demo.csv", "settlement");
        ingestFile("cod_remittances_demo.csv", "cod_remittance");
        ingestFile("freight_invoices_demo.csv", "freight_invoice");

        // Run matching
        matchingService.runMatching(LocalDate.of(2026, 11, 6));
    }

    private void saveMapping(Long sourceId, String fileType, String json) {
        com.reconcileiq.mapping.ColumnMapping cm = new com.reconcileiq.mapping.ColumnMapping();
        cm.setSourceId(sourceId);
        cm.setFileType(fileType);
        cm.setVersion(1);
        cm.setMappingsJson(json);
        cm.setIsActive(true);
        cm.setCreatedBy("system");
        cm.setApprovedAt(java.time.Instant.now());
        mappingRepository.save(cm);
    }

    private void ingestFile(String fileName, String fileType) throws Exception {
        java.io.File file = new java.io.File("../scripts/output/" + fileName);
        if (!file.exists()) {
            throw new IllegalStateException("Demo file not found: " + file.getAbsolutePath());
        }
        try (java.io.FileInputStream is = new java.io.FileInputStream(file)) {
            org.springframework.mock.web.MockMultipartFile mf = new org.springframework.mock.web.MockMultipartFile("file", fileName, "text/csv", is.readAllBytes());
            ingestionService.ingestFullFile(mf, websiteId, fileType);
        }
    }

    @Test
    void test100Vs1001() throws Exception {
        // Rule A7: TCS/TDS excluded from money at risk
        // Boundary 10.00 is within tolerance, 10.01 is flagged
        fail("Implement targeted assertions matching the specification. " +
                "Test that 10.00 difference is within tolerance (SETTLED), " +
                "and 10.01 difference is flagged as SHORT_PAID.");
    }

    @Test
    void testShortfall46() throws Exception {
        // Rule A2: ORD-W-204377 expected -96, received -142, shortfall 46
        fail("Implement assertion: return -96/-142 SHORT_PAID shortfall 46");
    }

    @Test
    void testDuplicateSettlement() throws Exception {
        // Rule: duplicate settlement not double-counted (dedup key: order_id, reference_no, net_amount, settlement_date)
        fail("Implement duplicate settlement detection test");
    }

    @Test
    void testTCSTDSExcluded() throws Exception {
        // Rule A7: TCS and TDS excluded from money at risk
        fail("Implement TCS/TDS exclusion test");
    }

    @Test
    void testPendingNotYetDue() throws Exception {
        // Rule A4: not-yet-due status PENDING for IN_TRANSIT/CANCELLED
        fail("Implement PENDING status test");
    }

    @Test
    void testIdempotentReRun() throws Exception {
        // Rule: re-running matching should produce identical results
        fail("Implement idempotent re-run test");
    }
}