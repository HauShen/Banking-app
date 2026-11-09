package com.Banking_app.transaction.application.port.in.result;

import com.Banking_app.transaction.domain.enums.TransactionStatus;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Outcome of a transfer request.
 *
 * @param amount    the MYR amount that moved (for a transfer paid in a foreign currency: the converted amount)
 * @param original  the foreign currency, amount and rate; null for a MYR transfer
 * @param duplicate true when the idempotency key was already used by this account: nothing new happened and
 *                  these are the original transaction's values. The web adapter picks the message from it.
 */
public record TransferResult(String transactionReference,
                             TransactionStatus status,
                             Instant createdAt,
                             BigDecimal amount,
                             ForeignAmountView original,
                             boolean duplicate) {
}
