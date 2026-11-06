package com.reconcileiq.ingestion;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UploadBatchRepository extends JpaRepository<UploadBatch, Long> {
    Optional<UploadBatch> findByFileHash(String fileHash);

    @Query("SELECT ub FROM UploadBatch ub WHERE ub.source.id = :sourceId AND ub.fileType = :fileType")
    List<UploadBatch> findBySourceIdAndFileType(@Param("sourceId") Long sourceId, @Param("fileType") String fileType);
}