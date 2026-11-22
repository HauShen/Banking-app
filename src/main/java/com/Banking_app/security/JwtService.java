package com.Banking_app.security;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import com.Banking_app.userProfile.adapter.out.security.SecurityUserDetails;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
@Service
public class JwtService {

    /** Claim holding the user's token version at issue time (see TokenVersionService). */
    static final String TOKEN_VERSION_CLAIM = "ver";

    /**
     * Claim holding the user's id. The main app doesn't need it (it loads the user by username on every
     * request), but services that check the token on their own, like notification-service, can't look
     * the user up and get the id from here.
     */
    static final String USER_ID_CLAIM = "uid";

    @Value("${security.jwt.secret}")
    private String jwtSecret;

    @Value("${security.jwt.expiration}")
    private long jwtExpiration;

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /** @param tokenVersion the user's current token version; the token is valid only while it stays the same */
    public String generateToken(SecurityUserDetails userDetails, int tokenVersion) {
        Map<String, Object> claims = new HashMap<>();
        claims.put(USER_ID_CLAIM, userDetails.getUserId());
        claims.put("role", userDetails.getAuthorities().stream().findFirst().map(Object::toString).orElse("CUSTOMER"));
        claims.put(TOKEN_VERSION_CLAIM, tokenVersion);
        return generateToken(claims, userDetails);
    }

    public String generateToken(Map<String, Object> extraClaims, UserDetails userDetails) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + jwtExpiration);

        return Jwts.builder()
                .setClaims(extraClaims)
                .setSubject(userDetails.getUsername())
                .setIssuedAt(now)
                .setExpiration(expiry)
                .signWith(getSignInKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Valid = signature OK (checked while parsing), same user, not expired, and issued with the user's
     * current token version (a logout or an admin revocation increases it).
     */
    public boolean isTokenValid(String token, UserDetails userDetails, int currentTokenVersion) {
        String username = extractUsername(token);
        return username.equals(userDetails.getUsername())
                && !isTokenExpired(token)
                && extractTokenVersion(token) == currentTokenVersion;
    }

    /** Tokens issued before versions existed have no claim and count as version 0. */
    int extractTokenVersion(String token) {
        Integer version = extractClaim(token, claims -> claims.get(TOKEN_VERSION_CLAIM, Integer.class));
        return version == null ? 0 : version;
    }

    public long getExpirationMs() {
        return jwtExpiration;
    }

    private boolean isTokenExpired(String token) {
        Date expiration = extractClaim(token, Claims::getExpiration);
        return expiration.before(new Date());
    }

    private <T> T extractClaim(String token, Function<Claims, T> resolver) {
        Claims claims = extractAllClaims(token);
        return resolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSignInKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private SecretKey getSignInKey() {
        byte[] keyBytes = Decoders.BASE64.decode(jwtSecret);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
