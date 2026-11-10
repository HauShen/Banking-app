package com.Banking_app.fx.adapter.out.persistence.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Row of the {@code fx_quotes} table (V10). Persistence only: the rules live in the domain LockedQuote.
 * The owner is a plain user_id column (users belong to another module); the foreign key stays in the database.
 */
@Entity
@Table(name = "fx_quotes")
@Getter
@Setter
@NoArgsConstructor
public class LockedQuoteJpaEntity {

    /** A UUID as text, like the other VARCHAR(36) ids (users, refresh_tokens). */
    @Id
    @Column(name = "id", length = 36)
    private String id;

    @Column(name = "user_id", nullable = false, length = 36)
    private String userId;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "exchange_rate", nullable = false, precision = 19, scale = 8)
    private BigDecimal exchangeRate;

    @Column(name = "myr_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal myrAmount;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "used_at")
    private Instant usedAt;
}
