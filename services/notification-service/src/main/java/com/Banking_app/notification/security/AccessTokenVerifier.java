package com.Banking_app.notification.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.List;

/**
 * Checks an access token issued by the main app, on its own: no call to the main app, no users table.
 *
 * <p>Valid = signed with the shared JWT_SECRET (HS256), not expired, and it carries the user id ("uid") and the role
 * ("role", e.g. ROLE_CUSTOMER). The main app puts both in every access token (JwtService there).
 *
 * <p>Trade-off: the main app also checks the token version against its database, so a logout or an admin
 * revocation ends a token at once there. This service can't see that: a token keeps working here until it expires
 * (15 minutes at most). For reading one's own notifications that is acceptable.
 */
@Component
public class AccessTokenVerifier {

    static final String USER_ID_CLAIM = "uid";
    static final String ROLE_CLAIM = "role";

    private final SecretKey key;

    public AccessTokenVerifier(@Value("${security.jwt.secret}") String jwtSecret) {
        this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecret));
    }

    /** @throws JwtException or IllegalArgumentException when the token is not valid */
    public UsernamePasswordAuthenticationToken verify(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(key)   // also rejects any other algorithm than the key's (HMAC)
                .build()
                .parseClaimsJws(token)   // checks the signature and the expiry
                .getBody();
        String userId = claims.get(USER_ID_CLAIM, String.class);
        String role = claims.get(ROLE_CLAIM, String.class);
        if (userId == null || userId.isBlank() || role == null || !role.startsWith("ROLE_")) {
            throw new IllegalArgumentException("Token without user id or role");
        }
        return new UsernamePasswordAuthenticationToken(new AuthenticatedUser(userId, claims.getSubject()), null,
                List.of(new SimpleGrantedAuthority(role)));
    }
}
