package com.reconcileiq.claims;

import com.reconcileiq.matching.OrderReconciliation;
import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "claim_items")
public class ClaimItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "claim_id", nullable = false)
    private Claim claim;

    @Column(name = "order_id", nullable = false, length = 100)
    private String orderId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_reconciliation_id", nullable = true)
    private OrderReconciliation orderReconciliation;

    @Column(name = "claim_type", nullable = false, length = 50)
    private String claimType; // SHORT_PAYMENT, UNREMITTED_COD, FREIGHT_OVERCHARGE, LOST_RETURN

    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount = BigDecimal.ZERO;

    @Column(name = "evidence_notes", columnDefinition = "TEXT")
    private String evidenceNotes;

    public ClaimItem() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Claim getClaim() { return claim; }
    public void setClaim(Claim claim) { this.claim = claim; }
    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
    public OrderReconciliation getOrderReconciliation() { return orderReconciliation; }
    public void setOrderReconciliation(OrderReconciliation orderReconciliation) { this.orderReconciliation = orderReconciliation; }
    public String getClaimType() { return claimType; }
    public void setClaimType(String claimType) { this.claimType = claimType; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getEvidenceNotes() { return evidenceNotes; }
    public void setEvidenceNotes(String evidenceNotes) { this.evidenceNotes = evidenceNotes; }
}