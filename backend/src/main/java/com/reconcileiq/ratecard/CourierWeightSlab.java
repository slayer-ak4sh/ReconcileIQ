package com.reconcileiq.ratecard;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "courier_weight_slabs")
public class CourierWeightSlab {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rate_card_id", nullable = false)
    private CourierRateCard rateCard;

    @Column(name = "min_weight_kg", nullable = false, precision = 8, scale = 3)
    private BigDecimal minWeightKg;

    @Column(name = "max_weight_kg", nullable = false, precision = 8, scale = 3)
    private BigDecimal maxWeightKg;

    @Column(name = "rate", nullable = false, precision = 12, scale = 2)
    private BigDecimal rate;

    public CourierWeightSlab() {}

    public CourierWeightSlab(CourierRateCard rateCard, BigDecimal minWeightKg, BigDecimal maxWeightKg, BigDecimal rate) {
        this.rateCard = rateCard;
        this.minWeightKg = minWeightKg;
        this.maxWeightKg = maxWeightKg;
        this.rate = rate;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public CourierRateCard getRateCard() { return rateCard; }
    public void setRateCard(CourierRateCard rateCard) { this.rateCard = rateCard; }
    public BigDecimal getMinWeightKg() { return minWeightKg; }
    public void setMinWeightKg(BigDecimal minWeightKg) { this.minWeightKg = minWeightKg; }
    public BigDecimal getMaxWeightKg() { return maxWeightKg; }
    public void setMaxWeightKg(BigDecimal maxWeightKg) { this.maxWeightKg = maxWeightKg; }
    public BigDecimal getRate() { return rate; }
    public void setRate(BigDecimal rate) { this.rate = rate; }
}