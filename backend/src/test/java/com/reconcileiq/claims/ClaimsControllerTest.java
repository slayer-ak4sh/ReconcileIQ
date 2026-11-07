package com.reconcileiq.claims;

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
@Transactional
public class ClaimsControllerTest {

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
        userRepository.flush();

        // Create users
        User exec = new User();
        exec.setEmail("exec@demo.com");
        exec.setPasswordHash(passwordEncoder.encode("ExecPassword123!"));
        exec.setRole("FINANCE_EXECUTIVE");
        exec.setFullName("Finance Executive");
        exec.setCreatedAt(Instant.now());
        userRepository.saveAndFlush(exec);

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

        // Create test order reconciliations - save them in same transaction
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
    }

    @Test
    void testCreateClaimExecutiveOnly() throws Exception {
        // Executive can create
        mockMvc.perform(post("/api/v1/claims")
                .header("Authorization", "Bearer " + execToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "partnerCode": "Website",
                        "partnerType": "CHANNEL",
                        "createdBy": "exec@demo.com",
                        "items": [
                            {"orderId": "ORD-001", "claimType": "SHORT_PAYMENT", "amount": 1000, "evidenceNotes": "test"}
                        ]
                    }
                    """))
                .andExpect(status().isOk());

        // Co-founder cannot create (403)
        mockMvc.perform(post("/api/v1/claims")
                .header("Authorization", "Bearer " + founderToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "partnerCode": "Website",
                        "partnerType": "CHANNEL",
                        "createdBy": "founder@demo.com",
                        "items": [
                            {"orderId": "ORD-001", "claimType": "SHORT_PAYMENT", "amount": 1000, "evidenceNotes": "test"}
                        ]
                    }
                    """))
                .andExpect(status().isForbidden());

        // Unauthenticated gets 401
        mockMvc.perform(post("/api/v1/claims")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testSubmitClaimExecutiveOnly() throws Exception {
        // Create draft claim as executive
        String createResp = mockMvc.perform(post("/api/v1/claims")
                .header("Authorization", "Bearer " + execToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "partnerCode": "Website",
                        "partnerType": "CHANNEL",
                        "createdBy": "exec@demo.com",
                        "items": [
                            {"orderId": "ORD-001", "claimType": "SHORT_PAYMENT", "amount": 1000, "evidenceNotes": "test"}
                        ]
                    }
                    """))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String claimId = createResp.split("\"id\":")[1].split(",")[0];

        // Executive can submit
        mockMvc.perform(post("/api/v1/claims/" + claimId + "/submit")
                .header("Authorization", "Bearer " + execToken))
                .andExpect(status().isOk());

        // Co-founder cannot submit (403)
        mockMvc.perform(post("/api/v1/claims/" + claimId + "/submit")
                .header("Authorization", "Bearer " + founderToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void testApproveRejectCoFounderOnly() throws Exception {
        // Create and submit claim as executive
        String createResp = mockMvc.perform(post("/api/v1/claims")
                .header("Authorization", "Bearer " + execToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "partnerCode": "Website",
                        "partnerType": "CHANNEL",
                        "createdBy": "exec@demo.com",
                        "items": [
                            {"orderId": "ORD-002", "claimType": "SHORT_PAYMENT", "amount": 1000, "evidenceNotes": "test"}
                        ]
                    }
                    """))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String claimId = createResp.split("\"id\":")[1].split(",")[0];

        mockMvc.perform(post("/api/v1/claims/" + claimId + "/submit")
                .header("Authorization", "Bearer " + execToken))
                .andExpect(status().isOk());

        // Co-founder CAN approve
        mockMvc.perform(post("/api/v1/claims/" + claimId + "/approve")
                .header("Authorization", "Bearer " + founderToken)
                .param("notes", "Approved"))
                .andExpect(status().isOk());

        // Create another claim for reject test
        String createResp2 = mockMvc.perform(post("/api/v1/claims")
                .header("Authorization", "Bearer " + execToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "partnerCode": "Website",
                        "partnerType": "CHANNEL",
                        "createdBy": "exec@demo.com",
                        "items": [
                            {"orderId": "ORD-002", "claimType": "SHORT_PAYMENT", "amount": 1000, "evidenceNotes": "test"}
                        ]
                    }
                    """))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String claimId2 = createResp2.split("\"id\":")[1].split(",")[0];

        mockMvc.perform(post("/api/v1/claims/" + claimId2 + "/submit")
                .header("Authorization", "Bearer " + execToken))
                .andExpect(status().isOk());

        // Co-founder CAN reject
        mockMvc.perform(post("/api/v1/claims/" + claimId2 + "/reject")
                .header("Authorization", "Bearer " + founderToken)
                .param("notes", "Rejected"))
                .andExpect(status().isOk());

        // Executive CANNOT approve (403)
        mockMvc.perform(post("/api/v1/claims/" + claimId + "/approve")
                .header("Authorization", "Bearer " + execToken)
                .param("notes", "test"))
                .andExpect(status().isForbidden());

        // Executive CANNOT reject (403)
        mockMvc.perform(post("/api/v1/claims/" + claimId2 + "/reject")
                .header("Authorization", "Bearer " + execToken)
                .param("notes", "test"))
                .andExpect(status().isForbidden());
    }

    @Test
    void testRecoverExecutiveOnly() throws Exception {
        // Create, submit, approve claim
        String createResp = mockMvc.perform(post("/api/v1/claims")
                .header("Authorization", "Bearer " + execToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "partnerCode": "Website",
                        "partnerType": "CHANNEL",
                        "createdBy": "exec@demo.com",
                        "items": [
                            {"orderId": "ORD-001", "claimType": "SHORT_PAYMENT", "amount": 1000, "evidenceNotes": "test"}
                        ]
                    }
                    """))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String claimId = createResp.split("\"id\":")[1].split(",")[0];

        mockMvc.perform(post("/api/v1/claims/" + claimId + "/submit")
                .header("Authorization", "Bearer " + execToken))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/claims/" + claimId + "/approve")
                .header("Authorization", "Bearer " + founderToken)
                .param("notes", "Approved"))
                .andExpect(status().isOk());

        // Executive CAN recover
        mockMvc.perform(post("/api/v1/claims/" + claimId + "/recover")
                .header("Authorization", "Bearer " + execToken))
                .andExpect(status().isOk());

        // Co-founder CANNOT recover (403)
        mockMvc.perform(post("/api/v1/claims/" + claimId + "/recover")
                .header("Authorization", "Bearer " + founderToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void testListAndDetailUnauthenticated() throws Exception {
        // Unauthenticated gets 401 for list
        mockMvc.perform(get("/api/v1/claims"))
                .andExpect(status().isUnauthorized());

        // Unauthenticated gets 401 for detail
        mockMvc.perform(get("/api/v1/claims/1"))
                .andExpect(status().isUnauthorized());

        // Unauthenticated gets 401 for evidence.csv
        mockMvc.perform(get("/api/v1/claims/1/evidence.csv"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testEvidenceCsvBothRoles() throws Exception {
        String createResp = mockMvc.perform(post("/api/v1/claims")
                .header("Authorization", "Bearer " + execToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "partnerCode": "Website",
                        "partnerType": "CHANNEL",
                        "createdBy": "exec@demo.com",
                        "items": [
                            {"orderId": "ORD-001", "claimType": "SHORT_PAYMENT", "amount": 1000, "evidenceNotes": "test"}
                        ]
                    }
                    """))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String claimId = createResp.split("\"id\":")[1].split(",")[0];

        // Executive can download evidence.csv
        mockMvc.perform(get("/api/v1/claims/" + claimId + "/evidence.csv")
                .header("Authorization", "Bearer " + execToken))
                .andExpect(status().isOk())
                .andExpect(content().contentType("text/csv"));

        // Co-founder can download evidence.csv
        mockMvc.perform(get("/api/v1/claims/" + claimId + "/evidence.csv")
                .header("Authorization", "Bearer " + founderToken))
                .andExpect(status().isOk())
                .andExpect(content().contentType("text/csv"));
    }
}