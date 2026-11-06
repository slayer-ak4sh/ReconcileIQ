package com.reconcileiq.matching;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "review_queue")
public class ReviewQueue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "batch_id", nullable = false)
    private Long batchId;

    @Column(name = "file_type", nullable = false, length = 50)
    private String fileType;

    @Column(name = "source_id", nullable = false)
    private Long sourceId;

    @Column(name = "raw_data_json", columnDefinition = "TEXT", nullable = false)
    private String rawDataJson;

    @Column(name = "suggested_order_id", length = 100)
    private String suggestedOrderId;

    @Column(name = "confidence", precision = 5, scale = 2)
    private BigDecimal confidence;

    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

    @Column(name = "status", nullable = false, length = 50)
    private String status = "PENDING"; // PENDING, RESOLVED, IGNORED

    @Column(name = "reviewed_by", length = 255)
    private String reviewedBy;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    public ReviewQueue() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getBatchId() { return batchId; }
    public void setBatchId(Long batchId) { this.batchId = batchId; }
    public String getFileType() { return fileType; }
    public void setFileType(String fileType) { this.fileType = fileType; }
    public Long getSourceId() { return sourceId; }
    public void setSourceId(Long sourceId) { this.sourceId = sourceId; }
    public String getRawDataJson() { return rawDataJson; }
    public void setRawDataJson(String rawDataJson) { this.rawDataJson = rawDataJson; }
    public String getSuggestedOrderId() { return suggestedOrderId; }
    public void setSuggestedOrderId(String suggestedOrderId) { this.suggestedOrderId = suggestedOrderId; }
    public BigDecimal getConfidence() { return confidence; }
    public void setConfidence(BigDecimal confidence) { this.confidence = confidence; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getReviewedBy() { return reviewedBy; }
    public void setReviewedBy(String reviewedBy) { this.reviewedBy = reviewedBy; }
    public Instant getReviewedAt() { return reviewedAt; }
    public void setReviewedAt(Instant reviewedAt) { this.reviewedAt = reviewedAt; }
}