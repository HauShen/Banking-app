package com.Banking_app.alert.domain.models;

import com.Banking_app.alert.domain.enums.AlertRule;
import com.Banking_app.alert.domain.enums.AlertStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * A transfer that an admin should look at, and why. Raised by {@link AlertRules}; an admin marks it as reviewed.
 * Immutable: {@link #review(String, Instant)} returns a copy.
 */
public final class TransactionAlert {

    private final Long id;               // null until saved
    private final UUID eventId;          // the transfer's event
    private final AlertRule rule;
    private final String accountNumber;  // the sending account
    private final String ownerId;
    private final String reference;      // the transfer's reference number
    private final BigDecimal amount;     // MYR, of the transfer that raised it
    private final String detail;         // why, in one sentence
    private final Instant createdAt;     // when the transfer happened
    private final AlertStatus status;
    private final Instant reviewedAt;
    private final String reviewedBy;     // the admin's user id

    private TransactionAlert(Long id, UUID eventId, AlertRule rule, String accountNumber, String ownerId,
                             String reference, BigDecimal amount, String detail, Instant createdAt,
                             AlertStatus status, Instant reviewedAt, String reviewedBy) {
        this.id = id;
        this.eventId = Objects.requireNonNull(eventId, "eventId");
        this.rule = Objects.requireNonNull(rule, "rule");
        this.accountNumber = Objects.requireNonNull(accountNumber, "accountNumber");
        this.ownerId = Objects.requireNonNull(ownerId, "ownerId");
        this.reference = Objects.requireNonNull(reference, "reference");
        this.amount = Objects.requireNonNull(amount, "amount");
        this.detail = Objects.requireNonNull(detail, "detail");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        this.status = Objects.requireNonNull(status, "status");
        this.reviewedAt = reviewedAt;
        this.reviewedBy = reviewedBy;
        if ((status == AlertStatus.REVIEWED) != (reviewedAt != null && reviewedBy != null)) {
            throw new IllegalArgumentException("A reviewed alert needs a review time and reviewer, an open one neither");
        }
    }

    /** A new, open alert about {@code transfer}. */
    static TransactionAlert raise(AlertRule rule, ObservedTransfer transfer, String detail) {
        return new TransactionAlert(null, transfer.eventId(), rule, transfer.fromAccountNumber(),
                transfer.fromOwnerId(), transfer.reference(), transfer.amount(), detail, transfer.occurredAt(),
                AlertStatus.OPEN, null, null);
    }

    public static TransactionAlert rehydrate(Long id, UUID eventId, AlertRule rule, String accountNumber,
                                             String ownerId, String reference, BigDecimal amount, String detail,
                                             Instant createdAt, AlertStatus status, Instant reviewedAt,
                                             String reviewedBy) {
        return new TransactionAlert(id, eventId, rule, accountNumber, ownerId, reference, amount, detail, createdAt,
                status, reviewedAt, reviewedBy);
    }

    /** Marked as reviewed by {@code adminId}. Already reviewed → unchanged (the first review counts). */
    public TransactionAlert review(String adminId, Instant now) {
        if (isReviewed()) {
            return this;
        }
        return new TransactionAlert(id, eventId, rule, accountNumber, ownerId, reference, amount, detail, createdAt,
                AlertStatus.REVIEWED, Objects.requireNonNull(now, "now"), Objects.requireNonNull(adminId, "adminId"));
    }

    public boolean isReviewed() {
        return status == AlertStatus.REVIEWED;
    }

    public Long getId() { return id; }
    public UUID getEventId() { return eventId; }
    public AlertRule getRule() { return rule; }
    public String getAccountNumber() { return accountNumber; }
    public String getOwnerId() { return ownerId; }
    public String getReference() { return reference; }
    public BigDecimal getAmount() { return amount; }
    public String getDetail() { return detail; }
    public Instant getCreatedAt() { return createdAt; }
    public AlertStatus getStatus() { return status; }
    public Instant getReviewedAt() { return reviewedAt; }
    public String getReviewedBy() { return reviewedBy; }
}
