package com.reconcileiq.claims;

import com.reconcileiq.matching.OrderReconciliation;
import com.reconcileiq.matching.OrderReconciliationRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class ClaimsService {

    private final ClaimRepository claimRepository;
    private final ClaimItemRepository claimItemRepository;
    private final ClaimEventRepository claimEventRepository;
    private final OrderReconciliationRepository reconRepository;

    public ClaimsService(ClaimRepository claimRepository,
                         ClaimItemRepository claimItemRepository,
                         ClaimEventRepository claimEventRepository,
                         OrderReconciliationRepository reconRepository) {
        this.claimRepository = claimRepository;
        this.claimItemRepository = claimItemRepository;
        this.claimEventRepository = claimEventRepository;
        this.reconRepository = reconRepository;
    }

    @Transactional
    public Claim createDraftClaim(String partnerCode, String partnerType, String createdBy,
                                  List<ClaimItemRequest> items) {
        Claim claim = new Claim();
        claim.setClaimNumber("CLM-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        claim.setPartnerCode(partnerCode);
        claim.setPartnerType(partnerType);
        claim.setCreatedBy(createdBy);
        claim.setStatus("DRAFT");

        // Save claim first to generate ID
        claim = claimRepository.save(claim);

        BigDecimal total = BigDecimal.ZERO;
        for (ClaimItemRequest itemReq : items) {
            OrderReconciliation recon = reconRepository.findByOrderId(itemReq.orderId())
                    .orElseThrow(() -> new IllegalArgumentException("Order reconciliation not found: " + itemReq.orderId()));

            ClaimItem item = new ClaimItem();
            item.setClaim(claim);
            item.setOrderId(itemReq.orderId());
            item.setOrderReconciliation(recon);
            item.setClaimType(itemReq.claimType());
            item.setAmount(itemReq.amount());
            item.setEvidenceNotes(itemReq.evidenceNotes());
            claimItemRepository.save(item);
            total = total.add(itemReq.amount());
        }
        claim.setTotalAmount(total);
        return claimRepository.save(claim);
    }

    @Transactional
    public Claim submitClaim(Long claimId, String submittedBy, String userRole) {
        Claim claim = claimRepository.findById(claimId)
                .orElseThrow(() -> new IllegalArgumentException("Claim not found"));

        // Role-based access control: only FINANCE_EXECUTIVE can submit claims
        if (!"FINANCE_EXECUTIVE".equals(userRole)) {
            throw new SecurityException("User with role '" + userRole + "' is not allowed to submit claims");
        }
        if (!"DRAFT".equals(claim.getStatus())) {
            throw new IllegalStateException("Only DRAFT claims can be submitted");
        }
        claim.setStatus("SUBMITTED");
        claim.setSubmittedAt(Instant.now());
        logEvent(claim, "DRAFT", "SUBMITTED", submittedBy, "Claim submitted for review");
        return claimRepository.save(claim);
    }

    @Transactional
    public Claim approveClaim(Long claimId, String decidedBy, String notes, String userRole) {
        Claim claim = claimRepository.findById(claimId)
                .orElseThrow(() -> new IllegalArgumentException("Claim not found"));

        // Role-based access control
        boolean allowed = ("FINANCE_EXECUTIVE".equals(userRole) || "COFOUNDER".equals(userRole))
                && "SUBMITTED".equals(claim.getStatus());
        if (!allowed) {
            throw new SecurityException("User with role '" + userRole + "' is not allowed to approve claims");
        }

        claim.setStatus("APPROVED");
        claim.setDecidedBy(decidedBy);
        claim.setDecidedAt(Instant.now());
        claim.setDecisionNotes(notes);
        logEvent(claim, "SUBMITTED", "APPROVED", decidedBy, notes);
        return claimRepository.save(claim);
    }

    @Transactional
    public Claim rejectClaim(Long claimId, String decidedBy, String notes, String userRole) {
        Claim claim = claimRepository.findById(claimId)
                .orElseThrow(() -> new IllegalArgumentException("Claim not found"));

        // Role-based access control
        boolean allowed = ("FINANCE_EXECUTIVE".equals(userRole) || "COFOUNDER".equals(userRole))
                && "SUBMITTED".equals(claim.getStatus());
        if (!allowed) {
            throw new SecurityException("User with role '" + userRole + "' is not allowed to reject claims");
        }

        claim.setStatus("REJECTED");
        claim.setDecidedBy(decidedBy);
        claim.setDecidedAt(Instant.now());
        claim.setDecisionNotes(notes);
        logEvent(claim, "SUBMITTED", "REJECTED", decidedBy, notes);
        return claimRepository.save(claim);
    }

    @Transactional
    public Claim markRecovered(Long claimId, String recoveredBy, String userRole) {
        Claim claim = claimRepository.findById(claimId)
                .orElseThrow(() -> new IllegalArgumentException("Claim not found"));

        // Role-based access control: only FINANCE_EXECUTIVE can recover
        if (!"FINANCE_EXECUTIVE".equals(userRole)) {
            throw new SecurityException("User with role '" + userRole + "' is not allowed to recover claims");
        }
        if (!"APPROVED".equals(claim.getStatus())) {
            throw new IllegalStateException("Only APPROVED claims can be marked recovered");
        }

        claim.setStatus("RECOVERED");
        claim.setRecoveredAt(Instant.now());
        logEvent(claim, "APPROVED", "RECOVERED", recoveredBy, "Payment recovered");
        return claimRepository.save(claim);
    }

    public List<Claim> getClaimsByPartner(String partnerCode, String partnerType) {
        return claimRepository.findByPartnerCodeAndPartnerType(partnerCode, partnerType);
    }

    public List<Claim> getClaimsByStatus(String status) {
        return claimRepository.findByStatus(status);
    }

    public Claim getClaim(Long id) {
        return claimRepository.findById(id).orElse(null);
    }

    public Page<Claim> searchClaims(String status, String partner,
                                 Pageable pageable) {
        Specification<Claim> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (status != null && !status.isEmpty()) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            if (partner != null && !partner.isEmpty()) {
                predicates.add(cb.equal(root.get("partnerCode"), partner));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return claimRepository.findAll(spec, pageable);
    }

    public List<ClaimItem> getClaimItems(Long claimId) {
        return claimItemRepository.findByClaimId(claimId);
    }

    public List<ClaimEvent> getClaimEvents(Long claimId) {
        return claimEventRepository.findByClaimIdOrderByCreatedAtAsc(claimId);
    }

    public String generateEvidenceCsv(Long claimId) {
        Claim claim = claimRepository.findById(claimId)
                .orElseThrow(() -> new IllegalArgumentException("Claim not found"));
        List<ClaimItem> items = claimItemRepository.findByClaimId(claimId);

        StringBuilder csv = new StringBuilder();
        csv.append("Claim Number,Partner Code,Partner Type,Order ID,Claim Type,Amount,Evidence Notes\n");
        for (ClaimItem item : items) {
            csv.append(String.format("%s,%s,%s,%s,%s,%.2f,%s\n",
                    claim.getClaimNumber(),
                    claim.getPartnerCode(),
                    claim.getPartnerType(),
                    item.getOrderId(),
                    item.getClaimType(),
                    item.getAmount(),
                    item.getEvidenceNotes() != null ? item.getEvidenceNotes().replace(",", ";") : ""));
        }
        return csv.toString();
    }

    private void logEvent(Claim claim, String fromStatus, String toStatus, String actor, String notes) {
        ClaimEvent event = new ClaimEvent();
        event.setClaim(claim);
        event.setFromStatus(fromStatus);
        event.setToStatus(toStatus);
        event.setActorEmail(actor);
        event.setNotes(notes);
        claimEventRepository.save(event);
    }

    public record ClaimItemRequest(
            String orderId,
            String claimType,
            BigDecimal amount,
            String evidenceNotes
    ) {}
}