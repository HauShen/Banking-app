package com.Banking_app.security;

import com.Banking_app.support.AbstractIntegrationTest;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The claims inside an access token. notification-service checks tokens on its own (it has no users table),
 * so it depends on exactly these claims: the user id in "uid" and the role in "role".
 */
class AccessTokenClaimsSecurityTest extends AbstractIntegrationTest {

    @Value("${security.jwt.secret}")
    private String jwtSecret;

    private Claims claimsOf(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecret)))
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    @Test
    @DisplayName("A customer's token holds the username, the user id and the customer role")
    void customerToken_holdsUserIdAndRole() {
        String username = uniqueName("claims");
        String token = register(username);

        Claims claims = claimsOf(token);

        assertThat(claims.getSubject()).isEqualTo(username);
        assertThat(claims.get("uid", String.class)).isEqualTo(userIdOf(token));
        assertThat(claims.get("role", String.class)).isEqualTo("ROLE_CUSTOMER");
    }

    @Test
    @DisplayName("An admin's token holds the admin role")
    void adminToken_holdsAdminRole() {
        String token = adminToken();

        Claims claims = claimsOf(token);

        assertThat(claims.get("uid", String.class)).isEqualTo(userIdOf(token));
        assertThat(claims.get("role", String.class)).isEqualTo("ROLE_ADMIN");
    }
}
