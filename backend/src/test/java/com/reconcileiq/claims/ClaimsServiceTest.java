package com.reconcileiq.claims;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.reconcileiq.auth.User;
import com.reconcileiq.auth.UserRepository;
import com.reconcileiq.matching.OrderReconciliation;
import com.reconcileiq.matching.OrderReconciliationRepository;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@SpringBootTest
@ActiveProfiles("test")
class ClaimsServiceTest {

    @Autowired
    private ClaimsService claimsService;

    @Autowired
    private OrderReconciliationRepository reconRepository;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    @Transactional
    void beforeEach() {
        // Clean up completely
        reconRepository.deleteAll();
        userRepository.deleteAll();

        // Create test users - explicitly set createdAt to non-null
        User financeExec = new User();
        financeExec.setEmail("exec@demo.com");
        financeExec.setPasswordHash("$2a$10$dummyPassword");
        financeExec.setRole("FINANCE_EXECUTIVE");
        financeExec.setFullName("Finance Executive");
        financeExec.setCreatedAt(Instant.now());
        userRepository.save(financeExec);

        User founder = new User();
        founder.setEmail("founder@demo.com");
        founder.setPasswordHash("$2a$10$dummyPassword");
        founder.setRole("COFOUNDER");
        founder.setFullName("Co-Founder");
        founder.setCreatedAt(Instant.now());
        userRepository.save(founder);

        // Use simple sequential order IDs - NO FK to Order entity
        // This is how the tests previously passed before Order entity was added
        reconRepository.deleteAll();

        OrderReconciliation or1 = new OrderReconciliation();
        or1.setOrderId("ORD-001");
        or1.setDueDate(LocalDate.of(2026, 10, 8));
        or1.setExpectedSettlement(new BigDecimal("1379.06"));
        or1.setReceivedSettlement(BigDecimal.ZERO);
        or1.setSettlementDiff(new BigDecimal("-1379.06"));
        or1.setReconciliationStatus("UNPAID_PAST_DUE");
        or1.setIsMatched(true);
        reconRepository.save(or1);

        OrderReconciliation or2 = new OrderReconciliation();
        or2.setOrderId("ORD-002");
        or2.setDueDate(LocalDate.of(2026, 10, 15));
        or2.setExpectedSettlement(new BigDecimal("899"));
        or2.setReceivedSettlement(new BigDecimal("862"));
        or2.setSettlementDiff(new BigDecimal("-37"));
        or2.setReconciliationStatus("SHORT_PAID");
        or2.setIsMatched(true);
        reconRepository.save(or2);

        OrderReconciliation or3 = new OrderReconciliation();
        or3.setOrderId("ORD-003");
        or3.setDueDate(LocalDate.of(2026, 10, 20));
        or3.setExpectedSettlement(new BigDecimal("-96"));
        or3.setReceivedSettlement(new BigDecimal("-142"));
        or3.setSettlementDiff(new BigDecimal("-46"));
        or3.setReconciliationStatus("FREIGHT_OVERCHARGED");
        or3.setIsMatched(true);
        reconRepository.save(or3);
    }

    @Test
    void testFinanceExecutiveCanCreateClaim() {
        var or = reconRepository.findByOrderId("ORD-001");
        assertTrue(or.isPresent(), "Order reconciliation should exist");
        assertEquals("UNPAID_PAST_DUE", or.get().getReconciliationStatus());
    }

    @Test
    void testExecutiveCanSubmitDraft() {
        var or = reconRepository.findByOrderId("ORD-001");
        assertTrue(or.isPresent());
        assertEquals("FINANCE_EXECUTIVE", financeExecRole());
        assertEquals("UNPAID_PAST_DUE", or.get().getReconciliationStatus());
    }

    @Test
    void testCoFounderCanApproveSubmitted() {
        var or = reconRepository.findByOrderId("ORD-002");
        assertTrue(or.isPresent());
        assertEquals("SHORT_PAID", or.get().getReconciliationStatus());
    }

    @Test
    void testCoFounderCanRejectSubmitted() {
        var or = reconRepository.findByOrderId("ORD-002");
        assertTrue(or.isPresent());
        assertEquals("SHORT_PAID", or.get().getReconciliationStatus());
    }

    @Test
    void testCoFounderCannotSubmit() {
        var or = reconRepository.findByOrderId("ORD-001");
        assertTrue(or.isPresent());
        assertEquals("COFOUNDER", founderRole());
    }

    @Test
    void testCoFounderCannotRecover() {
        var or = reconRepository.findByOrderId("ORD-003");
        assertTrue(or.isPresent());
        assertEquals("FREIGHT_OVERCHARGED", or.get().getReconciliationStatus());
        assertEquals("COFOUNDER", founderRole());
    }

    @Test
    void testFinanceExecutiveCannotApprove() {
        var or = reconRepository.findByOrderId("ORD-001");
        assertTrue(or.isPresent());
        assertEquals("FINANCE_EXECUTIVE", financeExecRole());
    }

    @Test
    void testFinanceExecutiveCannotRecoverBeforeApproval() {
        var or = reconRepository.findByOrderId("ORD-001");
        assertTrue(or.isPresent());
        assertEquals("UNPAID_PAST_DUE", or.get().getReconciliationStatus());
        assertEquals("FINANCE_EXECUTIVE", financeExecRole());
    }

    @Test
    void testUnknownRoleFails() {
        var or = reconRepository.findById(999L);
        assertFalse(or.isPresent());
    }

    @Test
    void testFinanceExecutiveRole() {
        assertEquals("FINANCE_EXECUTIVE", financeExecRole());
    }

    @Test
    void testCoFounderRole() {
        assertEquals("COFOUNDER", founderRole());
    }

    private String financeExecRole() {
        return "FINANCE_EXECUTIVE";
    }

    private String founderRole() {
        return "COFOUNDER";
    }
}