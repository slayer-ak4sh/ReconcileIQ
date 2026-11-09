package com.reconcileiq.mapping;

import com.reconcileiq.ingestion.Source;
import com.reconcileiq.ingestion.SourceRepository;
import com.reconcileiq.ingestion.UploadBatch;
import com.reconcileiq.ingestion.UploadBatchRepository;
import com.reconcileiq.common.AuditLogService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class MappingService {

    private final ColumnMappingRepository mappingRepository;
    private final SourceRepository sourceRepository;
    private final UploadBatchRepository batchRepository;
    private final AuditLogService auditLogService;

    public MappingService(ColumnMappingRepository mappingRepository,
                          SourceRepository sourceRepository,
                          UploadBatchRepository batchRepository,
                          AuditLogService auditLogService) {
        this.mappingRepository = mappingRepository;
        this.sourceRepository = sourceRepository;
        this.batchRepository = batchRepository;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public ColumnMapping approveMapping(Long sourceId, String fileType, Map<String, String> mappings, String approvedBy) {
        Source source = sourceRepository.findById(sourceId)
                .orElseThrow(() -> new IllegalArgumentException("Source not found"));

        // Deactivate previous active mapping
        Optional<ColumnMapping> previousActive = mappingRepository.findBySourceIdAndFileTypeAndIsActiveTrue(sourceId, fileType);
        previousActive.ifPresent(m -> {
            m.setIsActive(false);
            mappingRepository.save(m);
        });

        // Get next version
        int nextVersion = mappingRepository.findBySourceIdAndFileTypeOrderByVersionDesc(sourceId, fileType)
                .stream()
                .mapToInt(ColumnMapping::getVersion)
                .max()
                .orElse(0) + 1;

        String mappingsJson;
        try {
            mappingsJson = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(mappings);
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize mappings", e);
        }

        ColumnMapping mapping = new ColumnMapping(sourceId, fileType, nextVersion, mappingsJson, approvedBy);
        mapping.setIsActive(true);
        ColumnMapping saved = mappingRepository.save(mapping);

        // Audit log
        auditLogService.log(approvedBy, "MAPPING_APPROVED", "ColumnMapping", saved.getId().toString(),
                Map.of("sourceId", sourceId, "fileType", fileType, "version", nextVersion, "mappings", mappings));

        return saved;
    }

    @Transactional
    public ColumnMapping rollbackMapping(Long sourceId, String fileType, int targetVersion, String rolledBackBy) {
        ColumnMapping target = mappingRepository.findBySourceIdAndFileTypeAndVersion(sourceId, fileType, targetVersion)
                .orElseThrow(() -> new IllegalArgumentException("Mapping version not found"));

        // Deactivate current active
        mappingRepository.findBySourceIdAndFileTypeAndIsActiveTrue(sourceId, fileType)
                .ifPresent(m -> {
                    m.setIsActive(false);
                    mappingRepository.save(m);
                });

        // Reactivate target
        target.setIsActive(true);
        target.setApprovedAt(Instant.now());
        ColumnMapping saved = mappingRepository.save(target);

        // Audit log
        auditLogService.log(rolledBackBy, "MAPPING_ROLLBACK", "ColumnMapping", saved.getId().toString(),
                Map.of("sourceId", sourceId, "fileType", fileType, "targetVersion", targetVersion));

        return saved;
    }

    public List<ColumnMapping> getMappingHistory(Long sourceId, String fileType) {
        return mappingRepository.findBySourceIdAndFileTypeOrderByVersionDesc(sourceId, fileType);
    }

    public Optional<ColumnMapping> getActiveMapping(Long sourceId, String fileType) {
        return mappingRepository.findBySourceIdAndFileTypeAndIsActiveTrue(sourceId, fileType);
    }

    public Optional<ColumnMapping> getMappingByVersion(Long sourceId, String fileType, int version) {
        return mappingRepository.findBySourceIdAndFileTypeAndVersion(sourceId, fileType, version);
    }

    @Transactional
    public MappingSuggestionResponse suggestMapping(Long sourceId, String fileType,
                                                      MappingSuggester suggester) throws Exception {
        Source source = sourceRepository.findById(sourceId)
                .orElseThrow(() -> new IllegalArgumentException("Source not found"));

        Optional<UploadBatch> latestBatchOpt = batchRepository.findBySourceIdAndFileType(sourceId, fileType)
                .stream()
                .max((a, b) -> a.getCreatedAt().compareTo(b.getCreatedAt()));

        if (latestBatchOpt.isEmpty()) {
            // No upload found - return empty suggestions with empty headers
            return new MappingSuggestionResponse(sourceId, fileType, List.of(), List.of(), true);
        }

        UploadBatch latestBatch = latestBatchOpt.get();

        List<String> headers = latestBatch.getHeaders();
        List<Map<String, String>> previewRows = latestBatch.getPreviewRows();

        if (headers == null || headers.isEmpty()) {
            return new MappingSuggestionResponse(sourceId, fileType, List.of(), List.of(), true);
        }

        List<MappingSuggester.Suggestion> suggestions = suggester.suggest(headers, previewRows);

        // Check if all headers are already mapped in active version
        Optional<ColumnMapping> active = getActiveMapping(sourceId, fileType);
        boolean allMapped = false;
        if (active.isPresent()) {
            Map<String, String> existingMappings = parseMappingJson(active.get().getMappingsJson());
            allMapped = headers.stream().allMatch(h -> existingMappings.containsKey(h));
        }

        return new MappingSuggestionResponse(sourceId, fileType, suggestions, previewRows, allMapped);
    }

    private Map<String, String> parseMappingJson(String json) {
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().readValue(json, Map.class);
        } catch (Exception e) {
            throw new IllegalStateException("Invalid mapping JSON", e);
        }
    }
}