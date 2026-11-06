package com.reconcileiq.ingestion;

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

    public IngestionController(IngestionService ingestionService,
                               SourceRepository sourceRepository) {
        this.ingestionService = ingestionService;
        this.sourceRepository = sourceRepository;
    }

    @PostMapping(consumes = "multipart/form-data")
    @PreAuthorize("hasRole('ROLE_FINANCE_EXECUTIVE')")
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

    @GetMapping
    @PreAuthorize("hasRole('ROLE_FINANCE_EXECUTIVE')")
    public ResponseEntity<List<UploadBatch>> listUploads(
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
                    .flatMap(s -> s.getUploadBatches().stream())
                    .toList();
        }
        return ResponseEntity.ok(batches);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_FINANCE_EXECUTIVE')")
    public ResponseEntity<UploadBatch> getUpload(@PathVariable Long id) {
        return ingestionService.getBatchRepository().findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}