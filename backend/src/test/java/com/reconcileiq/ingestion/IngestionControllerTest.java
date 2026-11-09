package com.reconcileiq.ingestion;

import com.reconcileiq.ReconcileIqApplication;
import com.reconcileiq.auth.User;
import com.reconcileiq.auth.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = ReconcileIqApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class IngestionControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private SourceRepository sourceRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private String execToken;
    private String founderToken;

    @BeforeEach
    @Transactional
    void setup() throws Exception {
        userRepository.deleteAll();

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

        String execRes = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"exec@demo.com\",\"password\":\"ExecPassword123!\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String founderRes = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"founder@demo.com\",\"password\":\"FounderPassword123!\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        execToken = execRes.split("\"token\":\"")[1].split("\"")[0];
        founderToken = founderRes.split("\"token\":\"")[1].split("\"")[0];
    }

    @Test
    void testListSourcesUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/uploads/sources"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testListSourcesFinanceExecutive() throws Exception {
        mockMvc.perform(get("/api/v1/uploads/sources")
                .header("Authorization", "Bearer " + execToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void testListSourcesCofounder() throws Exception {
        mockMvc.perform(get("/api/v1/uploads/sources")
                .header("Authorization", "Bearer " + founderToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void testUploadSqlFileReturns400() throws Exception {
        // Create a source in the test DB
        Source source = new Source("Test Amazon", "AMAZON_TEST", "CHANNEL", "AMAZON", null);
        source = sourceRepository.save(source);

        MockMultipartFile sqlFile = new MockMultipartFile(
                "file",
                "V8__seed_additional_sources.sql",
                "application/sql",
                "INSERT INTO sources VALUES (1, 'test');".getBytes()
        );

        mockMvc.perform(multipart("/api/v1/uploads")
                .file(sqlFile)
                .param("sourceId", source.getId().toString())
                .param("fileType", "ORDERS")
                .header("Authorization", "Bearer " + execToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(
                        org.hamcrest.Matchers.containsString("Only .csv, .xlsx, .xls files are allowed")));
    }
}
