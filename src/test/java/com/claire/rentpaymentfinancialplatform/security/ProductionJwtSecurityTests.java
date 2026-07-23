package com.claire.rentpaymentfinancialplatform.security;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest(classes = {
        SecurityConfiguration.class,
        AuthenticatedUserProvider.class,
        ProductionJwtSecurityTests.TestBeans.class,
        ProductionJwtSecurityTests.TestSecurityController.class
})
@AutoConfigureMockMvc
@ActiveProfiles("prod")
class ProductionJwtSecurityTests {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JwtDecoder jwtDecoder;

    @Test
    void mapsJwtSubjectRenterIdAndRolesToApplicationUser() throws Exception {
        when(jwtDecoder.decode("renter-token")).thenReturn(jwt(
                "renter-token",
                Map.of("sub", "jwt-user", "renter_id", "renter-123", "roles", List.of("RENTER"))
        ));

        mockMvc.perform(get("/test/renter-scope")
                        .header("Authorization", "Bearer renter-token"))
                .andExpect(status().isOk())
                .andExpect(content().string("renter-123"));
    }

    @Test
    void supportsCamelCaseRenterIdAndScopeRoles() throws Exception {
        when(jwtDecoder.decode("scope-token")).thenReturn(jwt(
                "scope-token",
                Map.of("sub", "scope-user", "renterId", "renter-456", "scope", "RENTER SUPPORT")
        ));

        mockMvc.perform(get("/test/renter-scope")
                        .header("Authorization", "Bearer scope-token"))
                .andExpect(status().isOk())
                .andExpect(content().string("renter-456"));

        mockMvc.perform(get("/test/ops")
                        .header("Authorization", "Bearer scope-token"))
                .andExpect(status().isOk())
                .andExpect(content().string("ops"));
    }

    @Test
    void enforcesExistingRoleRulesForJwtUsers() throws Exception {
        when(jwtDecoder.decode("renter-token")).thenReturn(jwt(
                "renter-token",
                Map.of("sub", "jwt-user", "renter_id", "renter-123", "roles", List.of("RENTER"))
        ));
        when(jwtDecoder.decode("finops-token")).thenReturn(jwt(
                "finops-token",
                Map.of("sub", "finops-user", "roles", List.of("FINOPS"))
        ));

        mockMvc.perform(get("/test/ops")
                        .header("Authorization", "Bearer renter-token"))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/test/ops")
                        .header("Authorization", "Bearer finops-token"))
                .andExpect(status().isOk());
    }

    @Test
    void rejectsDevBearerTokensInProductionProfile() throws Exception {
        when(jwtDecoder.decode("dev:test-user:renter-123:RENTER"))
                .thenThrow(new JwtException("Unsupported token"));

        mockMvc.perform(get("/test/renter-scope")
                        .header("Authorization", "Bearer dev:test-user:renter-123:RENTER"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void providerWebhookPathRemainsOutsideJwtAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/provider-webhooks/mock-provider"))
                .andExpect(status().isNotFound());
    }

    private static Jwt jwt(String tokenValue, Map<String, Object> claims) {
        return new Jwt(
                tokenValue,
                Instant.parse("2026-01-01T00:00:00Z"),
                Instant.parse("2026-01-01T01:00:00Z"),
                Map.of("alg", "none"),
                claims
        );
    }

    @RestController
    static class TestSecurityController {

        private final AuthenticatedUserProvider authenticatedUserProvider;

        TestSecurityController(AuthenticatedUserProvider authenticatedUserProvider) {
            this.authenticatedUserProvider = authenticatedUserProvider;
        }

        @GetMapping("/test/renter-scope")
        @PreAuthorize("hasRole('RENTER')")
        String renterScope() {
            return authenticatedUserProvider.currentRenterId();
        }

        @GetMapping("/test/ops")
        @PreAuthorize("hasAnyRole('SUPPORT', 'FINOPS', 'ADMIN')")
        String operations() {
            return "ops";
        }
    }

    @TestConfiguration
    static class TestBeans {

        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper().findAndRegisterModules();
        }
    }
}
