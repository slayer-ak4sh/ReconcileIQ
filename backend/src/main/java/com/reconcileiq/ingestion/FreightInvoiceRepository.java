package com.reconcileiq.ingestion;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface FreightInvoiceRepository extends JpaRepository<FreightInvoice, Long> {
    List<FreightInvoice> findByOrderId(String orderId);
}