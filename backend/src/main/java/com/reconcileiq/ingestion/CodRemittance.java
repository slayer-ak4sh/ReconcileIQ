package com.reconcileiq.ingestion;

import jakarta.persistence.*;

@Entity
@Table(name = "cod_remittances")
public class CodRemittance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_id", nullable = false)
    private UploadBatch batch;

    @Column(nullable = false, length = 100)
    private String orderId;

    @Column(nullable = false, length = 50)
    private String courierCode;

    private String utrNo;

    @Column(nullable = false)
    private java.time.LocalDate remittanceDate;

    @Column(nullable = false, precision = 12, scale = 2)
    private java.math.BigDecimal codCollected;

    @Column(nullable = false, precision = 12, scale = 2)
    private java.math.BigDecimal feeDeducted;

    @Column(nullable = false, precision = 12, scale = 2)
    private java.math.BigDecimal netRemitted;

    @Column(nullable = false)
    private java.time.Instant createdAt;

    public CodRemittance() {
        this.createdAt = java.time.Instant.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public UploadBatch getBatch() { return batch; }
    public void setBatch(UploadBatch batch) { this.batch = batch; }
    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
    public String getCourierCode() { return courierCode; }
    public void setCourierCode(String courierCode) { this.courierCode = courierCode; }
    public String getUtrNo() { return utrNo; }
    public void setUtrNo(String utrNo) { this.utrNo = utrNo; }
    public java.time.LocalDate getRemittanceDate() { return remittanceDate; }
    public void setRemittanceDate(java.time.LocalDate remittanceDate) { this.remittanceDate = remittanceDate; }
    public java.math.BigDecimal getCodCollected() { return codCollected; }
    public void setCodCollected(java.math.BigDecimal codCollected) { this.codCollected = codCollected; }
    public java.math.BigDecimal getFeeDeducted() { return feeDeducted; }
    public void setFeeDeducted(java.math.BigDecimal feeDeducted) { this.feeDeducted = feeDeducted; }
    public java.math.BigDecimal getNetRemitted() { return netRemitted; }
    public void setNetRemitted(java.math.BigDecimal netRemitted) { this.netRemitted = netRemitted; }
    public java.time.Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(java.time.Instant createdAt) { this.createdAt = createdAt; }
}