package com.reconcileiq.ratecard;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface CourierRateCardRepository extends JpaRepository<CourierRateCard, Long> {
    Optional<CourierRateCard> findByCourierCode(String courierCode);

    @Query("SELECT c FROM CourierRateCard c WHERE c.courierCode = :courierCode AND c.effectiveFrom <= :date ORDER BY c.effectiveFrom DESC")
    Optional<CourierRateCard> findEffectiveByCourierCodeAndDate(@Param("courierCode") String courierCode, @Param("date") LocalDate date);
}