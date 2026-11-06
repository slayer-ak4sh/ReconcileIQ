package com.reconcileiq.mapping;

import jakarta.persistence.*;

@Entity
@Table(name = "column_mappings")
public class ColumnMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long sourceId;

    @Column(nullable = false)
    private String fileType; // ORDERS, SETTLEMENT, COD_REMITTANCE, FREIGHT_INVOICE, WAREHOUSE_SCAN

    @Column(nullable = false)
    private Integer version;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String mappingsJson;

    @Column(nullable = false)
    private Boolean isActive = true;

    @Column(nullable = false)
    private String createdBy;

    @Column(nullable = false)
    private java.time.Instant approvedAt;

    public ColumnMapping() {}

    public ColumnMapping(Long sourceId, String fileType, Integer version, String mappingsJson, String createdBy) {
        this.sourceId = sourceId;
        this.fileType = fileType;
        this.version = version;
        this.mappingsJson = mappingsJson;
        this.createdBy = createdBy;
        this.approvedAt = java.time.Instant.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getSourceId() { return sourceId; }
    public void setSourceId(Long sourceId) { this.sourceId = sourceId; }
    public String getFileType() { return fileType; }
    public void setFileType(String fileType) { this.fileType = fileType; }
    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
    public String getMappingsJson() { return mappingsJson; }
    public void setMappingsJson(String mappingsJson) { this.mappingsJson = mappingsJson; }
    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    public java.time.Instant getApprovedAt() { return approvedAt; }
    public void setApprovedAt(java.time.Instant approvedAt) { this.approvedAt = approvedAt; }
}