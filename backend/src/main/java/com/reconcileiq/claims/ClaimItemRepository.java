package com.reconcileiq.claims;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ClaimItemRepository extends JpaRepository<ClaimItem, Long> {
    List<ClaimItem> findByClaimId(Long claimId);
    List<ClaimItem> findByOrderId(String orderId);
}