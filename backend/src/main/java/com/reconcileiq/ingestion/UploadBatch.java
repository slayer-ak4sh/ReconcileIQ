package com.reconcileiq.ingestion;

import jakarta.persistence.*;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "upload_batches")
public class UploadBatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_id", nullable = false)
    private Source source;

    @Column(nullable = false)
    private String fileType;

    @Column(nullable = false)
    private String fileName;

    @Column(nullable = false, length = 64)
    private String fileHash;

    @Column(nullable = false)
    private Integer rowCount = 0;

    @Column(nullable = false)
    private Integer validRowCount = 0;

    @Column(nullable = false)
    private Integer errorCount = 0;

    @Column(nullable = false)
    private String status; // PROCESSING, COMPLETED, FAILED

    @Column(nullable = false)
    private String uploadedBy;

    @Column(nullable = false)
    private java.time.Instant createdAt;

    @Column(columnDefinition = "TEXT")
    private String headersJson;

    @Transient
    private List<Map<String, String>> previewRows;

    public UploadBatch() {}

    public UploadBatch(Source source, String fileType, String fileName, String fileHash, String uploadedBy) {
        this.source = source;
        this.fileType = fileType;
        this.fileName = fileName;
        this.fileHash = fileHash;
        this.uploadedBy = uploadedBy;
        this.status = "PROCESSING";
        this.createdAt = java.time.Instant.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Source getSource() { return source; }
    public void setSource(Source source) { this.source = source; }

    public Long getSourceId() {
        return source != null ? source.getId() : null;
    }

    public String getFileType() { return fileType; }
    public void setFileType(String fileType) { this.fileType = fileType; }
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    public String getFileHash() { return fileHash; }
    public void setFileHash(String fileHash) { this.fileHash = fileHash; }
    public Integer getRowCount() { return rowCount; }
    public void setRowCount(Integer rowCount) { this.rowCount = rowCount; }
    public Integer getValidRowCount() { return validRowCount; }
    public void setValidRowCount(Integer validRowCount) { this.validRowCount = validRowCount; }
    public Integer getErrorCount() { return errorCount; }
    public void setErrorCount(Integer errorCount) { this.errorCount = errorCount; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getUploadedBy() { return uploadedBy; }
    public void setUploadedBy(String uploadedBy) { this.uploadedBy = uploadedBy; }
    public java.time.Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(java.time.Instant createdAt) { this.createdAt = createdAt; }

    public List<String> getHeaders() {
        if (headersJson == null || headersJson.isBlank()) return List.of();
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper()
                    .readValue(headersJson, new com.fasterxml.jackson.core.type.TypeReference<List<String>>() {});
        } catch (Exception e) { return List.of(); }
    }
    public void setHeaders(List<String> headers) {
        try {
            this.headersJson = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(headers);
        } catch (Exception e) { this.headersJson = "[]"; }
    }
    public List<Map<String, String>> getPreviewRows() { return previewRows; }
    public void setPreviewRows(List<Map<String, String>> previewRows) { this.previewRows = previewRows; }
}