package com.reconcileiq.orders;

import com.reconcileiq.ReconcileIqApplication;
import com.reconcileiq.auth.User;
import com.reconcileiq.auth.UserRepository;
import com.reconcileiq.matching.OrderReconciliation;
import com.reconcileiq.matching.OrderReconciliationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = ReconcileIqApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class OrdersControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrderReconciliationRepository reconRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String execToken;
    private String founderToken;

    @BeforeEach
    @Transactional
    void setup() throws Exception {
        reconRepository.deleteAll();
        userRepository.deleteAll();

        // Create users
        User exec = new User();
        exec.setEmail("exec@demo.com");
        exec.setPasswordHash(passwordEncoder.encode("ExecPassword123!"));
        exec.setRole("FINANCE_EXECUTIVE");
        exec.setFullName("Finance Executive");
        exec.setCreatedAt(Instant.now());
        userRepository.save(exec);

        User founder = new User();
        founder.setEmail("founder@demo.com");
        founder.setPasswordHash(passwordEncoder.encode("FounderPassword123!"));
        founder.setRole("COFOUNDER");
        founder.setFullName("Co-Founder");
        founder.setCreatedAt(Instant.now());
        userRepository.save(founder);

        // Login to get tokens
        String execLogin = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"exec@demo.com\",\"password\":\"ExecPassword123!\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String founderLogin = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"founder@demo.com\",\"password\":\"FounderPassword123!\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        execToken = execLogin.split("\"token\":\"")[1].split("\"")[0];
        founderToken = founderLogin.split("\"token\":\"")[1].split("\"")[0];

        // Create test order reconciliations
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
        or3.setExpectedSettlement(new BigDecimal("500"));
        or3.setReceivedSettlement(new BigDecimal("500"));
        or3.setSettlementDiff(BigDecimal.ZERO);
        or3.setReconciliationStatus("SETTLED");
        or3.setIsMatched(true);
        reconRepository.save(or3);
    }

    @Test
    void testOrdersListUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/orders"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testOrdersListPaginated() throws Exception {
        mockMvc.perform(get("/api/v1/orders?page=0&size=2")
                .header("Authorization", "Bearer " + execToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(2));
    }

    @Test
    void testOrdersListStatusFilter() throws Exception {
        mockMvc.perform(get("/api/v1/orders?status=UNPAID_PAST_DUE")
                .header("Authorization", "Bearer " + execToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].reconciliationStatus").value("UNPAID_PAST_DUE"));

        mockMvc.perform(get("/api/v1/orders?status=SHORT_PAID")
                .header("Authorization", "Bearer " + execToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].reconciliationStatus").value("SHORT_PAID"));
    }

    @Test
    void testOrdersDetailUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/orders/ORD-001"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testOrdersDetailMaskedPii() throws Exception {
        mockMvc.perform(get("/api/v1/orders/ORD-001")
                .header("Authorization", "Bearer " + execToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.maskedName").value("—"))
                .andExpect(jsonPath("$.maskedPhone").value("—"))
                .andExpect(jsonPath("$.maskedAddress").value("—"));
    }

    @Test
    void testOrdersDetailBothRoles() throws Exception {
        // Executive
        mockMvc.perform(get("/api/v1/orders/ORD-001")
                .header("Authorization", "Bearer " + execToken))
                .andExpect(status().isOk());

        // Co-founder
        mockMvc.perform(get("/api/v1/orders/ORD-001")
                .header("Authorization", "Bearer " + founderToken))
                .andExpect(status().isOk());
    }

    @Test
    void testOrdersSearch() throws Exception {
        mockMvc.perform(get("/api/v1/orders?search=ORD-001")
                .header("Authorization", "Bearer " + execToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].orderId").value("ORD-001"));
    }
}