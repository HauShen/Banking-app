package com.Banking_app.security;

import com.Banking_app.support.AbstractIntegrationTest;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;

import java.security.Key;
import java.util.Date;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Who are you? Every way a token can be missing or wrong must end in 401 -
 * never a 500, and never a successful request.
 */
class AuthenticationSecurityTest extends AbstractIntegrationTest {

    private static final String PROTECTED = "/api/accounts/summary";

    @Value("${security.jwt.secret}")
    private String jwtSecret;

    @Test
    @DisplayName("A valid token is accepted (sanity check for the tests below)")
    void validToken_isAccepted() {
        String token = register(uniqueName("valid"));

        assertStatus(get(PROTECTED, token), 200);
    }

    @Test
    @DisplayName("No token -> 401")
    void noToken_isRejected() {
        ResponseEntity<String> response = get(PROTECTED, null);

        assertStatus(response, 401);
        assertThat(json(response).get("status").asInt()).isEqualTo(401);
        assertThat(text(response, "message")).isEqualTo("Authentication required");
        assertThat(text(response, "path")).isEqualTo(PROTECTED);
    }

    @Test
    @DisplayName("Garbage token -> 401, not 500")
    void garbageToken_isRejected() {
        assertStatus(get(PROTECTED, "abc"), 401);
    }

    @Test
    @DisplayName("Token with an edited signature -> 401")
    void tamperedSignature_isRejected() {
        String token = register(uniqueName("tamper"));

        assertStatus(get(PROTECTED, tamperSignature(token)), 401);
    }

    @Test
    @DisplayName("Forged admin token signed with another key -> 401")
    void tokenSignedWithAnotherKey_isRejected() {
        Key attackerKey = Keys.secretKeyFor(SignatureAlgorithm.HS256);
        String forged = Jwts.builder()
                .setSubject(ADMIN_USERNAME)
                .claim("role", "ROLE_ADMIN")
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 3_600_000))
                .signWith(attackerKey, SignatureAlgorithm.HS256)
                .compact();

        assertStatus(get("/api/admin/users", forged), 401);
    }

    @Test
    @DisplayName("Expired token (correctly signed) -> 401")
    void expiredToken_isRejected() {
        String username = uniqueName("expired");
        register(username);
        Key realKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecret));
        long now = System.currentTimeMillis();
        String expired = Jwts.builder()
                .setSubject(username)
                .setIssuedAt(new Date(now - 7_200_000))
                .setExpiration(new Date(now - 3_600_000))   // expired an hour ago
                .signWith(realKey, SignatureAlgorithm.HS256)
                .compact();

        assertStatus(get(PROTECTED, expired), 401);
    }

    @Test
    @DisplayName("Valid token of a user who was deleted afterwards -> 401")
    void tokenOfDeletedUser_isRejected() {
        String token = register(uniqueName("deleted"));
        String userId = userIdOf(token);

        assertStatus(delete("/api/admin/users/" + userId, adminToken()), 204);

        assertStatus(get(PROTECTED, token), 401);
    }

    @Test
    @DisplayName("A stale token in the header does not block login")
    void staleTokenInHeader_doesNotBlockLogin() {
        String username = uniqueName("stale");
        register(username);

        ResponseEntity<String> response = post("/api/auth/login", "abc",
                Map.of("username", username, "password", PASSWORD));

        assertStatus(response, 200);
        assertThat(text(response, "accessToken")).isNotBlank();
    }

    @Test
    @DisplayName("Wrong password and unknown user give the same generic 401 (no username probing)")
    void failedLogins_lookTheSame() {
        String username = uniqueName("login");
        register(username);

        ResponseEntity<String> wrongPassword = post("/api/auth/login", null,
                Map.of("username", username, "password", "not-the-password"));
        ResponseEntity<String> unknownUser = post("/api/auth/login", null,
                Map.of("username", uniqueName("nobody"), "password", PASSWORD));

        assertStatus(wrongPassword, 401);
        assertStatus(unknownUser, 401);
        assertThat(text(wrongPassword, "message")).isEqualTo("Invalid credentials");
        assertThat(text(unknownUser, "message")).isEqualTo("Invalid credentials");
    }

    @Test
    @DisplayName("The bootstrap admin from the test properties exists and can log in")
    void bootstrapAdmin_canLogIn() {
        assertThat(adminToken()).isNotBlank();
    }

    /**
     * Changes one character in the middle of the signature (the part after the last dot).
     * Not the last character: in Base64 its low bits can be unused, so a change there may decode
     * to the same bytes and the token would still be valid.
     */
    private static String tamperSignature(String token) {
        int index = token.lastIndexOf('.') + 10;
        char replacement = token.charAt(index) == 'A' ? 'B' : 'A';
        return token.substring(0, index) + replacement + token.substring(index + 1);
    }
}
