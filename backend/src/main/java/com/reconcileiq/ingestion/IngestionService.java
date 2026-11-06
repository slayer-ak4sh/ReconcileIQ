package com.reconcileiq.ingestion;

import com.reconcileiq.mapping.ColumnMapping;
import com.reconcileiq.mapping.ColumnMappingRepository;
import com.reconcileiq.mapping.MappingSuggester;
import com.reconcileiq.mapping.RuleBasedSuggester;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class IngestionService {

    private final SourceRepository sourceRepository;
    private final ColumnMappingRepository mappingRepository;
    public final UploadBatchRepository batchRepository;
    private final OrderRepository orderRepository;
    private final SettlementRepository settlementRepository;
    private final CodRemittanceRepository codRepository;
    private final FreightInvoiceRepository freightRepository;
    private final CsvParser csvParser;
    private final ExcelParser excelParser;
    private final RuleBasedSuggester ruleBasedSuggester;

    @Autowired
    public IngestionService(SourceRepository sourceRepository,
                            ColumnMappingRepository mappingRepository,
                            UploadBatchRepository batchRepository,
                            OrderRepository orderRepository,
                            SettlementRepository settlementRepository,
                            CodRemittanceRepository codRepository,
                            FreightInvoiceRepository freightRepository,
                            CsvParser csvParser,
                            ExcelParser excelParser,
                            RuleBasedSuggester ruleBasedSuggester) {
        this.sourceRepository = sourceRepository;
        this.mappingRepository = mappingRepository;
        this.batchRepository = batchRepository;
        this.orderRepository = orderRepository;
        this.settlementRepository = settlementRepository;
        this.codRepository = codRepository;
        this.freightRepository = freightRepository;
        this.csvParser = csvParser;
        this.excelParser = excelParser;
        this.ruleBasedSuggester = ruleBasedSuggester;
    }

    // ---------- File Upload & Parsing ----------

    public UploadBatch uploadAndParse(MultipartFile file, Long sourceId, String fileType) throws Exception {
        Source source = sourceRepository.findById(sourceId)
                .orElseThrow(() -> new IllegalArgumentException("Source not found: " + sourceId));

        String fileHash = computeHash(file);
        if (batchRepository.findByFileHash(fileHash).isPresent()) {
            throw new IllegalStateException("Duplicate file upload detected (same hash)");
        }

        String contentType = file.getContentType();
        boolean isExcel = contentType != null && (
                contentType.equals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet") ||
                contentType.equals("application/vnd.ms-excel")
        );

        UploadBatch batch = new UploadBatch(source, fileType, file.getOriginalFilename(), fileHash, getCurrentUserEmail());
        batch = batchRepository.save(batch);

        try {
            List<Map<String, String>> allRows = new ArrayList<>();

            ExcelParser.ParseResult excelResult = null;
            CsvParser.ParseResult csvResult = null;

            if (isExcel) {
                excelResult = excelParser.parse(file, 20, allRows::add);
            } else {
                csvResult = csvParser.parse(file, 20, allRows::add);
            }

            int totalRows = isExcel ? excelResult.totalRows : csvResult.totalRows;
            int validRows = isExcel ? excelResult.validRows : csvResult.validRows;
            int errorRows = isExcel ? excelResult.errorRows : csvResult.errorRows;
            List<Map<String, String>> previewRows = isExcel ? excelResult.previewRows : csvResult.previewRows;
            List<String> headers = isExcel ? excelResult.headers : csvResult.headers;

            batch.setRowCount(totalRows);
            batch.setValidRowCount(validRows);
            batch.setErrorCount(errorRows);
            batch.setStatus("COMPLETED");
            batchRepository.save(batch);

            // Store for mapping suggestion preview
            batch.setPreviewRows(previewRows);
            batch.setHeaders(headers);

        } catch (Exception e) {
            batch.setStatus("FAILED");
            batchRepository.save(batch);
            throw e;
        }

        return batch;
    }

    // ---------- Mapping Suggestions ----------

    public MappingSuggestionResponse suggestMapping(Long sourceId, String fileType) throws Exception {
        Source source = sourceRepository.findById(sourceId)
                .orElseThrow(() -> new IllegalArgumentException("Source not found"));

        UploadBatch latestBatch = batchRepository.findBySourceIdAndFileType(sourceId, fileType)
                .stream()
                .max(Comparator.comparing(UploadBatch::getCreatedAt))
                .orElseThrow(() -> new IllegalStateException("No uploaded file found for this source/type"));

        List<String> headers = latestBatch.getHeaders();
        List<Map<String, String>> previewRows = latestBatch.getPreviewRows();

        List<MappingSuggester.Suggestion> suggestions = ruleBasedSuggester.suggest(headers, previewRows);

        return new MappingSuggestionResponse(sourceId, fileType, suggestions, previewRows);
    }

    // ---------- Mapping Approval & Versioning ----------

    @Transactional
    public ColumnMapping approveMapping(Long sourceId, String fileType, Map<String, String> mappings, String approvedBy) {
        Source source = sourceRepository.findById(sourceId)
                .orElseThrow(() -> new IllegalArgumentException("Source not found"));

        // Deactivate previous active mapping
        mappingRepository.findBySourceIdAndFileTypeAndIsActiveTrue(sourceId, fileType)
                .ifPresent(m -> {
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
        return mappingRepository.save(mapping);
    }

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
        target.setApprovedAt(java.time.Instant.now());
        return mappingRepository.save(target);
    }

    public List<ColumnMapping> getMappingHistory(Long sourceId, String fileType) {
        return mappingRepository.findBySourceIdAndFileTypeOrderByVersionDesc(sourceId, fileType);
    }

    // ---------- Data Ingestion (after mapping approved) ----------

    @Transactional
    public IngestResult ingestMappedData(Long batchId) {
        UploadBatch batch = batchRepository.findById(batchId)
                .orElseThrow(() -> new IllegalArgumentException("Batch not found"));

        Source source = sourceRepository.findById(batch.getSourceId())
                .orElseThrow(() -> new IllegalArgumentException("Source not found"));

        ColumnMapping mapping = mappingRepository.findBySourceIdAndFileTypeAndIsActiveTrue(source.getId(), batch.getFileType())
                .orElseThrow(() -> new IllegalStateException("No active mapping for this source/type"));

        Map<String, String> fieldMap = parseMappingJson(mapping.getMappingsJson());

        int inserted = 0;
        int errors = 0;

        // Re-parse full file (simplified: in production, store parsed data or re-read)
        // For now, we assume the batch preview rows are representative
        // Real implementation would re-read the file from storage

        return new IngestResult(inserted, errors);
    }

    private String computeHash(MultipartFile file) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (var is = file.getInputStream()) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                digest.update(buffer, 0, bytesRead);
            }
        }
        return bytesToHex(digest.digest());
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    public UploadBatchRepository getBatchRepository() {
        return batchRepository;
    }

    private String getCurrentUserEmail() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : "system";
    }

    private Map<String, String> parseMappingJson(String json) {
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().readValue(json, Map.class);
        } catch (Exception e) {
            throw new IllegalStateException("Invalid mapping JSON", e);
        }
    }

    // DTOs
    public static class MappingSuggestionResponse {
        public final Long sourceId;
        public final String fileType;
        public final List<MappingSuggester.Suggestion> suggestions;
        public final List<Map<String, String>> previewRows;

        public MappingSuggestionResponse(Long sourceId, String fileType,
                                         List<MappingSuggester.Suggestion> suggestions,
                                         List<Map<String, String>> previewRows) {
            this.sourceId = sourceId;
            this.fileType = fileType;
            this.suggestions = suggestions;
            this.previewRows = previewRows;
        }
    }

    public static class IngestResult {
        public final int inserted;
        public final int errors;

        public IngestResult(int inserted, int errors) {
            this.inserted = inserted;
            this.errors = errors;
        }
    }
}