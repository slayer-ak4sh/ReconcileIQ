package com.reconcileiq.ratecard;

import com.reconcileiq.ReconcileIqApplication;
import com.reconcileiq.auth.User;
import com.reconcileiq.auth.UserRepository;
import com.reconcileiq.ratecard.ChannelRateCard;
import com.reconcileiq.ratecard.ChannelRateCardRepository;
import com.reconcileiq.ratecard.CourierRateCard;
import com.reconcileiq.ratecard.CourierRateCardRepository;
import com.reconcileiq.ratecard.CourierWeightSlab;
import com.reconcileiq.ratecard.CourierWeightSlabRepository;
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
public class RateCardsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ChannelRateCardRepository channelRateCardRepository;

    @Autowired
    private CourierRateCardRepository courierRateCardRepository;

    @Autowired
    private CourierWeightSlabRepository weightSlabRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String execToken;
    private String founderToken;

    @BeforeEach
    @Transactional
    void setup() throws Exception {
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

        // Seed rate cards (Flyway is disabled in tests, so we need to seed manually)
        // Only seed if not already present
        if (channelRateCardRepository.findByChannelCode("Website").isEmpty()) {
            ChannelRateCard channel1 = new ChannelRateCard();
            channel1.setChannelCode("Website");
            channel1.setCommissionPct(new BigDecimal("6.00"));
            channel1.setFixedFee(new BigDecimal("10.00"));
            channel1.setCollectionFeePct(new BigDecimal("1.00"));
            channel1.setMinCollectionFee(new BigDecimal("20.00"));
            channel1.setEffectiveFrom(LocalDate.now());
            channelRateCardRepository.save(channel1);

            ChannelRateCard channel2 = new ChannelRateCard();
            channel2.setChannelCode("Marketplace 1");
            channel2.setCommissionPct(new BigDecimal("5.00"));
            channel2.setFixedFee(new BigDecimal("0.00"));
            channel2.setCollectionFeePct(new BigDecimal("2.00"));
            channel2.setMinCollectionFee(new BigDecimal("10.00"));
            channel2.setEffectiveFrom(LocalDate.now());
            channelRateCardRepository.save(channel2);
        }

        if (courierRateCardRepository.findByCourierCode("Courier A").isEmpty()) {
            CourierRateCard courier1 = new CourierRateCard();
            courier1.setCourierCode("Courier A");
            courier1.setCodFeePct(new BigDecimal("2.00"));
            courier1.setMinCodFee(new BigDecimal("15.00"));
            courier1.setReturnFee(new BigDecimal("50.00"));
            courier1.setEffectiveFrom(LocalDate.now());
            courierRateCardRepository.save(courier1);

            CourierRateCard courier2 = new CourierRateCard();
            courier2.setCourierCode("Courier B");
            courier2.setCodFeePct(new BigDecimal("3.00"));
            courier2.setMinCodFee(new BigDecimal("20.00"));
            courier2.setReturnFee(new BigDecimal("75.00"));
            courier2.setEffectiveFrom(LocalDate.now());
            courierRateCardRepository.save(courier2);

            // Weight slabs
            CourierWeightSlab w1 = new CourierWeightSlab();
            w1.setRateCard(courier1);
            w1.setMinWeightKg(new BigDecimal("0.001"));
            w1.setMaxWeightKg(new BigDecimal("5.000"));
            w1.setRate(new BigDecimal("100.00"));
            weightSlabRepository.save(w1);

            CourierWeightSlab w2 = new CourierWeightSlab();
            w2.setRateCard(courier1);
            w2.setMinWeightKg(new BigDecimal("5.001"));
            w2.setMaxWeightKg(new BigDecimal("10.000"));
            w2.setRate(new BigDecimal("80.00"));
            weightSlabRepository.save(w2);

            CourierWeightSlab w3 = new CourierWeightSlab();
            w3.setRateCard(courier1);
            w3.setMinWeightKg(new BigDecimal("10.001"));
            w3.setMaxWeightKg(new BigDecimal("1000.000"));
            w3.setRate(new BigDecimal("60.00"));
            weightSlabRepository.save(w3);

            CourierWeightSlab w4 = new CourierWeightSlab();
            w4.setRateCard(courier2);
            w4.setMinWeightKg(new BigDecimal("0.001"));
            w4.setMaxWeightKg(new BigDecimal("3.000"));
            w4.setRate(new BigDecimal("120.00"));
            weightSlabRepository.save(w4);

            CourierWeightSlab w5 = new CourierWeightSlab();
            w5.setRateCard(courier2);
            w5.setMinWeightKg(new BigDecimal("3.001"));
            w5.setMaxWeightKg(new BigDecimal("5.000"));
            w5.setRate(new BigDecimal("100.00"));
            weightSlabRepository.save(w5);

            CourierWeightSlab w6 = new CourierWeightSlab();
            w6.setRateCard(courier2);
            w6.setMinWeightKg(new BigDecimal("5.001"));
            w6.setMaxWeightKg(new BigDecimal("1000.000"));
            w6.setRate(new BigDecimal("80.00"));
            weightSlabRepository.save(w6);
        }
    }

    @Test
    void testGetChannelRateCardsUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/ratecards"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testGetChannelRateCardsAuth() throws Exception {
        mockMvc.perform(get("/api/v1/ratecards")
                .header("Authorization", "Bearer " + execToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        mockMvc.perform(get("/api/v1/ratecards")
                .header("Authorization", "Bearer " + founderToken))
                .andExpect(status().isOk());
    }

    @Test
    void testGetCourierRateCardsAuth() throws Exception {
        mockMvc.perform(get("/api/v1/ratecards/courier")
                .header("Authorization", "Bearer " + execToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        mockMvc.perform(get("/api/v1/ratecards/courier")
                .header("Authorization", "Bearer " + founderToken))
                .andExpect(status().isOk());
    }

    @Test
    void testPutChannelRateCardExecutiveOnly() throws Exception {
        // Executive can PUT
        mockMvc.perform(put("/api/v1/ratecards/channel/Website")
                .header("Authorization", "Bearer " + execToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "channelCode": "Website",
                        "commissionPct": 6.0,
                        "fixedFee": 10.0,
                        "collectionFeePct": 1.0,
                        "minCollectionFee": 20.0
                    }
                    """))
                .andExpect(status().isOk());

        // Co-founder cannot PUT (403)
        mockMvc.perform(put("/api/v1/ratecards/channel/Website")
                .header("Authorization", "Bearer " + founderToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "channelCode": "Website",
                        "commissionPct": 6.0,
                        "fixedFee": 10.0,
                        "collectionFeePct": 1.0,
                        "minCollectionFee": 20.0
                    }
                    """))
                .andExpect(status().isForbidden());

        // Unauthenticated gets 401
        mockMvc.perform(put("/api/v1/ratecards/channel/Website")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testPutCourierRateCardExecutiveOnly() throws Exception {
        mockMvc.perform(put("/api/v1/ratecards/courier/Courier A")
                .header("Authorization", "Bearer " + execToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "courierCode": "Courier A",
                        "codFeePct": 2.0,
                        "minCodFee": 15.0,
                        "returnFee": 50.0
                    }
                    """))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/v1/ratecards/courier/Courier A")
                .header("Authorization", "Bearer " + founderToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void testGetSpecificRateCard() throws Exception {
        mockMvc.perform(get("/api/v1/ratecards/channel/Website")
                .header("Authorization", "Bearer " + execToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/ratecards/courier/Courier A")
                .header("Authorization", "Bearer " + execToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/ratecards/channel/NonExistent")
                .header("Authorization", "Bearer " + execToken))
                .andExpect(status().isNotFound());
    }
}