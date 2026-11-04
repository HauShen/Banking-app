package com.Banking_app.security;

import com.Banking_app.support.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Refresh tokens: an HttpOnly cookie that can be exchanged - once - for a new access token and a new cookie.
 * The test client has no cookie jar, so the tests read Set-Cookie and send the Cookie header themselves.
 */
class RefreshTokenSecurityTest extends AbstractIntegrationTest {

    private static final String PROTECTED = "/api/accounts/summary";
    private static final String COOKIE = "refresh_token";

    private ResponseEntity<String> loginResponse(String username) {
        ResponseEntity<String> response = post("/api/auth/login", null,
                Map.of("username", username, "password", PASSWORD));
        assertStatus(response, 200);
        return response;
    }

    private static String setCookieHeader(ResponseEntity<String> response) {
        String header = response.getHeaders().getFirst(HttpHeaders.SET_COOKIE);
        assertThat(header).as("Set-Cookie header").isNotNull().startsWith(COOKIE + "=");
        return header;
    }

    /** The raw refresh token from "refresh_token=<value>; Path=...". */
    private static String refreshTokenFrom(ResponseEntity<String> response) {
        String header = setCookieHeader(response);
        return header.substring(COOKIE.length() + 1, header.indexOf(';'));
    }

    private ResponseEntity<String> refresh(String refreshToken) {
        return http.post().uri("/api/auth/refresh")
                .headers(h -> {
                    if (refreshToken != null) {
                        h.add(HttpHeaders.COOKIE, COOKIE + "=" + refreshToken);
                    }
                })
                .retrieve().toEntity(String.class);
    }

    @Test
    @DisplayName("Login sets a hardened refresh cookie: HttpOnly, Secure, SameSite=Strict, Path=/api/auth, 7 days")
    void login_setsHardenedCookie() {
        String username = uniqueName("cookie");
        register(username);

        String header = setCookieHeader(loginResponse(username));

        assertThat(header).contains("HttpOnly", "Secure", "SameSite=Strict", "Path=/api/auth", "Max-Age=604800");
    }

    @Test
    @DisplayName("Register also starts a session with a refresh cookie")
    void register_setsCookie() {
        String username = uniqueName("newbie");
        ResponseEntity<String> response = post("/api/auth/register", null, Map.of(
                "username", username, "fullName", "New User",
                "email", username + "@example.com", "password", PASSWORD));

        assertStatus(response, 201);
        assertThat(refreshTokenFrom(response)).isNotBlank();
    }

    @Test
    @DisplayName("Refresh (no access token needed) returns a working access token and a new refresh cookie")
    void refresh_rotates() {
        String username = uniqueName("rotate");
        register(username);
        String first = refreshTokenFrom(loginResponse(username));

        ResponseEntity<String> refreshed = refresh(first);

        assertStatus(refreshed, 200);
        assertStatus(get(PROTECTED, text(refreshed, "accessToken")), 200);
        assertThat(refreshTokenFrom(refreshed)).isNotEqualTo(first);
    }

    @Test
    @DisplayName("Reusing a used refresh token revokes the whole family and the current access token")
    void reuse_revokesFamily() {
        String username = uniqueName("stolen");
        register(username);
        String first = refreshTokenFrom(loginResponse(username));
        ResponseEntity<String> refreshed = refresh(first);
        String second = refreshTokenFrom(refreshed);
        String accessToken = text(refreshed, "accessToken");

        assertStatus(refresh(first), 401);                // used again -> someone has a copy
        assertStatus(refresh(second), 401);               // the newest token of that family is dead too
        assertStatus(get(PROTECTED, accessToken), 401);   // and the access token issued with it
        loginResponse(username);                          // the real user can simply log in again
    }

    @Test
    @DisplayName("Reuse on one device does not kill the refresh token of another login")
    void reuse_keepsOtherFamilies() {
        String username = uniqueName("twodev");
        register(username);
        String phone = refreshTokenFrom(loginResponse(username));
        String laptop = refreshTokenFrom(loginResponse(username));
        assertStatus(refresh(laptop), 200);

        assertStatus(refresh(laptop), 401);   // reuse on the laptop family

        assertStatus(refresh(phone), 200);    // the phone gets a new access token by refreshing
    }

    @Test
    @DisplayName("Missing or garbage refresh cookie -> generic 401")
    void invalidCookie_isRejected() {
        ResponseEntity<String> missing = refresh(null);
        ResponseEntity<String> garbage = refresh("not-a-real-token");

        assertStatus(missing, 401);
        assertStatus(garbage, 401);
        assertThat(text(missing, "message")).isEqualTo("Invalid or expired refresh token");
        assertThat(text(garbage, "message")).isEqualTo("Invalid or expired refresh token");
    }

    @Test
    @DisplayName("Logout revokes the refresh token and tells the browser to delete the cookie")
    void logout_revokesRefreshToken() {
        String username = uniqueName("bye");
        register(username);
        ResponseEntity<String> login = loginResponse(username);
        String refreshToken = refreshTokenFrom(login);

        ResponseEntity<String> logout = post("/api/auth/logout", text(login, "accessToken"), Map.of());

        assertStatus(logout, 204);
        assertThat(setCookieHeader(logout)).startsWith(COOKIE + "=;").contains("Max-Age=0");
        assertStatus(refresh(refreshToken), 401);
    }

    @Test
    @DisplayName("Admin revoke-tokens also kills the user's refresh tokens")
    void adminRevoke_killsRefreshTokens() {
        String username = uniqueName("revoked");
        String userId = userIdOf(register(username));
        String refreshToken = refreshTokenFrom(loginResponse(username));

        assertStatus(post("/api/admin/users/" + userId + "/revoke-tokens", adminToken(), Map.of()), 204);

        assertStatus(refresh(refreshToken), 401);
    }

    @Test
    @DisplayName("A deleted user's refresh token is rejected")
    void deletedUser_isRejected() {
        String username = uniqueName("gone");
        String userId = userIdOf(register(username));
        String refreshToken = refreshTokenFrom(loginResponse(username));

        assertStatus(delete("/api/admin/users/" + userId, adminToken()), 204);

        assertStatus(refresh(refreshToken), 401);
    }
}
