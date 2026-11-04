package com.Banking_app.security.token;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

/**
 * Token revocation without a blacklist. Each user has a version number (0 until first revoked);
 * a token is only accepted while its "ver" claim equals the user's current version.
 *
 * Design note: this lives in the security layer with its own table instead of a column on users, so the
 * UserProfile domain stays free of authentication concerns. The cost is one primary-key lookup per request.
 */
@Service
public class TokenVersionService {

    private final TokenVersionJpaRepository repository;
    private final Clock clock;

    public TokenVersionService(TokenVersionJpaRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public int currentVersion(String userId) {
        return repository.findById(userId).map(TokenVersionJpaEntity::getVersion).orElse(0);
    }

    /** Every token issued to this user so far stops working on their next request. */
    @Transactional
    public void revokeAllTokens(String userId) {
        repository.increment(userId, clock.instant());
    }
}
