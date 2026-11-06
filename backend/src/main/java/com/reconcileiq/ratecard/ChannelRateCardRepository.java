package com.reconcileiq.ratecard;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface ChannelRateCardRepository extends JpaRepository<ChannelRateCard, Long> {
    Optional<ChannelRateCard> findByChannelCode(String channelCode);

    @Query("SELECT c FROM ChannelRateCard c WHERE c.channelCode = :channelCode AND c.effectiveFrom <= :date ORDER BY c.effectiveFrom DESC")
    Optional<ChannelRateCard> findEffectiveByChannelCodeAndDate(@Param("channelCode") String channelCode, @Param("date") LocalDate date);
}