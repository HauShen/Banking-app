package com.Banking_app.security.token;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/** One row per user whose tokens have ever been revoked (see V7 migration). Written only via the upsert query. */
@Entity
@Table(name = "user_token_versions")
public class TokenVersionJpaEntity {

    @Id
    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "version", nullable = false)
    private int version;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected TokenVersionJpaEntity() {
        // for JPA
    }

    public String getUserId() {
        return userId;
    }

    public int getVersion() {
        return version;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
