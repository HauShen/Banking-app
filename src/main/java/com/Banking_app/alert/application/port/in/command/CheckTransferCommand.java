package com.Banking_app.alert.application.port.in.command;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * A transfer has happened (from a TRANSFER_COMPLETED message). The alert module's own copy of the fields it needs; it
 * never uses the transaction module's classes. {@code amount} is MYR.
 */
public record CheckTransferCommand(UUID eventId,
                                   Instant occurredAt,
                                   String reference,
                                   String fromAccountNumber,
                                   String fromOwnerId,
                                   String toAccountNumber,
                                   BigDecimal amount) {
}
