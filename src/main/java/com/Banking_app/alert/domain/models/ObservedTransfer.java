package com.Banking_app.alert.domain.models;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** A transfer as the alert rules see it: who sent how much, when. {@code amount} is MYR. */
public record ObservedTransfer(UUID eventId,
                               Instant occurredAt,
                               String reference,
                               String fromAccountNumber,
                               String fromOwnerId,
                               String toAccountNumber,
                               BigDecimal amount) {

    public ObservedTransfer {
        Objects.requireNonNull(eventId, "eventId");
        Objects.requireNonNull(occurredAt, "occurredAt");
        Objects.requireNonNull(reference, "reference");
        Objects.requireNonNull(fromAccountNumber, "fromAccountNumber");
        Objects.requireNonNull(fromOwnerId, "fromOwnerId");
        Objects.requireNonNull(toAccountNumber, "toAccountNumber");
        Objects.requireNonNull(amount, "amount");
    }
}
