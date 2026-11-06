package com.reconcileiq.ingestion;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CodRemittanceRepository extends JpaRepository<CodRemittance, Long> {
    List<CodRemittance> findByOrderId(String orderId);
}