package com.reconcileiq.ingestion;

import jakarta.persistence.*;

@Entity
@Table(name = "sources")
public class Source {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String code;

    @Column(nullable = false)
    private String partnerType; // CHANNEL or COURIER

    private String channelCode;
    private String courierCode;

    @Column(nullable = false)
    private java.time.Instant createdAt;

    @OneToMany(mappedBy = "source")
    private java.util.List<UploadBatch> uploadBatches = new java.util.ArrayList<>();

    public java.util.List<UploadBatch> getUploadBatches() { return uploadBatches; }
    public void setUploadBatches(java.util.List<UploadBatch> uploadBatches) { this.uploadBatches = uploadBatches; }

    public Source() {}

    public Source(String name, String code, String partnerType, String channelCode, String courierCode) {
        this.name = name;
        this.code = code;
        this.partnerType = partnerType;
        this.channelCode = channelCode;
        this.courierCode = courierCode;
        this.createdAt = java.time.Instant.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getPartnerType() { return partnerType; }
    public void setPartnerType(String partnerType) { this.partnerType = partnerType; }
    public String getChannelCode() { return channelCode; }
    public void setChannelCode(String channelCode) { this.channelCode = channelCode; }
    public String getCourierCode() { return courierCode; }
    public void setCourierCode(String courierCode) { this.courierCode = courierCode; }
    public java.time.Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(java.time.Instant createdAt) { this.createdAt = createdAt; }
}