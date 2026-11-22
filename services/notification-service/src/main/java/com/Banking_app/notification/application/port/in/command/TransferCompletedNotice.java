package com.Banking_app.notification.application.port.in.command;

import com.Banking_app.notification.domain.models.ForeignAmount;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * A transfer has happened (from a TRANSFER_COMPLETED message). The notification module's own copy of the fields it
 * needs: it never uses the transaction module's classes, only the message format.
 *
 * @param amount   MYR
 * @param original the amount in a foreign currency, or null for a MYR transfer
 */
public record TransferCompletedNotice(UUID eventId,
                                      Instant occurredAt,
                                      String reference,
                                      String fromAccountNumber,
                                      String fromOwnerId,
                                      String toAccountNumber,
                                      String toOwnerId,
                                      BigDecimal amount,
                                      ForeignAmount original) {
}
