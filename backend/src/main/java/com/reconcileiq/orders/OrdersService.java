package com.reconcileiq.orders;

import com.reconcileiq.common.PiiMasker;
import com.reconcileiq.ingestion.Order;
import com.reconcileiq.ingestion.OrderRepository;
import com.reconcileiq.ingestion.Settlement;
import com.reconcileiq.ingestion.SettlementRepository;
import com.reconcileiq.ingestion.CodRemittance;
import com.reconcileiq.ingestion.CodRemittanceRepository;
import com.reconcileiq.ingestion.FreightInvoice;
import com.reconcileiq.ingestion.FreightInvoiceRepository;
import com.reconcileiq.matching.OrderReconciliation;
import com.reconcileiq.matching.OrderReconciliationRepository;
import com.reconcileiq.ratecard.ChannelRateCard;
import com.reconcileiq.ratecard.ChannelRateCardRepository;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class OrdersService {

    private final OrderRepository orderRepository;
    private final OrderReconciliationRepository reconRepository;
    private final SettlementRepository settlementRepository;
    private final CodRemittanceRepository codRemittanceRepository;
    private final FreightInvoiceRepository freightInvoiceRepository;
    private final ChannelRateCardRepository channelRateCardRepository;

    public OrdersService(OrderRepository orderRepository,
                         OrderReconciliationRepository reconRepository,
                         SettlementRepository settlementRepository,
                         CodRemittanceRepository codRemittanceRepository,
                         FreightInvoiceRepository freightInvoiceRepository,
                         ChannelRateCardRepository channelRateCardRepository) {
        this.orderRepository = orderRepository;
        this.reconRepository = reconRepository;
        this.settlementRepository = settlementRepository;
        this.codRemittanceRepository = codRemittanceRepository;
        this.freightInvoiceRepository = freightInvoiceRepository;
        this.channelRateCardRepository = channelRateCardRepository;
    }

    @Transactional(readOnly = true)
    public Page<OrderListItemResponse> searchOrders(String status, String channel, String partner,
                                                     String search, LocalDate fromDate, LocalDate toDate,
                                                     BigDecimal minAbsDifference, Pageable pageable) {

        Specification<OrderReconciliation> spec = Specification.where((root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Status filter
            if (status != null && !status.isEmpty()) {
                predicates.add(cb.equal(root.get("reconciliationStatus"), status));
            }

            // Search filter (order_id)
            if (search != null && !search.isEmpty()) {
                predicates.add(cb.like(root.get("orderId"), "%" + search + "%"));
            }

            // Min absolute difference filter (settlement_diff)
            if (minAbsDifference != null) {
                predicates.add(cb.greaterThanOrEqualTo(
                        cb.abs(root.get("settlementDiff")),
                        ((Number) minAbsDifference.doubleValue()).doubleValue()
                ));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        });

        Page<OrderReconciliation> page = reconRepository.findAll(spec, pageable);

        List<OrderListItemResponse> responses = page.getContent().stream()
                .map(this::toListItemResponse)
                .collect(Collectors.toList());

        return new PageImpl<>(responses, pageable, page.getTotalElements());
    }

    @Transactional(readOnly = true)
    public OrderDetailResponse getOrderDetail(String orderId) {
        OrderReconciliation recon = reconRepository.findByOrderId(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));

        List<SettlementDetail> settlements = settlementRepository.findByOrderId(orderId).stream()
                .map(this::toSettlementDetail)
                .collect(Collectors.toList());

        List<CodRemittanceDetail> codRemittances = codRemittanceRepository.findByOrderId(orderId).stream()
                .map(this::toCodRemittanceDetail)
                .collect(Collectors.toList());

        FreightDetail freight = freightInvoiceRepository.findByOrderId(orderId).stream()
                .max((a, b) -> a.getInvoiceDate().compareTo(b.getInvoiceDate()))
                .map(this::toFreightDetail)
                .orElse(null);

        // No order entity FK anymore - use defaults for order fields
        String channelCode = "UNKNOWN";
        String paymentType = "PREPAID";
        BigDecimal orderValue = BigDecimal.ZERO;

        ChannelRateCard rateCard = channelRateCardRepository.findByChannelCode(channelCode).orElse(null);

        CalculationBreakdown breakdown = new CalculationBreakdown(
                recon.getExpectedSettlement(),
                recon.getReceivedSettlement(),
                recon.getSettlementDiff(),
                recon.getTotalTcs(),
                recon.getTotalTds(),
                settlements,
                codRemittances,
                freight,
                rateCard
        );

        return new OrderDetailResponse(
                orderId,
                recon.getReconciliationStatus(),
                recon.getDueDate(),
                "—",
                "—",
                "—",
                breakdown
        );
    }

    private OrderListItemResponse toListItemResponse(OrderReconciliation recon) {
        return new OrderListItemResponse(
                recon.getOrderId(),
                "UNKNOWN",
                null,
                BigDecimal.ZERO,
                "PREPAID",
                "DELIVERED",
                recon.getReconciliationStatus(),
                recon.getDueDate(),
                recon.getExpectedSettlement(),
                recon.getReceivedSettlement(),
                recon.getSettlementDiff(),
                recon.getTotalTcs(),
                recon.getTotalTds()
        );
    }

    private SettlementDetail toSettlementDetail(Settlement settlement) {
        return new SettlementDetail(
                settlement.getReferenceNo(),
                settlement.getSettlementDate(),
                settlement.getGrossAmount(),
                settlement.getCommission(),
                settlement.getFixedFee(),
                settlement.getCollectionFee(),
                settlement.getTcsAmount(),
                settlement.getTdsAmount(),
                settlement.getNetAmount()
        );
    }

    private CodRemittanceDetail toCodRemittanceDetail(CodRemittance cod) {
        return new CodRemittanceDetail(
                cod.getUtrNo(),
                cod.getRemittanceDate(),
                cod.getCodCollected(),
                cod.getFeeDeducted(),
                cod.getNetRemitted()
        );
    }

    private FreightDetail toFreightDetail(FreightInvoice freightInvoice) {
        return new FreightDetail(
                freightInvoice.getInvoiceNo(),
                freightInvoice.getInvoiceDate(),
                freightInvoice.getBilledWeight(),
                freightInvoice.getFreightCharge(),
                freightInvoice.getCodFee(),
                freightInvoice.getReturnFee(),
                freightInvoice.getTotalFreight()
        );
    }

    // Inner record classes for responses
    public record OrderListItemResponse(
            String orderId,
            String channelCode,
            LocalDate orderDate,
            BigDecimal orderValue,
            String paymentType,
            String deliveryStatus,
            String reconciliationStatus,
            LocalDate dueDate,
            BigDecimal expectedSettlement,
            BigDecimal receivedSettlement,
            BigDecimal settlementDiff,
            BigDecimal totalTcs,
            BigDecimal totalTds
    ) {}

    public record OrderDetailResponse(
            String orderId,
            String reconciliationStatus,
            LocalDate dueDate,
            String maskedName,
            String maskedPhone,
            String maskedAddress,
            CalculationBreakdown calculation
    ) {}

    public record SettlementDetail(
            String referenceNo,
            LocalDate settlementDate,
            BigDecimal grossAmount,
            BigDecimal commission,
            BigDecimal fixedFee,
            BigDecimal collectionFee,
            BigDecimal tcsAmount,
            BigDecimal tdsAmount,
            BigDecimal netAmount
    ) {}

    public record CodRemittanceDetail(
            String utrNo,
            LocalDate remittanceDate,
            BigDecimal codCollected,
            BigDecimal feeDeducted,
            BigDecimal netRemitted
    ) {}

    public record FreightDetail(
            String invoiceNo,
            LocalDate invoiceDate,
            BigDecimal billedWeight,
            BigDecimal freightCharge,
            BigDecimal codFee,
            BigDecimal returnFee,
            BigDecimal totalFreight
    ) {}

    public record RateCardDetail(
            String channelCode,
            BigDecimal commissionPct,
            BigDecimal fixedFee,
            BigDecimal collectionFeePct,
            BigDecimal minCollectionFee,
            LocalDate effectiveFrom
    ) {}

    public record CalculationBreakdown(
            BigDecimal expected,
            BigDecimal received,
            BigDecimal difference,
            BigDecimal tcs,
            BigDecimal tds,
            List<SettlementDetail> settlements,
            List<CodRemittanceDetail> codRemittances,
            FreightDetail freight,
            ChannelRateCard rateCard
    ) {}
}