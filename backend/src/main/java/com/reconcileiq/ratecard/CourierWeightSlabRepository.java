package com.reconcileiq.ratecard;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CourierWeightSlabRepository extends JpaRepository<CourierWeightSlab, Long> {
    List<CourierWeightSlab> findByRateCardOrderByMinWeightKgAsc(CourierRateCard rateCard);
}