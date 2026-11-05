package com.Banking_app.transaction.application.port.in.result;

import com.Banking_app.transaction.domain.enums.TransactionStatus;

import java.time.Instant;

/**
 * Outcome of a transfer request.
 *
 * @param duplicate true when the idempotency key was already used by this account: nothing new happened and
 *                  these are the original transaction's values. The web adapter picks the message from it.
 */
public record TransferResult(String transactionReference,
                             TransactionStatus status,
                             Instant createdAt,
                             boolean duplicate) {
}
