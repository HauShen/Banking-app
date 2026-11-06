package com.Banking_app.transaction.application.port.in.command;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Record a top-up that the account module has already applied to the balance.
 *
 * @param balanceAfter the account's balance after the deposit, written into the CREDIT ledger entry
 * @param depositedAt  when the top-up happened (the account module's clock); used for created_at, success_at and
 *                     the ledger entry, exactly as the old LegacyCashDepositAdapter did
 */
public record RecordCashDepositCommand(Long toAccountId,
                                       BigDecimal amount,
                                       BigDecimal balanceAfter,
                                       Instant depositedAt,
                                       String description,
                                       String idempotencyKey) {
}
