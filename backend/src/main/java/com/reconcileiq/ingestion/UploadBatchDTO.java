package com.reconcileiq.ingestion;

public record UploadBatchDTO(Long id, Long sourceId, String fileType, String fileName,
                             Integer rowCount, Integer validRowCount, Integer errorCount,
                             String status, String uploadedBy, java.time.Instant createdAt) {
    public static UploadBatchDTO fromEntity(UploadBatch b) {
        return new UploadBatchDTO(b.getId(), b.getSourceId(), b.getFileType(), b.getFileName(),
                b.getRowCount(), b.getValidRowCount(), b.getErrorCount(), b.getStatus(),
                b.getUploadedBy(), b.getCreatedAt());
    }
}
