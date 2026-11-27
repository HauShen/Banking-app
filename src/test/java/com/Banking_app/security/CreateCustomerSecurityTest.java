package com.Banking_app.security;

import com.Banking_app.support.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** POST /api/admin/users/customers: only an admin creates customers (Phase A, step A1). */
class CreateCustomerSecurityTest extends AbstractIntegrationTest {

    private static final String INITIAL_PASSWORD = "Initial123!";

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    @DisplayName("Admin creates a customer -> 201, no tokens; the customer can log in; audited with the admin as actor")
    void admin_createsCustomer() {
        String username = uniqueName("cust");
        String admin = adminToken();

        ResponseEntity<String> response = post("/api/admin/users/customers", admin, body(username, username));

        assertStatus(response, 201);
        assertThat(text(response, "role")).isEqualTo("CUSTOMER");
        assertThat(text(response, "username")).isEqualTo(username);
        assertThat(json(response).has("accessToken")).as("the admin isn't logged in as the customer").isFalse();
        String customerId = text(response, "id");

        String customer = login(username, INITIAL_PASSWORD);
        assertThat(text(get("/api/users/me", customer), "role")).isEqualTo("CUSTOMER");

        assertThat(jdbc.queryForObject("""
                SELECT COUNT(*) FROM audit_events
                WHERE action = 'CUSTOMER_CREATED' AND target_user_id = ? AND actor_id = ?""",
                Long.class, customerId, userIdOf(admin))).isEqualTo(1L);
    }

    @Test
    @DisplayName("Customer -> 403")
    void customer_gets403() {
        String customer = register(uniqueName("cust"));
        String username = uniqueName("other");

        assertStatus(post("/api/admin/users/customers", customer, body(username, username)), 403);
    }

    @Test
    @DisplayName("Not logged in -> 401")
    void anonymous_gets401() {
        String username = uniqueName("cust");

        assertStatus(post("/api/admin/users/customers", null, body(username, username)), 401);
    }

    @Test
    @DisplayName("Username or email already taken -> 409")
    void duplicate_gets409() {
        String admin = adminToken();
        String username = uniqueName("cust");
        assertStatus(post("/api/admin/users/customers", admin, body(username, username)), 201);

        ResponseEntity<String> sameUsername = post("/api/admin/users/customers", admin,
                body(username, uniqueName("mail")));
        ResponseEntity<String> sameEmail = post("/api/admin/users/customers", admin,
                body(uniqueName("cust"), username));

        assertStatus(sameUsername, 409);
        assertThat(text(sameUsername, "message")).isEqualTo("Username already exists");
        assertStatus(sameEmail, 409);
        assertThat(text(sameEmail, "message")).isEqualTo("Email already exists");
    }

    @Test
    @DisplayName("Invalid body (password too short) -> 400")
    void invalidBody_gets400() {
        String username = uniqueName("cust");

        assertStatus(post("/api/admin/users/customers", adminToken(), Map.of(
                "username", username, "fullName", "Test", "email", username + "@example.com",
                "password", "short")), 400);
    }

    private static Map<String, String> body(String username, String emailName) {
        return Map.of(
                "username", username,
                "fullName", "Test " + username,
                "email", emailName + "@example.com",
                "password", INITIAL_PASSWORD);
    }
}
