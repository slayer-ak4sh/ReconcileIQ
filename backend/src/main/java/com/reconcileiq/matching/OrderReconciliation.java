package com.reconcileiq.matching;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Instant;

@Entity
@Table(name = "order_reconciliation")
public class OrderReconciliation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false, unique = true, length = 100)
    private String orderId;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(name = "expected_settlement", nullable = false, precision = 12, scale = 2)
    private BigDecimal expectedSettlement = BigDecimal.ZERO;

    @Column(name = "expected_freight", nullable = false, precision = 12, scale = 2)
    private BigDecimal expectedFreight = BigDecimal.ZERO;

    @Column(name = "received_settlement", nullable = false, precision = 12, scale = 2)
    private BigDecimal receivedSettlement = BigDecimal.ZERO;

    @Column(name = "received_freight", nullable = false, precision = 12, scale = 2)
    private BigDecimal receivedFreight = BigDecimal.ZERO;

    @Column(name = "settlement_diff", nullable = false, precision = 12, scale = 2)
    private BigDecimal settlementDiff = BigDecimal.ZERO;

    @Column(name = "freight_diff", nullable = false, precision = 12, scale = 2)
    private BigDecimal freightDiff = BigDecimal.ZERO;

    @Column(name = "total_tcs", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalTcs = BigDecimal.ZERO;

    @Column(name = "total_tds", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalTds = BigDecimal.ZERO;

    @Column(name = "reconciliation_status", nullable = false, length = 50)
    private String reconciliationStatus;

    @Column(name = "is_matched", nullable = false)
    private Boolean isMatched = false;

    @Column(name = "matched_at", nullable = false)
    private Instant matchedAt = Instant.now();

    public OrderReconciliation() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }
    public BigDecimal getExpectedSettlement() { return expectedSettlement; }
    public void setExpectedSettlement(BigDecimal expectedSettlement) { this.expectedSettlement = expectedSettlement; }
    public BigDecimal getExpectedFreight() { return expectedFreight; }
    public void setExpectedFreight(BigDecimal expectedFreight) { this.expectedFreight = expectedFreight; }
    public BigDecimal getReceivedSettlement() { return receivedSettlement; }
    public void setReceivedSettlement(BigDecimal receivedSettlement) { this.receivedSettlement = receivedSettlement; }
    public BigDecimal getReceivedFreight() { return receivedFreight; }
    public void setReceivedFreight(BigDecimal receivedFreight) { this.receivedFreight = receivedFreight; }
    public BigDecimal getSettlementDiff() { return settlementDiff; }
    public void setSettlementDiff(BigDecimal settlementDiff) { this.settlementDiff = settlementDiff; }
    public BigDecimal getFreightDiff() { return freightDiff; }
    public void setFreightDiff(BigDecimal freightDiff) { this.freightDiff = freightDiff; }
    public BigDecimal getTotalTcs() { return totalTcs; }
    public void setTotalTcs(BigDecimal totalTcs) { this.totalTcs = totalTcs; }
    public BigDecimal getTotalTds() { return totalTds; }
    public void setTotalTds(BigDecimal totalTds) { this.totalTds = totalTds; }
    public String getReconciliationStatus() { return reconciliationStatus; }
    public void setReconciliationStatus(String reconciliationStatus) { this.reconciliationStatus = reconciliationStatus; }
    public Boolean getIsMatched() { return isMatched; }
    public void setIsMatched(Boolean isMatched) { this.isMatched = isMatched; }
    public Instant getMatchedAt() { return matchedAt; }
    public void setMatchedAt(Instant matchedAt) { this.matchedAt = matchedAt; }
}