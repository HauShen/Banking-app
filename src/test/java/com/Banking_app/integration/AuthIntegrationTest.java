package com.Banking_app.integration;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Authentication and registration")
class AuthIntegrationTest extends AbstractIntegrationTest {

    @Test
    @DisplayName("a registered user can log in and use the returned token")
    void registerThenLoginReturnsUsableToken() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "username", "alice",
                                "fullName", "Alice Tan",
                                "email", "alice@example.com",
                                "password", "password123"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"));

        String token = login("alice", "password123");

        mockMvc.perform(get("/api/auth/me").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("alice"))
                .andExpect(jsonPath("$.email").value("alice@example.com"))
                .andExpect(jsonPath("$.role").value("CUSTOMER"));
    }

    @Test
    @DisplayName("a duplicate username is rejected")
    void duplicateUsernameIsRejected() throws Exception {
        registerCustomer("alice", "alice@example.com", "password123");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "username", "alice",
                                "fullName", "Someone Else",
                                "email", "different@example.com",
                                "password", "password123"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Username already exists"));
    }

    @Test
    @DisplayName("a duplicate email is rejected")
    void duplicateEmailIsRejected() throws Exception {
        registerCustomer("alice", "alice@example.com", "password123");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "username", "bob",
                                "fullName", "Bob Lim",
                                "email", "alice@example.com",
                                "password", "password123"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Email already exists"));
    }

    @Test
    @DisplayName("login with a wrong password does not issue a token")
    void loginWithWrongPasswordFails() throws Exception {
        registerCustomer("alice", "alice@example.com", "password123");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("username", "alice", "password", "wrong-password"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("a protected endpoint rejects an unauthenticated caller")
    void protectedEndpointRejectsAnonymousCaller() throws Exception {
        mockMvc.perform(get("/api/accounts/get_all"))
                .andExpect(result ->
                        assertThat(result.getResponse().getStatus())
                                .as("anonymous access must be refused")
                                .isIn(401, 403));
    }

    @Test
    @DisplayName("a protected endpoint rejects a garbage token")
    void protectedEndpointRejectsInvalidToken() throws Exception {
        mockMvc.perform(get("/api/accounts/get_all").header("Authorization", "Bearer not-a-real-token"))
                .andExpect(result ->
                        assertThat(result.getResponse().getStatus())
                                .as("an unparseable token must be refused")
                                .isGreaterThanOrEqualTo(400));
    }

    @Test
    @DisplayName("a self-registering user cannot choose the ADMIN role")
    void selfRegistrationCannotEscalateToAdmin() throws Exception {
        // /api/auth/register is public. If the request body's `role` were honoured,
        // anyone on the internet could mint themselves an admin account.
        String response = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "username", "mallory",
                                "fullName", "Mallory Attacker",
                                "email", "mallory@example.com",
                                "password", "password123",
                                "role", "ADMIN"))))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String token = readTree(response).get("accessToken").asText();

        JsonNode me = readTree(mockMvc.perform(get("/api/auth/me").header("Authorization", bearer(token)))
                .andReturn()
                .getResponse()
                .getContentAsString());

        assertThat(me.get("role").asText())
                .as("a public registration must always produce a CUSTOMER")
                .isEqualTo("CUSTOMER");
    }
}
