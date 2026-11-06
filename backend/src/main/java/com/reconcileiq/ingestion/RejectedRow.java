package com.reconcileiq.ingestion;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "rejected_rows")
public class RejectedRow {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "batch_id", nullable = false)
    private Long batchId;

    @Column(name = "row_number", nullable = false)
    private Integer rowNumber;

    @Column(name = "raw_data_json", columnDefinition = "TEXT", nullable = false)
    private String rawDataJson;

    @Column(name = "error_reason", columnDefinition = "TEXT", nullable = false)
    private String errorReason;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    public RejectedRow() {}

    public RejectedRow(Long batchId, Integer rowNumber, String rawDataJson, String errorReason) {
        this.batchId = batchId;
        this.rowNumber = rowNumber;
        this.rawDataJson = rawDataJson;
        this.errorReason = errorReason;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getBatchId() { return batchId; }
    public void setBatchId(Long batchId) { this.batchId = batchId; }
    public Integer getRowNumber() { return rowNumber; }
    public void setRowNumber(Integer rowNumber) { this.rowNumber = rowNumber; }
    public String getRawDataJson() { return rawDataJson; }
    public void setRawDataJson(String rawDataJson) { this.rawDataJson = rawDataJson; }
    public String getErrorReason() { return errorReason; }
    public void setErrorReason(String errorReason) { this.errorReason = errorReason; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}