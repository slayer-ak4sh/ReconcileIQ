package com.reconcileiq.ingestion;

import jakarta.persistence.*;

@Entity
@Table(name = "freight_invoices")
public class FreightInvoice {

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

    private String invoiceNo;

    @Column(nullable = false)
    private java.time.LocalDate invoiceDate;

    @Column(nullable = false, precision = 8, scale = 3)
    private java.math.BigDecimal billedWeight;

    @Column(nullable = false, precision = 12, scale = 2)
    private java.math.BigDecimal freightCharge;

    @Column(nullable = false, precision = 12, scale = 2)
    private java.math.BigDecimal codFee;

    @Column(nullable = false, precision = 12, scale = 2)
    private java.math.BigDecimal returnFee;

    @Column(nullable = false, precision = 12, scale = 2)
    private java.math.BigDecimal totalFreight;

    @Column(nullable = false)
    private java.time.Instant createdAt;

    public FreightInvoice() {
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
    public String getInvoiceNo() { return invoiceNo; }
    public void setInvoiceNo(String invoiceNo) { this.invoiceNo = invoiceNo; }
    public java.time.LocalDate getInvoiceDate() { return invoiceDate; }
    public void setInvoiceDate(java.time.LocalDate invoiceDate) { this.invoiceDate = invoiceDate; }
    public java.math.BigDecimal getBilledWeight() { return billedWeight; }
    public void setBilledWeight(java.math.BigDecimal billedWeight) { this.billedWeight = billedWeight; }
    public java.math.BigDecimal getFreightCharge() { return freightCharge; }
    public void setFreightCharge(java.math.BigDecimal freightCharge) { this.freightCharge = freightCharge; }
    public java.math.BigDecimal getCodFee() { return codFee; }
    public void setCodFee(java.math.BigDecimal codFee) { this.codFee = codFee; }
    public java.math.BigDecimal getReturnFee() { return returnFee; }
    public void setReturnFee(java.math.BigDecimal returnFee) { this.returnFee = returnFee; }
    public java.math.BigDecimal getTotalFreight() { return totalFreight; }
    public void setTotalFreight(java.math.BigDecimal totalFreight) { this.totalFreight = totalFreight; }
    public java.time.Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(java.time.Instant createdAt) { this.createdAt = createdAt; }
}