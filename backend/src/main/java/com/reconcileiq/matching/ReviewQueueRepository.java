package com.reconcileiq.matching;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReviewQueueRepository extends JpaRepository<ReviewQueue, Long> {
    List<ReviewQueue> findByBatchIdAndFileType(Long batchId, String fileType);
    List<ReviewQueue> findByStatus(String status);
}