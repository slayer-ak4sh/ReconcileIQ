package com.reconcileiq.mapping;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ColumnMappingRepository extends JpaRepository<ColumnMapping, Long> {
    Optional<ColumnMapping> findBySourceIdAndFileTypeAndIsActiveTrue(Long sourceId, String fileType);
    List<ColumnMapping> findBySourceIdAndFileTypeOrderByVersionDesc(Long sourceId, String fileType);
    @Query("SELECT cm FROM ColumnMapping cm WHERE cm.sourceId = :sourceId AND cm.fileType = :fileType AND cm.version = :version")
    Optional<ColumnMapping> findBySourceIdAndFileTypeAndVersion(Long sourceId, String fileType, Integer version);
}