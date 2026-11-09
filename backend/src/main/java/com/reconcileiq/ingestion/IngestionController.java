package com.reconcileiq.ingestion;

import com.reconcileiq.mapping.MappingService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/uploads")
public class IngestionController {

    private final IngestionService ingestionService;
    private final SourceRepository sourceRepository;
    private final MappingService mappingService;
    private final RejectedRowRepository rejectedRowRepository;

    public IngestionController(IngestionService ingestionService,
                               SourceRepository sourceRepository,
                               MappingService mappingService,
                               RejectedRowRepository rejectedRowRepository) {
        this.ingestionService = ingestionService;
        this.sourceRepository = sourceRepository;
        this.mappingService = mappingService;
        this.rejectedRowRepository = rejectedRowRepository;
    }

    @GetMapping("/sources")
    @PreAuthorize("hasAnyAuthority('FINANCE_EXECUTIVE', 'COFOUNDER')")
    public ResponseEntity<List<SourceDTO>> listSources() {
        List<SourceDTO> dtos = sourceRepository.findAll().stream()
                .map(SourceDTO::fromEntity)
                .toList();
        return ResponseEntity.ok(dtos);
    }

    @PostMapping(consumes = "multipart/form-data")
    @PreAuthorize("hasAnyAuthority('FINANCE_EXECUTIVE', 'COFOUNDER')")
    public ResponseEntity<?> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("sourceId") Long sourceId,
            @RequestParam("fileType") String fileType) {
        try {
            UploadBatch batch = ingestionService.uploadAndParse(file, sourceId, fileType);
            return ResponseEntity.ok(Map.of(
                    "batchId", batch.getId(),
                    "status", batch.getStatus(),
                    "totalRows", batch.getRowCount(),
                    "validRows", batch.getValidRowCount(),
                    "errorRows", batch.getErrorCount(),
                    "headers", batch.getHeaders(),
                    "previewRows", batch.getPreviewRows()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping(value = "/{batchId}/ingest")
    @PreAuthorize("hasAnyAuthority('FINANCE_EXECUTIVE', 'COFOUNDER')")
    public ResponseEntity<?> ingestFullFile(
            @PathVariable Long batchId) {
        try {
            IngestionService.IngestResult result = ingestionService.ingestMappedData(batchId);
            return ResponseEntity.ok(Map.of(
                    "inserted", result.inserted,
                    "errors", result.errors
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('FINANCE_EXECUTIVE', 'COFOUNDER')")
    public ResponseEntity<List<UploadBatchDTO>> listUploads(
            @RequestParam(required = false) Long sourceId,
            @RequestParam(required = false) String fileType) {
        List<UploadBatch> batches;
        if (sourceId != null && fileType != null) {
            batches = sourceRepository.findById(sourceId)
                    .map(s -> s.getUploadBatches().stream()
                            .filter(b -> b.getFileType().equals(fileType))
                            .toList())
                    .orElse(List.of());
        } else if (sourceId != null) {
            batches = sourceRepository.findById(sourceId).map(Source::getUploadBatches).orElse(List.of());
        } else {
            batches = sourceRepository.findAll().stream()
                    .flatMap(s -> s.getUploadBatches().stream()
                            .peek(b -> b.getSource())) // ensure lazy load
                    .toList();
        }
        List<UploadBatchDTO> dtos = batches.stream().map(UploadBatchDTO::fromEntity).toList();
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('FINANCE_EXECUTIVE', 'COFOUNDER')")
    public ResponseEntity<UploadBatchDTO> getUpload(@PathVariable Long id) {
        return ingestionService.getBatchRepository().findById(id)
                .map(UploadBatchDTO::fromEntity)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/rejected")
    @PreAuthorize("hasAnyAuthority('FINANCE_EXECUTIVE', 'COFOUNDER')")
    public ResponseEntity<List<RejectedRow>> getRejectedRows(@PathVariable Long id) {
        return ResponseEntity.ok(rejectedRowRepository.findByBatchId(id));
    }
}
