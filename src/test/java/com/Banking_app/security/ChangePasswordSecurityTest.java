package com.Banking_app.security;

import com.Banking_app.support.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * POST /api/auth/change-password over real HTTP: the signed-in user changes their password, every other session
 * ends, and this one continues with new tokens.
 */
class ChangePasswordSecurityTest extends AbstractIntegrationTest {

    private static final String URL = "/api/auth/change-password";
    private static final String PROTECTED = "/api/users/me";
    private static final String COOKIE = "refresh_token";
    private static final String NEW_PASSWORD = "N3wPassword!";

    @Autowired
    private LoginThrottleProperties throttle;

    @Autowired
    private JdbcTemplate jdbc;

    private ResponseEntity<String> changePassword(String token, String current, String next) {
        return post(URL, token, Map.of("currentPassword", current, "newPassword", next));
    }

    private ResponseEntity<String> loginResponse(String username, String password) {
        return post("/api/auth/login", null, Map.of("username", username, "password", password));
    }

    private static String refreshTokenFrom(ResponseEntity<String> response) {
        String header = response.getHeaders().getFirst(HttpHeaders.SET_COOKIE);
        assertThat(header).as("Set-Cookie header").isNotNull().startsWith(COOKIE + "=");
        return header.substring(COOKIE.length() + 1, header.indexOf(';'));
    }

    private ResponseEntity<String> refresh(String refreshToken) {
        return http.post().uri("/api/auth/refresh")
                .headers(h -> h.add(HttpHeaders.COOKIE, COOKIE + "=" + refreshToken))
                .retrieve().toEntity(String.class);
    }

    @Test
    @DisplayName("200 with a new access token and refresh cookie; the new password logs in, the old one doesn't")
    void changesPassword() {
        String username = uniqueName("pw");
        String token = register(username);

        ResponseEntity<String> response = changePassword(token, PASSWORD, NEW_PASSWORD);

        assertStatus(response, 200);
        assertStatus(get(PROTECTED, text(response, "accessToken")), 200);
        assertThat(refreshTokenFrom(response)).isNotBlank();
        assertStatus(loginResponse(username, NEW_PASSWORD), 200);
        assertStatus(loginResponse(username, PASSWORD), 401);
    }

    @Test
    @DisplayName("every other session ends: old access tokens and refresh cookies stop working")
    void endsOtherSessions() {
        String username = uniqueName("pw");
        register(username);
        ResponseEntity<String> otherDevice = loginResponse(username, PASSWORD);
        String otherAccess = text(otherDevice, "accessToken");
        String otherRefresh = refreshTokenFrom(otherDevice);
        String thisDevice = login(username, PASSWORD);

        assertStatus(changePassword(thisDevice, PASSWORD, NEW_PASSWORD), 200);

        assertStatus(get(PROTECTED, otherAccess), 401);
        assertStatus(get(PROTECTED, thisDevice), 401);   // replaced by the token in the response
        assertStatus(refresh(otherRefresh), 401);
    }

    @Test
    @DisplayName("the change is written to the audit log")
    void audited() {
        String token = register(uniqueName("pw"));
        String userId = userIdOf(token);

        assertStatus(changePassword(token, PASSWORD, NEW_PASSWORD), 200);

        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM audit_events WHERE action = 'PASSWORD_CHANGED' AND target_user_id = ?",
                Long.class, userId)).isEqualTo(1L);
    }

    @Test
    @DisplayName("wrong current password → 400 on currentPassword (not 401), and the password stays")
    void wrongCurrentPassword() {
        String username = uniqueName("pw");
        String token = register(username);

        ResponseEntity<String> response = changePassword(token, "Wrong-Passw0rd", NEW_PASSWORD);

        assertStatus(response, 400);
        assertThat(json(response).get("fieldErrors").get("currentPassword").asText())
                .isEqualTo("Current password is incorrect");
        assertStatus(get(PROTECTED, token), 200);              // still signed in
        assertStatus(loginResponse(username, PASSWORD), 200);   // password unchanged
    }

    @Test
    @DisplayName("new password too short, missing, or the same as the current one → 400 on newPassword")
    void badNewPassword() {
        String token = register(uniqueName("pw"));

        ResponseEntity<String> tooShort = changePassword(token, PASSWORD, "short");
        assertStatus(tooShort, 400);
        assertThat(json(tooShort).get("fieldErrors").get("newPassword").asText())
                .isEqualTo("Password must be 8-100 characters");

        ResponseEntity<String> missing = post(URL, token, Map.of("currentPassword", PASSWORD));
        assertStatus(missing, 400);
        assertThat(json(missing).get("fieldErrors").has("newPassword")).isTrue();

        ResponseEntity<String> same = changePassword(token, PASSWORD, PASSWORD);
        assertStatus(same, 400);
        assertThat(json(same).get("fieldErrors").get("newPassword").asText())
                .isEqualTo("New password must differ from the current one");
    }

    @Test
    @DisplayName("needs a valid access token: none → 401")
    void needsToken() {
        assertStatus(changePassword(null, PASSWORD, NEW_PASSWORD), 401);
    }

    @Test
    @DisplayName("wrong guesses count as failed logins: after the limit, change-password and login both get 429")
    void guessingIsThrottled() {
        String username = uniqueName("pw");
        String token = register(username);

        for (int i = 0; i < throttle.maxFailuresPerUser(); i++) {
            assertStatus(changePassword(token, "Wrong-Passw0rd" + i, NEW_PASSWORD), 400);
        }

        ResponseEntity<String> locked = changePassword(token, PASSWORD, NEW_PASSWORD);
        assertStatus(locked, 429);
        assertThat(locked.getHeaders().getFirst(HttpHeaders.RETRY_AFTER)).isNotBlank();
        assertStatus(loginResponse(username, PASSWORD), 429);
    }
}
