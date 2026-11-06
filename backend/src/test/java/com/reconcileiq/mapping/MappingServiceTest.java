package com.reconcileiq.mapping;

import com.reconcileiq.common.AuditLog;
import com.reconcileiq.common.AuditLogRepository;
import com.reconcileiq.ingestion.Source;
import com.reconcileiq.ingestion.SourceRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class MappingServiceTest {

    @Autowired
    private MappingService mappingService;

    @Autowired
    private SourceRepository sourceRepository;

    @Autowired
    private ColumnMappingRepository mappingRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Test
    void testApproveMappingCreatesNewVersionAndDeactivatesPrevious() {
        Source source = sourceRepository.save(new Source("Test Source", "SRC_TEST", "CHANNEL", "SRC_TEST", null));

        Map<String, String> mappingsV1 = Map.of("orderId", "Ord No", "orderValue", "Net Payout");
        ColumnMapping v1 = mappingService.approveMapping(source.getId(), "ORDERS", mappingsV1, "exec@demo.com");

        assertNotNull(v1.getId());
        assertEquals(1, v1.getVersion());
        assertTrue(v1.getIsActive());

        // Approve v2
        Map<String, String> mappingsV2 = Map.of("orderId", "Ord No", "orderValue", "Net Payout", "orderDate", "Settled On");
        ColumnMapping v2 = mappingService.approveMapping(source.getId(), "ORDERS", mappingsV2, "exec@demo.com");

        assertEquals(2, v2.getVersion());
        assertTrue(v2.getIsActive());

        // Verify v1 is deactivated
        ColumnMapping v1Reloaded = mappingRepository.findById(v1.getId()).orElseThrow();
        assertFalse(v1Reloaded.getIsActive(), "Previous version must be inactive");

        // Exactly one version must be active
        List<ColumnMapping> activeMappings = mappingRepository.findAll().stream()
                .filter(m -> m.getSourceId().equals(source.getId()) && m.getFileType().equals("ORDERS") && m.getIsActive())
                .toList();
        assertEquals(1, activeMappings.size(), "Exactly one active version expected");
    }

    @Test
    void testRollbackReactivatesOlderVersion() {
        Source source = sourceRepository.save(new Source("Test Source 2", "SRC_TEST_2", "CHANNEL", "SRC_TEST_2", null));

        Map<String, String> mappingsV1 = Map.of("orderId", "Ord No");
        ColumnMapping v1 = mappingService.approveMapping(source.getId(), "ORDERS", mappingsV1, "exec@demo.com");

        Map<String, String> mappingsV2 = Map.of("orderId", "Ord No", "orderValue", "Net Payout");
        ColumnMapping v2 = mappingService.approveMapping(source.getId(), "ORDERS", mappingsV2, "exec@demo.com");

        // Currently v2 is active
        assertTrue(v2.getIsActive());

        // Rollback to v1
        ColumnMapping rolledBack = mappingService.rollbackMapping(source.getId(), "ORDERS", 1, "exec@demo.com");

        assertEquals(1, rolledBack.getVersion());
        assertTrue(rolledBack.getIsActive());

        // Verify v2 is now inactive
        ColumnMapping v2Reloaded = mappingRepository.findById(v2.getId()).orElseThrow();
        assertFalse(v2Reloaded.getIsActive());

        // Exactly one active version
        List<ColumnMapping> activeMappings = mappingRepository.findAll().stream()
                .filter(m -> m.getSourceId().equals(source.getId()) && m.getFileType().equals("ORDERS") && m.getIsActive())
                .toList();
        assertEquals(1, activeMappings.size());
    }

    @Test
    void testAuditLogWrittenForApproveAndRollback() {
        Source source = sourceRepository.save(new Source("Test Source 3", "SRC_TEST_3", "CHANNEL", "SRC_TEST_3", null));

        Map<String, String> mappings = Map.of("orderId", "Ord No");
        ColumnMapping v1 = mappingService.approveMapping(source.getId(), "ORDERS", mappings, "exec@demo.com");

        // Verify audit log for approval
        List<AuditLog> logsApprove = auditLogRepository.findByResourceTypeAndResourceId("ColumnMapping", v1.getId().toString());
        assertFalse(logsApprove.isEmpty());
        assertEquals("MAPPING_APPROVED", logsApprove.get(0).getAction());
        assertEquals("exec@demo.com", logsApprove.get(0).getActorEmail());

        // Rollback
        ColumnMapping v2 = mappingService.approveMapping(source.getId(), "ORDERS", Map.of("orderId", "Ord No 2"), "exec@demo.com");
        ColumnMapping rolledBack = mappingService.rollbackMapping(source.getId(), "ORDERS", 1, "exec@demo.com");

        List<AuditLog> logsRollback = auditLogRepository.findByResourceTypeAndResourceId("ColumnMapping", rolledBack.getId().toString());
        assertTrue(logsRollback.stream().anyMatch(l -> "MAPPING_ROLLBACK".equals(l.getAction())));
    }
}