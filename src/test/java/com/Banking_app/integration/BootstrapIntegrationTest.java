package com.Banking_app.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The first-admin bootstrap flow. The security-relevant property here is that the
 * public bootstrap endpoint closes itself permanently once an admin exists.
 */
@DisplayName("First-admin bootstrap")
class BootstrapIntegrationTest extends AbstractIntegrationTest {

    private static final String PASSWORD = "password123";

    @Test
    @DisplayName("reports that bootstrap is allowed while no admin exists")
    void statusAllowsBootstrapOnEmptySystem() throws Exception {
        mockMvc.perform(get("/api/auth/bootstrap-status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.adminExists").value(false))
                .andExpect(jsonPath("$.allowed").value(true))
                .andExpect(jsonPath("$.userCount").value(0));
    }

    @Test
    @DisplayName("creates the first admin with the ADMIN role")
    void firstAdminIsCreatedWithAdminRole() throws Exception {
        mockMvc.perform(post("/api/auth/bootstrap-admin/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "username", "admin",
                                "fullName", "First Admin",
                                "email", "admin@example.com",
                                "password", PASSWORD))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("admin"))
                .andExpect(jsonPath("$.role").value("ADMIN"));

        String token = login("admin", PASSWORD);

        mockMvc.perform(get("/api/auth/me").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }

    @Test
    @DisplayName("closes itself once an admin exists")
    void secondBootstrapAttemptIsRejected() throws Exception {
        bootstrapAdmin("admin", "admin@example.com", PASSWORD);

        mockMvc.perform(get("/api/auth/bootstrap-status"))
                .andExpect(jsonPath("$.adminExists").value(true))
                .andExpect(jsonPath("$.allowed").value(false));

        mockMvc.perform(post("/api/auth/bootstrap-admin/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "username", "sneaky",
                                "fullName", "Second Admin",
                                "email", "sneaky@example.com",
                                "password", PASSWORD))))
                .andExpect(result ->
                        assertThat(result.getResponse().getStatus())
                                .as("the bootstrap endpoint must not mint a second admin")
                                .isGreaterThanOrEqualTo(400));
    }
}
