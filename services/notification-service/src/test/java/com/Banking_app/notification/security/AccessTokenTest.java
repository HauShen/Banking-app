package com.Banking_app.notification.security;

import com.Banking_app.notification.support.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.util.Base64;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** This service checks the main app's access tokens on its own: what it accepts and what it answers 401 to. */
class AccessTokenTest extends AbstractIntegrationTest {

    private static final String UNREAD = "/api/notifications/unread-count";

    @Test
    @DisplayName("a valid customer or admin token → 200")
    void validToken() {
        assertStatus(get(UNREAD, newUser().token()), 200);
        assertStatus(get(UNREAD, token("8d8c3a52-0000-4000-8000-000000000001", "ROLE_ADMIN", 60_000)), 200);
    }

    @Test
    @DisplayName("no token, garbage, an expired token → 401 with the JSON error body")
    void missingOrExpired() {
        ResponseEntity<String> none = get(UNREAD, null);
        assertStatus(none, 401);
        assertThat(text(none, "message")).isEqualTo("Authentication required");
        assertThat(text(none, "path")).isEqualTo(UNREAD);

        assertStatus(get(UNREAD, "not-a-jwt"), 401);
        assertStatus(get(UNREAD, token("8d8c3a52-0000-4000-8000-000000000002", "ROLE_CUSTOMER", -1_000)), 401);
    }

    @Test
    @DisplayName("a token signed with another secret → 401")
    void otherSecret() {
        String otherSecret = Base64.getEncoder().encodeToString("another-secret-of-at-least-32-bytes!!".getBytes());
        String forged = token(Map.of("uid", "8d8c3a52-0000-4000-8000-000000000003", "role", "ROLE_ADMIN"), 60_000,
                otherSecret);

        assertStatus(get(UNREAD, forged), 401);
    }

    @Test
    @DisplayName("a token without the user id or the role (e.g. issued before the uid claim existed) → 401")
    void missingClaims() {
        assertStatus(get(UNREAD, token(Map.of("role", "ROLE_CUSTOMER", "ver", 0), 60_000, jwtSecret())), 401);
        assertStatus(get(UNREAD, token(Map.of("uid", "8d8c3a52-0000-4000-8000-000000000004"), 60_000, jwtSecret())),
                401);
    }

    @Test
    @DisplayName("/actuator/health is public; any other path is refused")
    void otherPaths() {
        assertStatus(get("/actuator/health", null), 200);
        assertStatus(get("/api/accounts/summary", newUser().token()), 403);
        assertStatus(get("/actuator/env", null), 401);
    }
}
