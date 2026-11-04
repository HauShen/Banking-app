package com.Banking_app.security.refresh;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "refresh_tokens")
public class RefreshTokenJpaEntity {

    @Id
    @Column(name = "id", nullable = false)
    private String id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "family_id", nullable = false)
    private String familyId;

    /** SHA-256 (hex) of the raw token. */
    @Column(name = "token_hash", nullable = false, unique = true)
    private String tokenHash;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    /** Set when the token was used (rotated), on logout, or when its family was revoked. */
    @Column(name = "revoked_at")
    private Instant revokedAt;

    /** Id of the token that replaced this one in a normal rotation. */
    @Column(name = "replaced_by")
    private String replacedBy;

    protected RefreshTokenJpaEntity() {
        // for JPA
    }

    public RefreshTokenJpaEntity(String id, String userId, String familyId, String tokenHash,
                                 Instant createdAt, Instant expiresAt) {
        this.id = id;
        this.userId = userId;
        this.familyId = familyId;
        this.tokenHash = tokenHash;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
    }

    void markReplaced(Instant now, String replacementId) {
        this.revokedAt = now;
        this.replacedBy = replacementId;
    }

    public String getId() { return id; }
    public String getUserId() { return userId; }
    public String getFamilyId() { return familyId; }
    public String getTokenHash() { return tokenHash; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getExpiresAt() { return expiresAt; }
    public Instant getRevokedAt() { return revokedAt; }
    public String getReplacedBy() { return replacedBy; }
}
