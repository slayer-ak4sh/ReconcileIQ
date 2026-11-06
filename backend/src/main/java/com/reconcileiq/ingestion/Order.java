package com.reconcileiq.ingestion;

import jakarta.persistence.*;

@Entity
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String orderId;

    @Column(nullable = false, length = 50)
    private String channelCode;

    @Column(nullable = false)
    private java.time.LocalDate orderDate;

    @Column(nullable = false, precision = 12, scale = 2)
    private java.math.BigDecimal orderValue;

    @Column(nullable = false, length = 20)
    private String paymentType; // COD, PREPAID

    @Column(nullable = false, length = 50)
    private String deliveryStatus; // DELIVERED, RETURNED_TO_WAREHOUSE, IN_TRANSIT, CANCELLED

    private String customerName;
    private String customerPhone;
    private String customerAddress;

    @Column(name = "expected_amount_override", precision = 12, scale = 2)
    private java.math.BigDecimal expectedAmountOverride;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_id")
    private UploadBatch batch;

    @Column(nullable = false)
    private java.time.Instant createdAt;

    public Order() {
        this.createdAt = java.time.Instant.now();
    }

    public Order(String orderId, String channelCode, java.time.LocalDate orderDate,
                 java.math.BigDecimal orderValue, String paymentType, String deliveryStatus,
                 String customerName, String customerPhone, String customerAddress) {
        this.orderId = orderId;
        this.channelCode = channelCode;
        this.orderDate = orderDate;
        this.orderValue = orderValue;
        this.paymentType = paymentType;
        this.deliveryStatus = deliveryStatus;
        this.customerName = customerName;
        this.customerPhone = customerPhone;
        this.customerAddress = customerAddress;
        this.createdAt = java.time.Instant.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
    public String getChannelCode() { return channelCode; }
    public void setChannelCode(String channelCode) { this.channelCode = channelCode; }
    public java.time.LocalDate getOrderDate() { return orderDate; }
    public void setOrderDate(java.time.LocalDate orderDate) { this.orderDate = orderDate; }
    public java.math.BigDecimal getOrderValue() { return orderValue; }
    public void setOrderValue(java.math.BigDecimal orderValue) { this.orderValue = orderValue; }
    public String getPaymentType() { return paymentType; }
    public void setPaymentType(String paymentType) { this.paymentType = paymentType; }
    public String getDeliveryStatus() { return deliveryStatus; }
    public void setDeliveryStatus(String deliveryStatus) { this.deliveryStatus = deliveryStatus; }
    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    public String getCustomerPhone() { return customerPhone; }
    public void setCustomerPhone(String customerPhone) { this.customerPhone = customerPhone; }
    public String getCustomerAddress() { return customerAddress; }
    public void setCustomerAddress(String customerAddress) { this.customerAddress = customerAddress; }
    public java.math.BigDecimal getExpectedAmountOverride() { return expectedAmountOverride; }
    public void setExpectedAmountOverride(java.math.BigDecimal expectedAmountOverride) { this.expectedAmountOverride = expectedAmountOverride; }
    public UploadBatch getBatch() { return batch; }
    public void setBatch(UploadBatch batch) { this.batch = batch; }
    public java.time.Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(java.time.Instant createdAt) { this.createdAt = createdAt; }
}