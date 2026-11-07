package com.reconcileiq.dashboard;

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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = ReconcileIqApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class DashboardControllerTest {

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

        // Create test order reconciliations for dashboard
        OrderReconciliation or1 = new OrderReconciliation();
        or1.setOrderId("ORD-001");
        or1.setDueDate(LocalDate.of(2026, 10, 1));
        or1.setExpectedSettlement(new BigDecimal("1000"));
        or1.setReceivedSettlement(BigDecimal.ZERO);
        or1.setSettlementDiff(new BigDecimal("-1000"));
        or1.setReconciliationStatus("UNPAID_PAST_DUE");
        or1.setIsMatched(true);
        reconRepository.save(or1);

        OrderReconciliation or2 = new OrderReconciliation();
        or2.setOrderId("ORD-002");
        or2.setDueDate(LocalDate.of(2026, 10, 5));
        or2.setExpectedSettlement(new BigDecimal("500"));
        or2.setReceivedSettlement(new BigDecimal("450"));
        or2.setSettlementDiff(new BigDecimal("-50"));
        or2.setReconciliationStatus("SHORT_PAID");
        or2.setIsMatched(true);
        reconRepository.save(or2);
    }

    @Test
    void testDashboardSummaryUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/dashboard/summary"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testDashboardSummaryAuth() throws Exception {
        mockMvc.perform(get("/api/v1/dashboard/summary")
                .header("Authorization", "Bearer " + execToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalOrders").exists())
                .andExpect(jsonPath("$.moneyAtRisk").exists())
                .andExpect(jsonPath("$.byPartner").exists())
                .andExpect(jsonPath("$.byAgeBucket").exists());

        mockMvc.perform(get("/api/v1/dashboard/summary")
                .header("Authorization", "Bearer " + founderToken))
                .andExpect(status().isOk());
    }

    @Test
    void testDashboardMoneyAtRiskUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/dashboard/money-at-risk"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testDashboardMoneyAtRiskAuth() throws Exception {
        mockMvc.perform(get("/api/v1/dashboard/money-at-risk")
                .header("Authorization", "Bearer " + execToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.byPartner").exists())
                .andExpect(jsonPath("$.byAgeBucket").exists());

        mockMvc.perform(get("/api/v1/dashboard/money-at-risk")
                .header("Authorization", "Bearer " + founderToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.byPartner").exists())
                .andExpect(jsonPath("$.byAgeBucket").exists());
    }

    @Test
    void testDashboardWithAsOfDate() throws Exception {
        mockMvc.perform(get("/api/v1/dashboard/summary?asOfDate=2026-10-10")
                .header("Authorization", "Bearer " + execToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/dashboard/money-at-risk?asOfDate=2026-10-10")
                .header("Authorization", "Bearer " + execToken))
                .andExpect(status().isOk());
    }
}