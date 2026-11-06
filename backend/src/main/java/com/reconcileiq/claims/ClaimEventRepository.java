package com.reconcileiq.claims;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ClaimEventRepository extends JpaRepository<ClaimEvent, Long> {
    List<ClaimEvent> findByClaimIdOrderByCreatedAtAsc(Long claimId);
}