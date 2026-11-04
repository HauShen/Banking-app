package com.Banking_app.security;

import com.Banking_app.support.AbstractIntegrationTest;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;

import java.util.Date;
import java.util.Map;
import java.util.UUID;

/**
 * Logout and admin revocation: a token stops working as soon as the user's token version changes,
 * long before it would expire.
 */
class TokenRevocationSecurityTest extends AbstractIntegrationTest {

    private static final String PROTECTED = "/api/accounts/summary";

    @Value("${security.jwt.secret}")
    private String jwtSecret;

    private void logout(String token, int expectedStatus) {
        assertStatus(post("/api/auth/logout", token, Map.of()), expectedStatus);
    }

    @Test
    @DisplayName("After logout the same token gets 401")
    void logout_revokesTheToken() {
        String token = register(uniqueName("logout"));
        assertStatus(get(PROTECTED, token), 200);

        logout(token, 204);

        assertStatus(get(PROTECTED, token), 401);
    }

    @Test
    @DisplayName("Logout revokes the user's tokens on every device")
    void logout_revokesEveryToken() {
        String username = uniqueName("devices");
        String phone = register(username);
        String laptop = login(username, PASSWORD);

        logout(laptop, 204);

        assertStatus(get(PROTECTED, laptop), 401);
        assertStatus(get(PROTECTED, phone), 401);
    }

    @Test
    @DisplayName("Logging in again after logout gives a working token")
    void loginAfterLogout_works() {
        String username = uniqueName("again");
        String oldToken = register(username);
        logout(oldToken, 204);

        String newToken = login(username, PASSWORD);

        assertStatus(get(PROTECTED, newToken), 200);
        assertStatus(get(PROTECTED, oldToken), 401);
    }

    @Test
    @DisplayName("One user's logout does not affect other users")
    void logout_doesNotAffectOtherUsers() {
        String alice = register(uniqueName("alice"));
        String bob = register(uniqueName("bob"));

        logout(alice, 204);

        assertStatus(get(PROTECTED, bob), 200);
    }

    @Test
    @DisplayName("Logout without a token -> 401")
    void logout_withoutToken_isRejected() {
        logout(null, 401);
    }

    @Test
    @DisplayName("A token from before token versions existed (no 'ver' claim) works until the first logout")
    void tokenWithoutVersionClaim_countsAsVersionZero() {
        String username = uniqueName("legacy");
        String current = register(username);
        long now = System.currentTimeMillis();
        String legacy = Jwts.builder()
                .setSubject(username)
                .setIssuedAt(new Date(now))
                .setExpiration(new Date(now + 3_600_000))
                .signWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecret)), SignatureAlgorithm.HS256)
                .compact();
        assertStatus(get(PROTECTED, legacy), 200);

        logout(current, 204);

        assertStatus(get(PROTECTED, legacy), 401);
    }

    @Test
    @DisplayName("An admin can log a user out of every device")
    void admin_canRevokeAUsersTokens() {
        String username = uniqueName("compromised");
        String token = register(username);
        String userId = userIdOf(token);

        assertStatus(post("/api/admin/users/" + userId + "/revoke-tokens", adminToken(), Map.of()), 204);

        assertStatus(get(PROTECTED, token), 401);
        assertStatus(get(PROTECTED, login(username, PASSWORD)), 200);   // the user can simply log in again
    }

    @Test
    @DisplayName("A customer cannot revoke someone else's tokens -> 403")
    void customer_cannotRevokeOthers() {
        String victim = register(uniqueName("victim"));
        String attacker = register(uniqueName("attacker"));

        assertStatus(post("/api/admin/users/" + userIdOf(victim) + "/revoke-tokens", attacker, Map.of()), 403);

        assertStatus(get(PROTECTED, victim), 200);
    }

    @Test
    @DisplayName("Revoking tokens of an unknown user -> 404")
    void revokeUnknownUser_isNotFound() {
        String unknownId = UUID.randomUUID().toString();

        assertStatus(post("/api/admin/users/" + unknownId + "/revoke-tokens", adminToken(), Map.of()), 404);
    }
}
