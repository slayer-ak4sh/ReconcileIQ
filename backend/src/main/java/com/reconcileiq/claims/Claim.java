package com.reconcileiq.claims;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "claims")
public class Claim {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "claim_number", nullable = false, unique = true, length = 100)
    private String claimNumber;

    @Column(name = "partner_code", nullable = false, length = 50)
    private String partnerCode;

    @Column(name = "partner_type", nullable = false, length = 20)
    private String partnerType; // CHANNEL or COURIER

    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(name = "status", nullable = false, length = 50)
    private String status = "DRAFT"; // DRAFT, SUBMITTED, APPROVED, REJECTED, RECOVERED

    @Column(name = "created_by", nullable = false, length = 255)
    private String createdBy;

    @Column(name = "submitted_at")
    private Instant submittedAt;

    @Column(name = "decided_by", length = 255)
    private String decidedBy;

    @Column(name = "decided_at")
    private Instant decidedAt;

    @Column(name = "decision_notes", columnDefinition = "TEXT")
    private String decisionNotes;

    @Column(name = "recovered_at")
    private Instant recoveredAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    public Claim() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getClaimNumber() { return claimNumber; }
    public void setClaimNumber(String claimNumber) { this.claimNumber = claimNumber; }
    public String getPartnerCode() { return partnerCode; }
    public void setPartnerCode(String partnerCode) { this.partnerCode = partnerCode; }
    public String getPartnerType() { return partnerType; }
    public void setPartnerType(String partnerType) { this.partnerType = partnerType; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    public Instant getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(Instant submittedAt) { this.submittedAt = submittedAt; }
    public String getDecidedBy() { return decidedBy; }
    public void setDecidedBy(String decidedBy) { this.decidedBy = decidedBy; }
    public Instant getDecidedAt() { return decidedAt; }
    public void setDecidedAt(Instant decidedAt) { this.decidedAt = decidedAt; }
    public String getDecisionNotes() { return decisionNotes; }
    public void setDecisionNotes(String decisionNotes) { this.decisionNotes = decisionNotes; }
    public Instant getRecoveredAt() { return recoveredAt; }
    public void setRecoveredAt(Instant recoveredAt) { this.recoveredAt = recoveredAt; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}