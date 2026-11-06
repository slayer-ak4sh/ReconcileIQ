package com.reconcileiq.claims;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClaimRepository extends JpaRepository<Claim, Long> {
    Optional<Claim> findByClaimNumber(String claimNumber);
    List<Claim> findByPartnerCodeAndPartnerType(String partnerCode, String partnerType);
    List<Claim> findByStatus(String status);
}