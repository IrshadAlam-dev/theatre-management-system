package com.example.theatre;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.example.theatre.repository.UserRepository;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "app.jwt.secret-base64=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=")
@Transactional
class SecurityIT {

    @Autowired private WebApplicationContext context;
    @Autowired private JwtEncoder encoder;
    @Autowired private UserRepository users;
    @Autowired private PasswordEncoder passwordEncoder;
    private MockMvc mvc;
    private JsonMapper json;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        json = JsonMapper.builder().findAndAddModules().build();
    }

    @Test
    void registrationAndLoginIssueCustomerTokenWithoutPassword() throws Exception {
        String email = "customer-" + UUID.randomUUID() + "@example.com";
        String register = json.writeValueAsString(new com.example.theatre.dto.RegisterRequest(
                "Test Customer", email, "strong-password-123", null));

        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(register))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("CUSTOMER"))
                .andExpect(jsonPath("$.password").doesNotExist());

        String storedHash = users.findByEmailIgnoreCase(email).orElseThrow().getPassword();
        assertThat(storedHash).isNotEqualTo("strong-password-123");
        assertThat(passwordEncoder.matches("strong-password-123", storedHash)).isTrue();

        String login = json.writeValueAsString(new com.example.theatre.dto.LoginRequest(email, "strong-password-123"));
        String body = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(login))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.user.password").doesNotExist())
                .andReturn().getResponse().getContentAsString();

        JsonNode result = json.readTree(body);
        assertThat(result.get("accessToken").asString()).isNotBlank();
        mvc.perform(get("/api/users").header("Authorization", "Bearer " + result.get("accessToken").asString()))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/movies")
                        .header("Authorization", "Bearer " + result.get("accessToken").asString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void rejectsBadCredentialsWithoutRevealingWhichPartWasWrong() throws Exception {
        String login = json.writeValueAsString(new com.example.theatre.dto.LoginRequest(
                "missing@example.com", "wrong-password"));

        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(login))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }

    @Test
    void publicReadIsAllowedButAdminReadRequiresToken() throws Exception {
        mvc.perform(get("/api/movies")).andExpect(status().isOk());
        mvc.perform(get("/api/users"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
        mvc.perform(get("/api/users").header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/users").header("Authorization", "Bearer " + adminToken()))
                .andExpect(status().isOk());
    }

    @Test
    void frontendLoadsWithoutLogin() throws Exception {
        mvc.perform(get("/"))
                .andExpect(status().isOk());
        mvc.perform(get("/app.css"))
                .andExpect(status().isOk());
        mvc.perform(get("/app.js"))
                .andExpect(status().isOk());
    }

    @Test
    void onlyAdminsCanInspectAnotherUsersPayment() throws Exception {
        mvc.perform(get("/api/payments/admin/999999"))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/payments/admin/999999")
                        .header("Authorization", "Bearer " + token("CUSTOMER")))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/payments/admin/999999")
                        .header("Authorization", "Bearer " + adminToken()))
                .andExpect(status().isNotFound());
    }

    private String adminToken() {
        return token("ADMIN");
    }

    private String token(String role) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("theatre-management-system")
                .subject("1")
                .issuedAt(now)
                .expiresAt(now.plusSeconds(300))
                .claim("roles", List.of(role))
                .build();
        return encoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
    }
}
