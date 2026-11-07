package com.reconcileiq.ratecard;

import com.reconcileiq.common.ResourceNotFoundException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/ratecards")
public class RateCardsController {

    private final RateCardService rateCardService;

    public RateCardsController(RateCardService rateCardService) {
        this.rateCardService = rateCardService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('FINANCE_EXECUTIVE') or hasAuthority('COFOUNDER')")
    public List<ChannelRateCard> getChannelRateCards() {
        return rateCardService.getAllChannelRateCards();
    }

    @GetMapping("/courier")
    @PreAuthorize("hasAuthority('FINANCE_EXECUTIVE') or hasAuthority('COFOUNDER')")
    public List<CourierRateCard> getCourierRateCards() {
        return rateCardService.getAllCourierRateCards();
    }

    @GetMapping("/channel/{channelCode}")
    @PreAuthorize("hasAuthority('FINANCE_EXECUTIVE') or hasAuthority('COFOUNDER')")
    public ChannelRateCard getChannelRateCard(@PathVariable String channelCode) {
        return rateCardService.getChannelRateCard(channelCode).orElseThrow(() -> new ResourceNotFoundException("Channel rate card not found: " + channelCode));
    }

    @GetMapping("/courier/{courierCode}")
    @PreAuthorize("hasAuthority('FINANCE_EXECUTIVE') or hasAuthority('COFOUNDER')")
    public CourierRateCard getCourierRateCard(@PathVariable String courierCode) {
        return rateCardService.getCourierRateCard(courierCode).orElseThrow(() -> new ResourceNotFoundException("Courier rate card not found: " + courierCode));
    }

    @PutMapping("/channel/{channelCode}")
    @PreAuthorize("hasAuthority('FINANCE_EXECUTIVE')")
    public ChannelRateCard updateChannelRateCard(@PathVariable String channelCode,
                                                 @RequestBody ChannelRateCard card) {
        ChannelRateCard existing = rateCardService.getChannelRateCard(channelCode)
                .orElseThrow(() -> new ResourceNotFoundException("Channel rate card not found: " + channelCode));
        existing.setCommissionPct(card.getCommissionPct());
        existing.setFixedFee(card.getFixedFee());
        existing.setCollectionFeePct(card.getCollectionFeePct());
        existing.setMinCollectionFee(card.getMinCollectionFee());
        existing.setEffectiveFrom(card.getEffectiveFrom());
        return rateCardService.saveChannelRateCard(existing);
    }

    @PutMapping("/courier/{courierCode}")
    @PreAuthorize("hasAuthority('FINANCE_EXECUTIVE')")
    public CourierRateCard updateCourierRateCard(@PathVariable String courierCode,
                                                 @RequestBody CourierRateCard card) {
        CourierRateCard existing = rateCardService.getCourierRateCard(courierCode)
                .orElseThrow(() -> new ResourceNotFoundException("Courier rate card not found: " + courierCode));
        existing.setCodFeePct(card.getCodFeePct());
        existing.setMinCodFee(card.getMinCodFee());
        existing.setReturnFee(card.getReturnFee());
        existing.setEffectiveFrom(card.getEffectiveFrom());
        return rateCardService.saveCourierRateCard(existing);
    }
}