package com.reconcileiq.ratecard;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class RateCardService {

    private final ChannelRateCardRepository channelRepo;
    private final CourierRateCardRepository courierRepo;
    private final CourierWeightSlabRepository slabRepo;

    public RateCardService(ChannelRateCardRepository channelRepo,
                           CourierRateCardRepository courierRepo,
                           CourierWeightSlabRepository slabRepo) {
        this.channelRepo = channelRepo;
        this.courierRepo = courierRepo;
        this.slabRepo = slabRepo;
    }

    // Channel rate card methods
    public ChannelRateCard saveChannelRateCard(ChannelRateCard card) {
        return channelRepo.save(card);
    }

    public Optional<ChannelRateCard> getChannelRateCard(String channelCode) {
        return channelRepo.findByChannelCode(channelCode);
    }

    public Optional<ChannelRateCard> getEffectiveChannelRateCard(String channelCode, LocalDate date) {
        return channelRepo.findEffectiveByChannelCodeAndDate(channelCode, date);
    }

    public List<ChannelRateCard> getAllChannelRateCards() {
        return channelRepo.findAll();
    }

    // Courier rate card methods
    public CourierRateCard saveCourierRateCard(CourierRateCard card) {
        return courierRepo.save(card);
    }

    public Optional<CourierRateCard> getCourierRateCard(String courierCode) {
        return courierRepo.findByCourierCode(courierCode);
    }

    public Optional<CourierRateCard> getEffectiveCourierRateCard(String courierCode, LocalDate date) {
        return courierRepo.findEffectiveByCourierCodeAndDate(courierCode, date);
    }

    public List<CourierRateCard> getAllCourierRateCards() {
        return courierRepo.findAll();
    }

    // Weight slab methods
    public CourierWeightSlab saveWeightSlab(CourierWeightSlab slab) {
        return slabRepo.save(slab);
    }

    public List<CourierWeightSlab> getWeightSlabs(CourierRateCard rateCard) {
        return slabRepo.findByRateCardOrderByMinWeightKgAsc(rateCard);
    }

    // Expected settlement computation (channel)
    public BigDecimal computeExpectedSettlement(String channelCode, BigDecimal orderValue,
                                                String paymentType, LocalDate orderDate) {
        Optional<ChannelRateCard> cardOpt = getEffectiveChannelRateCard(channelCode, orderDate);
        if (cardOpt.isEmpty()) {
            return orderValue; // No rate card, assume full value
        }
        ChannelRateCard card = cardOpt.get();

        // expected = order_value - (order_value * commission_pct / 100) - fixed_fee - max(order_value * collection_fee_pct / 100, min_collection_fee)
        BigDecimal commission = orderValue.multiply(card.getCommissionPct()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        BigDecimal collectionFee = orderValue.multiply(card.getCollectionFeePct()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        BigDecimal effectiveCollectionFee = collectionFee.max(card.getMinCollectionFee());

        return orderValue
                .subtract(commission)
                .subtract(card.getFixedFee())
                .subtract(effectiveCollectionFee);
    }

    // Expected freight computation (courier)
    public BigDecimal computeExpectedFreight(String courierCode, BigDecimal billedWeightKg,
                                             String paymentType, String deliveryStatus, LocalDate invoiceDate) {
        Optional<CourierRateCard> cardOpt = getEffectiveCourierRateCard(courierCode, invoiceDate);
        if (cardOpt.isEmpty()) {
            return BigDecimal.ZERO;
        }
        CourierRateCard card = cardOpt.get();
        List<CourierWeightSlab> slabs = getWeightSlabs(card);

        // Find matching weight slab
        BigDecimal weightSlabRate = BigDecimal.ZERO;
        for (CourierWeightSlab slab : slabs) {
            if (billedWeightKg.compareTo(slab.getMinWeightKg()) >= 0 &&
                billedWeightKg.compareTo(slab.getMaxWeightKg()) < 0) {
                weightSlabRate = slab.getRate();
                break;
            }
        }
        // If weight exceeds highest slab, use highest slab rate
        if (weightSlabRate.equals(BigDecimal.ZERO) && !slabs.isEmpty()) {
            weightSlabRate = slabs.get(slabs.size() - 1).getRate();
        }

        // expected_freight = weight_slab_rate + (payment_type == COD ? cod_fee : 0) + (delivery_status == RETURNED ? return_fee : 0)
        BigDecimal expected = weightSlabRate;
        if ("COD".equalsIgnoreCase(paymentType)) {
            BigDecimal codFee = weightSlabRate.multiply(card.getCodFeePct()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            expected = expected.add(codFee.max(card.getMinCodFee()));
        }
        if ("RETURNED_TO_WAREHOUSE".equalsIgnoreCase(deliveryStatus) || "RETURNED".equalsIgnoreCase(deliveryStatus)) {
            expected = expected.add(card.getReturnFee());
        }
        return expected;
    }
}