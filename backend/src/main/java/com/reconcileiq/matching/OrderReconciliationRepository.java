package com.reconcileiq.matching;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderReconciliationRepository extends JpaRepository<OrderReconciliation, Long>,
        org.springframework.data.jpa.repository.JpaSpecificationExecutor<OrderReconciliation> {
    Optional<OrderReconciliation> findByOrderId(String orderId);

    List<OrderReconciliation> findByReconciliationStatus(String status);

    @Query("SELECT o FROM OrderReconciliation o WHERE o.dueDate <= :date AND o.receivedSettlement = 0 AND o.reconciliationStatus = 'UNPAID_PAST_DUE'")
    List<OrderReconciliation> findUnpaidPastDue(@Param("date") java.time.LocalDate date);

    @Query("SELECT o FROM OrderReconciliation o WHERE o.reconciliationStatus IN ('SHORT_PAID', 'UNPAID_PAST_DUE')")
    List<OrderReconciliation> findMoneyAtRiskOrders();
}