package com.reconcileiq.ratecard;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "channel_rate_cards")
public class ChannelRateCard {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "channel_code", nullable = false, unique = true, length = 50)
    private String channelCode;

    @Column(name = "commission_pct", nullable = false, precision = 5, scale = 2)
    private BigDecimal commissionPct = BigDecimal.ZERO;

    @Column(name = "fixed_fee", nullable = false, precision = 12, scale = 2)
    private BigDecimal fixedFee = BigDecimal.ZERO;

    @Column(name = "collection_fee_pct", nullable = false, precision = 5, scale = 2)
    private BigDecimal collectionFeePct = BigDecimal.ZERO;

    @Column(name = "min_collection_fee", nullable = false, precision = 12, scale = 2)
    private BigDecimal minCollectionFee = BigDecimal.ZERO;

    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom = LocalDate.now();

    public ChannelRateCard() {}

    public ChannelRateCard(String channelCode, BigDecimal commissionPct, BigDecimal fixedFee,
                           BigDecimal collectionFeePct, BigDecimal minCollectionFee, LocalDate effectiveFrom) {
        this.channelCode = channelCode;
        this.commissionPct = commissionPct;
        this.fixedFee = fixedFee;
        this.collectionFeePct = collectionFeePct;
        this.minCollectionFee = minCollectionFee;
        this.effectiveFrom = effectiveFrom;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getChannelCode() { return channelCode; }
    public void setChannelCode(String channelCode) { this.channelCode = channelCode; }
    public BigDecimal getCommissionPct() { return commissionPct; }
    public void setCommissionPct(BigDecimal commissionPct) { this.commissionPct = commissionPct; }
    public BigDecimal getFixedFee() { return fixedFee; }
    public void setFixedFee(BigDecimal fixedFee) { this.fixedFee = fixedFee; }
    public BigDecimal getCollectionFeePct() { return collectionFeePct; }
    public void setCollectionFeePct(BigDecimal collectionFeePct) { this.collectionFeePct = collectionFeePct; }
    public BigDecimal getMinCollectionFee() { return minCollectionFee; }
    public void setMinCollectionFee(BigDecimal minCollectionFee) { this.minCollectionFee = minCollectionFee; }
    public LocalDate getEffectiveFrom() { return effectiveFrom; }
    public void setEffectiveFrom(LocalDate effectiveFrom) { this.effectiveFrom = effectiveFrom; }
}