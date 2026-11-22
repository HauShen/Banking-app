package com.Banking_app.notification.application.port.in.command;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** A top-up has been credited (from a CASH_DEPOSITED message). */
public record CashDepositedNotice(UUID eventId,
                                  Instant occurredAt,
                                  String reference,
                                  String accountNumber,
                                  String ownerId,
                                  BigDecimal amount) {
}
