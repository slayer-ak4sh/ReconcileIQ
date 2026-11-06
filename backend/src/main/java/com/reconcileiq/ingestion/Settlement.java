package com.reconcileiq.ingestion;

import jakarta.persistence.*;

@Entity
@Table(name = "settlements")
public class Settlement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_id", nullable = false)
    private UploadBatch batch;

    @Column(nullable = false, length = 100)
    private String orderId;

    @Column(nullable = false, length = 50)
    private String channelCode;

    private String referenceNo;

    @Column(nullable = false)
    private java.time.LocalDate settlementDate;

    @Column(nullable = false, precision = 12, scale = 2)
    private java.math.BigDecimal grossAmount;

    @Column(nullable = false, precision = 12, scale = 2)
    private java.math.BigDecimal commission;

    @Column(nullable = false, precision = 12, scale = 2)
    private java.math.BigDecimal fixedFee;

    @Column(nullable = false, precision = 12, scale = 2)
    private java.math.BigDecimal collectionFee;

    @Column(nullable = false, precision = 12, scale = 2)
    private java.math.BigDecimal tcsAmount;

    @Column(nullable = false, precision = 12, scale = 2)
    private java.math.BigDecimal tdsAmount;

    @Column(nullable = false, precision = 12, scale = 2)
    private java.math.BigDecimal netAmount;

    @Column(nullable = false)
    private java.time.Instant createdAt;

    public Settlement() {
        this.createdAt = java.time.Instant.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public UploadBatch getBatch() { return batch; }
    public void setBatch(UploadBatch batch) { this.batch = batch; }
    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
    public String getChannelCode() { return channelCode; }
    public void setChannelCode(String channelCode) { this.channelCode = channelCode; }
    public String getReferenceNo() { return referenceNo; }
    public void setReferenceNo(String referenceNo) { this.referenceNo = referenceNo; }
    public java.time.LocalDate getSettlementDate() { return settlementDate; }
    public void setSettlementDate(java.time.LocalDate settlementDate) { this.settlementDate = settlementDate; }
    public java.math.BigDecimal getGrossAmount() { return grossAmount; }
    public void setGrossAmount(java.math.BigDecimal grossAmount) { this.grossAmount = grossAmount; }
    public java.math.BigDecimal getCommission() { return commission; }
    public void setCommission(java.math.BigDecimal commission) { this.commission = commission; }
    public java.math.BigDecimal getFixedFee() { return fixedFee; }
    public void setFixedFee(java.math.BigDecimal fixedFee) { this.fixedFee = fixedFee; }
    public java.math.BigDecimal getCollectionFee() { return collectionFee; }
    public void setCollectionFee(java.math.BigDecimal collectionFee) { this.collectionFee = collectionFee; }
    public java.math.BigDecimal getTcsAmount() { return tcsAmount; }
    public void setTcsAmount(java.math.BigDecimal tcsAmount) { this.tcsAmount = tcsAmount; }
    public java.math.BigDecimal getTdsAmount() { return tdsAmount; }
    public void setTdsAmount(java.math.BigDecimal tdsAmount) { this.tdsAmount = tdsAmount; }
    public java.math.BigDecimal getNetAmount() { return netAmount; }
    public void setNetAmount(java.math.BigDecimal netAmount) { this.netAmount = netAmount; }
    public java.time.Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(java.time.Instant createdAt) { this.createdAt = createdAt; }
}