package com.claire.rentpaymentfinancialplatform.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest(classes = {
        SecurityConfiguration.class,
        AuthenticatedUserProvider.class,
        DevBearerTokenAuthenticationFilter.class,
        LocalDevSecurityFlowTests.TestBeans.class,
        LocalDevSecurityFlowTests.TestSecurityController.class
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class LocalDevSecurityFlowTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void keepsDevBearerTokenFlowForLocalDevTestProfiles() throws Exception {
        mockMvc.perform(get("/test/renter-scope")
                        .header("Authorization", "Bearer dev:test-user:renter-123:RENTER"))
                .andExpect(status().isOk())
                .andExpect(content().string("renter-123"));
    }

    @Test
    void stillRejectsWrongDevRoleForProtectedEndpoints() throws Exception {
        mockMvc.perform(get("/test/ops")
                        .header("Authorization", "Bearer dev:test-user:renter-123:RENTER"))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/test/ops")
                        .header("Authorization", "Bearer dev:support-user:-:SUPPORT"))
                .andExpect(status().isOk())
                .andExpect(content().string("ops"));
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
