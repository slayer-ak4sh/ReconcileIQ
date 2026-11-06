package com.reconcileiq.claims;

import com.reconcileiq.matching.OrderReconciliation;
import com.reconcileiq.matching.OrderReconciliationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

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

        BigDecimal total = BigDecimal.ZERO;
        for (ClaimItemRequest itemReq : items) {
            OrderReconciliation recon = reconRepository.findByOrderId(itemReq.getOrderId())
                    .orElseThrow(() -> new IllegalArgumentException("Order reconciliation not found: " + itemReq.getOrderId()));

            ClaimItem item = new ClaimItem();
            item.setClaim(claim);
            item.setOrderId(itemReq.getOrderId());
            item.setOrderReconciliation(recon);
            item.setClaimType(itemReq.getClaimType());
            item.setAmount(itemReq.getAmount());
            item.setEvidenceNotes(itemReq.getEvidenceNotes());
            claimItemRepository.save(item);
            total = total.add(itemReq.getAmount());
        }
        claim.setTotalAmount(total);
        return claimRepository.save(claim);
    }

    @Transactional
    public Claim submitClaim(Long claimId, String submittedBy) {
        Claim claim = claimRepository.findById(claimId)
                .orElseThrow(() -> new IllegalArgumentException("Claim not found"));
        if (!"DRAFT".equals(claim.getStatus())) {
            throw new IllegalStateException("Only DRAFT claims can be submitted");
        }
        claim.setStatus("SUBMITTED");
        claim.setSubmittedAt(Instant.now());
        logEvent(claim, "DRAFT", "SUBMITTED", submittedBy, "Claim submitted for review");
        return claimRepository.save(claim);
    }

    @Transactional
    public Claim approveClaim(Long claimId, String decidedBy, String notes) {
        Claim claim = claimRepository.findById(claimId)
                .orElseThrow(() -> new IllegalArgumentException("Claim not found"));
        if (!"SUBMITTED".equals(claim.getStatus())) {
            throw new IllegalStateException("Only SUBMITTED claims can be approved");
        }
        claim.setStatus("APPROVED");
        claim.setDecidedBy(decidedBy);
        claim.setDecidedAt(Instant.now());
        claim.setDecisionNotes(notes);
        logEvent(claim, "SUBMITTED", "APPROVED", decidedBy, notes);
        return claimRepository.save(claim);
    }

    @Transactional
    public Claim rejectClaim(Long claimId, String decidedBy, String notes) {
        Claim claim = claimRepository.findById(claimId)
                .orElseThrow(() -> new IllegalArgumentException("Claim not found"));
        if (!"SUBMITTED".equals(claim.getStatus())) {
            throw new IllegalStateException("Only SUBMITTED claims can be rejected");
        }
        claim.setStatus("REJECTED");
        claim.setDecidedBy(decidedBy);
        claim.setDecidedAt(Instant.now());
        claim.setDecisionNotes(notes);
        logEvent(claim, "SUBMITTED", "REJECTED", decidedBy, notes);
        return claimRepository.save(claim);
    }

    @Transactional
    public Claim markRecovered(Long claimId, String recoveredBy) {
        Claim claim = claimRepository.findById(claimId)
                .orElseThrow(() -> new IllegalArgumentException("Claim not found"));
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

    public static class ClaimItemRequest {
        private String orderId;
        private String claimType;
        private BigDecimal amount;
        private String evidenceNotes;

        public String getOrderId() { return orderId; }
        public void setOrderId(String orderId) { this.orderId = orderId; }
        public String getClaimType() { return claimType; }
        public void setClaimType(String claimType) { this.claimType = claimType; }
        public BigDecimal getAmount() { return amount; }
        public void setAmount(BigDecimal amount) { this.amount = amount; }
        public String getEvidenceNotes() { return evidenceNotes; }
        public void setEvidenceNotes(String evidenceNotes) { this.evidenceNotes = evidenceNotes; }
    }
}