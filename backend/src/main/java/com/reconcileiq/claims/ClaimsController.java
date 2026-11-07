package com.reconcileiq.claims;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/claims")
public class ClaimsController {

    private final ClaimsService claimsService;

    public ClaimsController(ClaimsService claimsService) {
        this.claimsService = claimsService;
    }

    private String getCurrentUserEmail() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof com.reconcileiq.auth.UserPrincipal) {
            return ((com.reconcileiq.auth.UserPrincipal) auth.getPrincipal()).getUsername();
        }
        return "unknown";
    }

    private String getUserRole() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof com.reconcileiq.auth.UserPrincipal) {
            return ((com.reconcileiq.auth.UserPrincipal) auth.getPrincipal()).getRole();
        }
        return "FINANCE_EXECUTIVE";
    }

    @PostMapping
    @PreAuthorize("hasAuthority('FINANCE_EXECUTIVE')")
    public Claim createDraft(@RequestBody CreateClaimRequest request) {
        List<ClaimsService.ClaimItemRequest> items = request.items().stream()
                .map(itemReq -> new ClaimsService.ClaimItemRequest(
                        itemReq.orderId(),
                        itemReq.claimType(),
                        itemReq.amount(),
                        itemReq.evidenceNotes()
                ))
                .toList();

        return claimsService.createDraftClaim(
                request.partnerCode(),
                request.partnerType(),
                getCurrentUserEmail(),
                items
        );
    }

    @GetMapping
    @PreAuthorize("hasAuthority('FINANCE_EXECUTIVE') or hasAuthority('COFOUNDER')")
    public Page<Claim> listClaims(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String partner,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        Pageable pageable = PageRequest.of(page, size,
                "desc".equalsIgnoreCase(sortDir) ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending());

        return claimsService.searchClaims(status, partner, pageable);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('FINANCE_EXECUTIVE') or hasAuthority('COFOUNDER')")
    public Claim getClaim(@PathVariable Long id) {
        return claimsService.getClaim(id);
    }

    @PostMapping("/{id}/submit")
    @PreAuthorize("hasAuthority('FINANCE_EXECUTIVE')")
    public Claim submitClaim(@PathVariable Long id) {
        return claimsService.submitClaim(id, getCurrentUserEmail(), getUserRole());
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAuthority('COFOUNDER')")
    public Claim approveClaim(@PathVariable Long id,
                              @RequestParam(required = false) String notes) {
        return claimsService.approveClaim(id, getCurrentUserEmail(), notes, getUserRole());
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAuthority('COFOUNDER')")
    public Claim rejectClaim(@PathVariable Long id,
                             @RequestParam(required = false) String notes) {
        return claimsService.rejectClaim(id, getCurrentUserEmail(), notes, getUserRole());
    }

    @PostMapping("/{id}/recover")
    @PreAuthorize("hasAuthority('FINANCE_EXECUTIVE')")
    public Claim recoverClaim(@PathVariable Long id) {
        return claimsService.markRecovered(id, getCurrentUserEmail(), getUserRole());
    }

    @GetMapping("/{id}/evidence.csv")
    @PreAuthorize("hasAuthority('FINANCE_EXECUTIVE') or hasAuthority('COFOUNDER')")
    public org.springframework.http.ResponseEntity<org.springframework.core.io.InputStreamResource> downloadEvidenceCsv(@PathVariable Long id) {
        String csv = claimsService.generateEvidenceCsv(id);
        org.springframework.core.io.InputStreamResource resource = new org.springframework.core.io.InputStreamResource(new java.io.ByteArrayInputStream(csv.getBytes()));
        return org.springframework.http.ResponseEntity.ok()
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=evidence.csv")
                .contentType(org.springframework.http.MediaType.parseMediaType("text/csv"))
                .body(resource);
    }

    // Request DTO
    public record CreateClaimRequest(
            String partnerCode,
            String partnerType,
            String createdBy,
            List<ClaimItemRequest> items
    ) {}

    public record ClaimItemRequest(
            String orderId,
            String claimType,
            BigDecimal amount,
            String evidenceNotes
    ) {}
}