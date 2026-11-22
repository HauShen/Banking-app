package com.Banking_app.transaction.application.port.out;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * What reads need to know about an account: its number for display, owner for access checks, currency, and for
 * the balance history its current balance and when it was opened.
 */
public record AccountRef(Long accountId, String accountNumber, String ownerId, String currency,
                         BigDecimal balance, Instant openedAt) {
}
