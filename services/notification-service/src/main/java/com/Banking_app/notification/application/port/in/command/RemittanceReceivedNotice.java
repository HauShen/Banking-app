package com.Banking_app.notification.application.port.in.command;

import com.Banking_app.notification.domain.models.ForeignAmount;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Money from abroad has been credited (from a FOREIGN_REMITTANCE_RECEIVED message). {@code amount} is MYR. */
public record RemittanceReceivedNotice(UUID eventId,
                                       Instant occurredAt,
                                       String reference,
                                       String accountNumber,
                                       String ownerId,
                                       BigDecimal amount,
                                       ForeignAmount original) {
}
