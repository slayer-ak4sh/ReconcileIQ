package com.reconcileiq.ratecard;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "courier_rate_cards")
public class CourierRateCard {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "courier_code", nullable = false, unique = true, length = 50)
    private String courierCode;

    @Column(name = "cod_fee_pct", nullable = false, precision = 5, scale = 2)
    private BigDecimal codFeePct = BigDecimal.ZERO;

    @Column(name = "min_cod_fee", nullable = false, precision = 12, scale = 2)
    private BigDecimal minCodFee = BigDecimal.ZERO;

    @Column(name = "return_fee", nullable = false, precision = 12, scale = 2)
    private BigDecimal returnFee = BigDecimal.ZERO;

    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom = LocalDate.now();

    public CourierRateCard() {}

    public CourierRateCard(String courierCode, BigDecimal codFeePct, BigDecimal minCodFee,
                           BigDecimal returnFee, LocalDate effectiveFrom) {
        this.courierCode = courierCode;
        this.codFeePct = codFeePct;
        this.minCodFee = minCodFee;
        this.returnFee = returnFee;
        this.effectiveFrom = effectiveFrom;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCourierCode() { return courierCode; }
    public void setCourierCode(String courierCode) { this.courierCode = courierCode; }
    public BigDecimal getCodFeePct() { return codFeePct; }
    public void setCodFeePct(BigDecimal codFeePct) { this.codFeePct = codFeePct; }
    public BigDecimal getMinCodFee() { return minCodFee; }
    public void setMinCodFee(BigDecimal minCodFee) { this.minCodFee = minCodFee; }
    public BigDecimal getReturnFee() { return returnFee; }
    public void setReturnFee(BigDecimal returnFee) { this.returnFee = returnFee; }
    public LocalDate getEffectiveFrom() { return effectiveFrom; }
    public void setEffectiveFrom(LocalDate effectiveFrom) { this.effectiveFrom = effectiveFrom; }
}