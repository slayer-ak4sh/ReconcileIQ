package com.reconcileiq.mapping;

import com.reconcileiq.ingestion.IngestionService;
import com.reconcileiq.mapping.RuleBasedSuggester;
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

    private final MappingService mappingService;
    private final RuleBasedSuggester ruleBasedSuggester;

    public MappingController(MappingService mappingService, RuleBasedSuggester ruleBasedSuggester) {
        this.mappingService = mappingService;
        this.ruleBasedSuggester = ruleBasedSuggester;
    }

    @GetMapping("/active")
    @PreAuthorize("hasAuthority('FINANCE_EXECUTIVE')")
    public ResponseEntity<List<ColumnMapping>> getAllActiveMappings() {
        // This would need a custom query - simplified for now
        return ResponseEntity.ok(List.of());
    }

    @PostMapping("/suggest")
    @PreAuthorize("hasAuthority('FINANCE_EXECUTIVE')")
    public ResponseEntity<?> suggestMapping(@RequestParam Long sourceId, @RequestParam String fileType) {
        try {
            MappingSuggestionResponse response = mappingService.suggestMapping(
                    sourceId, fileType, ruleBasedSuggester);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/approve")
    @PreAuthorize("hasAuthority('FINANCE_EXECUTIVE')")
    public ResponseEntity<?> approveMapping(@RequestBody ApproveRequest request) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String approvedBy = auth != null ? auth.getName() : "system";

            ColumnMapping mapping = mappingService.approveMapping(
                    request.sourceId, request.fileType, request.mappings, approvedBy);
            return ResponseEntity.ok(mapping);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{sourceId}/{fileType}/history")
    @PreAuthorize("hasAuthority('FINANCE_EXECUTIVE')")
    public ResponseEntity<List<ColumnMapping>> getHistory(@PathVariable Long sourceId, @PathVariable String fileType) {
        return ResponseEntity.ok(mappingService.getMappingHistory(sourceId, fileType));
    }

    @PostMapping("/{sourceId}/{fileType}/rollback")
    @PreAuthorize("hasAuthority('FINANCE_EXECUTIVE')")
    public ResponseEntity<?> rollbackMappingBySource(@PathVariable Long sourceId, @PathVariable String fileType, @RequestParam int targetVersion) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String rolledBackBy = auth != null ? auth.getName() : "system";

            ColumnMapping rolledBack = mappingService.rollbackMapping(sourceId, fileType, targetVersion, rolledBackBy);
            return ResponseEntity.ok(rolledBack);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{sourceId}/{fileType}/active")
    @PreAuthorize("hasAuthority('FINANCE_EXECUTIVE')")
    public ResponseEntity<ColumnMapping> getActiveMapping(@PathVariable Long sourceId, @PathVariable String fileType) {
        return mappingService.getActiveMapping(sourceId, fileType)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    public static class ApproveRequest {
        public Long sourceId;
        public String fileType;
        public Map<String, String> mappings;
    }
}