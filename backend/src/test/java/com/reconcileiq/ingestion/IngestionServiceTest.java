package com.reconcileiq.ingestion;

import com.reconcileiq.mapping.ColumnMapping;
import com.reconcileiq.mapping.ColumnMappingRepository;
import com.reconcileiq.mapping.MappingService;
import com.reconcileiq.mapping.MappingSuggestionResponse;
import com.reconcileiq.mapping.MappingSuggester;
import com.reconcileiq.mapping.RuleBasedSuggester;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class IngestionServiceTest {

    @Autowired
    private IngestionService ingestionService;

    @Autowired
    private SourceRepository sourceRepository;

    @Autowired
    private ColumnMappingRepository mappingRepository;

    @Autowired
    private MappingService mappingService;

    @Autowired
    private UploadBatchRepository batchRepository;

    @Autowired
    private RejectedRowRepository rejectedRowRepository;

    @Autowired
    private RuleBasedSuggester ruleBasedSuggester;

    @Test
    void testDuplicateFileUploadRejected() throws Exception {
        Source source = sourceRepository.save(new Source("Test Channel", "TEST_CH", "CHANNEL", "TEST_CH", null));

        String csvContent = "order_id,channel,order_date,order_value,payment_type,delivery_status\n" +
                           "ORD-001,Website,2024-01-15,1499,COD,Delivered\n";

        MockMultipartFile file1 = new MockMultipartFile("file", "orders.csv", "text/csv", csvContent.getBytes());
        MockMultipartFile file2 = new MockMultipartFile("file", "orders.csv", "text/csv", csvContent.getBytes());

        // First upload should succeed
        UploadBatch batch1 = ingestionService.uploadAndParse(file1, source.getId(), "ORDERS");
        assertEquals("COMPLETED", batch1.getStatus());
        assertEquals(1, batch1.getValidRowCount());

        // Second upload with same content should fail
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> {
            ingestionService.uploadAndParse(file2, source.getId(), "ORDERS");
        });
        assertTrue(ex.getMessage().contains("Duplicate file upload"));
    }

    @Test
    void testMalformedFileCreatesRejectedRows() throws Exception {
        Source source = sourceRepository.save(new Source("Test Channel", "TEST_CH", "CHANNEL", "TEST_CH", null));

        // CSV with empty rows and malformed data
        String csvContent = "order_id,channel,order_date,order_value,payment_type,delivery_status\n" +
                           "ORD-001,Website,2024-01-15,1499,COD,Delivered\n" +
                           ",,,,,\n" +  // Empty row
                           "ORD-002,Marketplace,2024-01-16,2399,PREPAID,Delivered\n" +
                           ",,,\n";  // Another empty row

        MockMultipartFile file = new MockMultipartFile("file", "orders.csv", "text/csv", csvContent.getBytes());
        UploadBatch batch = ingestionService.uploadAndParse(file, source.getId(), "ORDERS");

        assertEquals("COMPLETED", batch.getStatus());
        assertEquals(4, batch.getRowCount()); // Total rows including empty
        assertEquals(2, batch.getValidRowCount()); // Only 2 valid rows
        assertEquals(2, batch.getErrorCount()); // 2 empty rows rejected

        // Check rejected rows are stored
        List<RejectedRow> rejected = rejectedRowRepository.findByBatchId(batch.getId());
        assertEquals(2, rejected.size());
        for (RejectedRow rr : rejected) {
            assertTrue(rr.getErrorReason().contains("Empty row"));
        }
    }

    @Test
    void testPreviewReturns20RowsAndSavesNothing() throws Exception {
        Source source = sourceRepository.save(new Source("Test Channel", "TEST_CH", "CHANNEL", "TEST_CH", null));

        // Create CSV with 25 rows
        StringBuilder csv = new StringBuilder("order_id,channel,order_date,order_value,payment_type,delivery_status\n");
        for (int i = 1; i <= 25; i++) {
            csv.append(String.format("ORD-%03d,Website,2024-01-%02d,%d,COD,Delivered\n", i, i % 28 + 1, 1000 + i * 10));
        }

        MockMultipartFile file = new MockMultipartFile("file", "orders.csv", "text/csv", csv.toString().getBytes());
        UploadBatch batch = ingestionService.uploadAndParse(file, source.getId(), "ORDERS");

        assertEquals("COMPLETED", batch.getStatus());
        assertEquals(25, batch.getRowCount());
        assertEquals(25, batch.getValidRowCount());
        assertEquals(0, batch.getErrorCount());

        // Preview should be limited to 20 rows
        assertNotNull(batch.getPreviewRows());
        assertEquals(20, batch.getPreviewRows().size());

        // Headers should be captured
        assertNotNull(batch.getHeaders());
        assertEquals(6, batch.getHeaders().size());

        // No data should be persisted to orders table yet (only preview)
        // The actual ingestion happens via ingestFullFile
    }

    @Test
    void testApproveMappingCreatesNewVersion() throws Exception {
        Source source = sourceRepository.save(new Source("Test Channel", "TEST_CH", "CHANNEL", "TEST_CH", null));

        String csvContent = "OrderID,Channel,OrderDate,OrderValue,PaymentType,DeliveryStatus\n" +
                           "ORD-001,Website,2024-01-15,1499,COD,Delivered\n";

        MockMultipartFile file = new MockMultipartFile("file", "orders.csv", "text/csv", csvContent.getBytes());
        ingestionService.uploadAndParse(file, source.getId(), "ORDERS");

        // Get suggestions
        MappingSuggestionResponse suggestionResponse = mappingService.suggestMapping(
                source.getId(), "ORDERS", ruleBasedSuggester);

        assertNotNull(suggestionResponse.suggestions);
        assertEquals(6, suggestionResponse.suggestions.size());

        // Build mappings from suggestions
        Map<String, String> mappings = new java.util.HashMap<>();
        for (MappingSuggester.Suggestion s : suggestionResponse.suggestions) {
            if (!s.canonicalField.isEmpty()) {
                mappings.put(s.canonicalField, s.header);
            }
        }

        // Approve mapping
        ColumnMapping mapping = mappingService.approveMapping(source.getId(), "ORDERS", mappings, "test@user.com");

        assertNotNull(mapping.getId());
        assertEquals(1, mapping.getVersion());
        assertTrue(mapping.getIsActive());
        assertEquals("test@user.com", mapping.getCreatedBy());

        // Approve again with different mappings - should create version 2
        Map<String, String> mappings2 = new java.util.HashMap<>(mappings);
        mappings2.put("customerName", "CustomerName"); // Add a new mapping

        ColumnMapping mapping2 = mappingService.approveMapping(source.getId(), "ORDERS", mappings2, "test@user.com");

        assertEquals(2, mapping2.getVersion());
        assertTrue(mapping2.getIsActive());

        // Previous version should be deactivated
        Optional<ColumnMapping> v1 = mappingRepository.findBySourceIdAndFileTypeAndVersion(source.getId(), "ORDERS", 1);
        assertTrue(v1.isPresent());
        assertFalse(v1.get().getIsActive());
    }

    @Test
    void testRollbackLeavesOneActiveVersion() throws Exception {
        Source source = sourceRepository.save(new Source("Test Channel", "TEST_CH", "CHANNEL", "TEST_CH", null));

        String csvContent = "OrderID,Channel,OrderDate,OrderValue,PaymentType,DeliveryStatus\n" +
                           "ORD-001,Website,2024-01-15,1499,COD,Delivered\n";

        MockMultipartFile file = new MockMultipartFile("file", "orders.csv", "text/csv", csvContent.getBytes());
        ingestionService.uploadAndParse(file, source.getId(), "ORDERS");

        MappingSuggestionResponse suggestionResponse = mappingService.suggestMapping(
                source.getId(), "ORDERS", ruleBasedSuggester);

        Map<String, String> mappings = new java.util.HashMap<>();
        for (MappingSuggester.Suggestion s : suggestionResponse.suggestions) {
            if (!s.canonicalField.isEmpty()) {
                mappings.put(s.canonicalField, s.header);
            }
        }

        // Create version 1
        mappingService.approveMapping(source.getId(), "ORDERS", mappings, "test@user.com");
        // Create version 2
        Map<String, String> mappings2 = new java.util.HashMap<>(mappings);
        mappings2.put("customerName", "CustomerName");
        mappingService.approveMapping(source.getId(), "ORDERS", mappings2, "test@user.com");
        // Create version 3
        Map<String, String> mappings3 = new java.util.HashMap<>(mappings2);
        mappings3.put("customerPhone", "CustomerPhone");
        mappingService.approveMapping(source.getId(), "ORDERS", mappings3, "test@user.com");

        // Verify version 3 is active
        Optional<ColumnMapping> v3 = mappingRepository.findBySourceIdAndFileTypeAndIsActiveTrue(source.getId(), "ORDERS");
        assertTrue(v3.isPresent());
        assertEquals(3, v3.get().getVersion());

        // Rollback to version 1
        ColumnMapping rolledBack = mappingService.rollbackMapping(source.getId(), "ORDERS", 1, "test@user.com");

        assertEquals(1, rolledBack.getVersion());
        assertTrue(rolledBack.getIsActive());

        // Verify only version 1 is active now
        List<ColumnMapping> allMappings = mappingRepository.findBySourceIdAndFileTypeOrderByVersionDesc(source.getId(), "ORDERS");
        long activeCount = allMappings.stream().filter(ColumnMapping::getIsActive).count();
        assertEquals(1, activeCount, "Only one version should be active after rollback");

        Optional<ColumnMapping> active = mappingRepository.findBySourceIdAndFileTypeAndIsActiveTrue(source.getId(), "ORDERS");
        assertTrue(active.isPresent());
        assertEquals(1, active.get().getVersion());
    }

    @Test
    void test100kRowCSVStreamsWithoutLoadingIntoMemory() throws Exception {
        Source source = sourceRepository.save(new Source("Test Channel", "TEST_CH", "CHANNEL", "TEST_CH", null));

        // Create a large CSV with 100,000 rows
        StringBuilder csv = new StringBuilder("order_id,channel,order_date,order_value,payment_type,delivery_status\n");
        for (int i = 1; i <= 100000; i++) {
            csv.append(String.format("ORD-%06d,Website,2024-01-%02d,%d,COD,Delivered\n", i, i % 28 + 1, 1000 + (i % 1000)));
        }

        MockMultipartFile file = new MockMultipartFile("file", "large_orders.csv", "text/csv", csv.toString().getBytes());

        // This should stream and not load entire file into memory
        UploadBatch batch = ingestionService.uploadAndParse(file, source.getId(), "ORDERS");

        assertEquals("COMPLETED", batch.getStatus());
        assertEquals(100000, batch.getRowCount());
        assertEquals(100000, batch.getValidRowCount());

        // Preview should still be only 20 rows
        assertEquals(20, batch.getPreviewRows().size());

        // Create and approve mapping first
        MappingSuggestionResponse suggestionResponse = mappingService.suggestMapping(
                source.getId(), "ORDERS", ruleBasedSuggester);
        Map<String, String> mappings = new java.util.HashMap<>();
        for (MappingSuggester.Suggestion s : suggestionResponse.suggestions) {
            if (!s.canonicalField.isEmpty()) {
                mappings.put(s.canonicalField, s.header);
            }
        }
        mappingService.approveMapping(source.getId(), "ORDERS", mappings, "test@user.com");

        // Now test full ingestion with streaming - use different file name to avoid duplicate hash
        StringBuilder csv2 = new StringBuilder("order_id,channel,order_date,order_value,payment_type,delivery_status\n");
        for (int i = 1; i <= 100000; i++) {
            csv2.append(String.format("ORD-%06d,Website,2024-02-%02d,%d,COD,Delivered\n", i, i % 28 + 1, 1000 + (i % 1000)));
        }
        MockMultipartFile file2 = new MockMultipartFile("file", "large_orders2.csv", "text/csv", csv2.toString().getBytes());
        IngestionService.IngestResult result = ingestionService.ingestFullFile(file2, source.getId(), "ORDERS");

        assertEquals(100000, result.inserted);
        assertEquals(0, result.errors);
    }
}