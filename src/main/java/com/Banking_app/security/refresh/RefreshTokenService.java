package com.Banking_app.security.refresh;

import com.Banking_app.security.token.TokenVersionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

/**
 * Rotating refresh tokens with reuse detection.
 *
 * <ul>
 *   <li>Login / register: {@link #issueNewFamily} - a new random 256-bit token, a new family.</li>
 *   <li>Refresh: {@link #rotate} - the presented token is marked used and replaced by a new one (same family).</li>
 *   <li>Reuse: presenting an already-used token means two parties hold it (e.g. stolen). The whole family is
 *       revoked and the user's token version is increased, so the attacker's current access token dies too.</li>
 * </ul>
 */
@Service
public class RefreshTokenService {

    private static final Logger log = LoggerFactory.getLogger(RefreshTokenService.class);
    private static final SecureRandom RANDOM = new SecureRandom();

    private final RefreshTokenJpaRepository repository;
    private final TokenVersionService tokenVersionService;
    private final RefreshTokenProperties properties;
    private final Clock clock;

    public RefreshTokenService(RefreshTokenJpaRepository repository, TokenVersionService tokenVersionService,
                               RefreshTokenProperties properties, Clock clock) {
        this.repository = repository;
        this.tokenVersionService = tokenVersionService;
        this.properties = properties;
        this.clock = clock;
    }

    /** The user the rotated token belongs to, and its replacement. */
    public record Rotation(String userId, IssuedRefreshToken refreshToken) { }

    @Transactional
    public IssuedRefreshToken issueNewFamily(String userId) {
        return create(userId, UUID.randomUUID().toString(), clock.instant()).issued();
    }

    /**
     * noRollbackFor: on reuse detection the family revocation must be committed even though we then throw.
     */
    @Transactional(noRollbackFor = InvalidRefreshTokenException.class)
    public Rotation rotate(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new InvalidRefreshTokenException("Refresh token missing");
        }
        Instant now = clock.instant();
        RefreshTokenJpaEntity current = repository.findByTokenHashForUpdate(sha256(rawToken))
                .orElseThrow(() -> new InvalidRefreshTokenException("Unknown refresh token"));

        if (current.getRevokedAt() != null) {
            if (current.getReplacedBy() != null) {
                log.warn("Refresh token reuse detected for user {} (family {}): revoking the family",
                        current.getUserId(), current.getFamilyId());
                repository.revokeFamily(current.getFamilyId(), now);
                tokenVersionService.revokeAllTokens(current.getUserId());
            }
            throw new InvalidRefreshTokenException("Refresh token revoked");
        }
        if (!now.isBefore(current.getExpiresAt())) {
            throw new InvalidRefreshTokenException("Refresh token expired");
        }

        Created next = create(current.getUserId(), current.getFamilyId(), now);
        current.markReplaced(now, next.entity().getId());   // saved on commit (managed entity)
        return new Rotation(current.getUserId(), next.issued());
    }

    /** Logout / admin revocation: no refresh token of this user can be used any more. */
    @Transactional
    public void revokeAllForUser(String userId) {
        repository.revokeAllForUser(userId, clock.instant());
    }

    private record Created(RefreshTokenJpaEntity entity, IssuedRefreshToken issued) { }

    private Created create(String userId, String familyId, Instant now) {
        String rawToken = newRawToken();
        RefreshTokenJpaEntity entity = new RefreshTokenJpaEntity(UUID.randomUUID().toString(), userId, familyId,
                sha256(rawToken), now, now.plus(properties.lifetime()));
        repository.save(entity);
        return new Created(entity, new IssuedRefreshToken(rawToken, properties.lifetime()));
    }

    /** 32 random bytes (256 bits) from SecureRandom, URL-safe Base64: 43 characters. */
    private static String newRawToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * A plain hash is enough here (unlike passwords): the token is 256 random bits, so it can't be guessed
     * or brute-forced from the hash, and the lookup must be by hash.
     */
    static String sha256(String rawToken) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);   // every JVM must provide it
        }
    }
}
