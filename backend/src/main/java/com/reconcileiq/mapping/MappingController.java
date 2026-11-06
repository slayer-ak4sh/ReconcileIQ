package com.reconcileiq.mapping;

import com.reconcileiq.ingestion.IngestionService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/mappings")
public class MappingController {

    private final IngestionService ingestionService;
    private final ColumnMappingRepository mappingRepository;

    public MappingController(IngestionService ingestionService, ColumnMappingRepository mappingRepository) {
        this.ingestionService = ingestionService;
        this.mappingRepository = mappingRepository;
    }

    @GetMapping
    @PreAuthorize("hasRole('ROLE_FINANCE_EXECUTIVE')")
    public ResponseEntity<List<ColumnMapping>> getActiveMappings() {
        return ResponseEntity.ok(mappingRepository.findAll().stream()
                .filter(ColumnMapping::getIsActive)
                .toList());
    }

    @PostMapping("/suggest")
    @PreAuthorize("hasRole('ROLE_FINANCE_EXECUTIVE')")
    public ResponseEntity<?> suggestMapping(@RequestParam Long sourceId, @RequestParam String fileType) {
        try {
            IngestionService.MappingSuggestionResponse response = ingestionService.suggestMapping(sourceId, fileType);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/approve")
    @PreAuthorize("hasRole('ROLE_FINANCE_EXECUTIVE')")
    public ResponseEntity<?> approveMapping(@RequestBody ApproveRequest request) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String approvedBy = auth != null ? auth.getName() : "system";

            ColumnMapping mapping = ingestionService.approveMapping(
                    request.sourceId, request.fileType, request.mappings, approvedBy);
            return ResponseEntity.ok(mapping);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{sourceId}/{fileType}/history")
    @PreAuthorize("hasRole('ROLE_FINANCE_EXECUTIVE')")
    public ResponseEntity<List<ColumnMapping>> getHistory(@PathVariable Long sourceId, @PathVariable String fileType) {
        return ResponseEntity.ok(ingestionService.getMappingHistory(sourceId, fileType));
    }

    @PostMapping("/{id}/rollback")
    @PreAuthorize("hasRole('ROLE_FINANCE_EXECUTIVE')")
    public ResponseEntity<?> rollbackMapping(@PathVariable Long id, @RequestParam int targetVersion) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String rolledBackBy = auth != null ? auth.getName() : "system";

            ColumnMapping mapping = mappingRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Mapping not found"));

            ColumnMapping rolledBack = ingestionService.rollbackMapping(
                    mapping.getSourceId(), mapping.getFileType(), targetVersion, rolledBackBy);
            return ResponseEntity.ok(rolledBack);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    public static class ApproveRequest {
        public Long sourceId;
        public String fileType;
        public Map<String, String> mappings;
    }
}