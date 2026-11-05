package com.Banking_app.transaction.application.port.in.command;

import java.math.BigDecimal;

/**
 * Record a top-up that the account module has already applied to the balance.
 *
 * @param balanceAfter the account's balance after the deposit, written into the CREDIT ledger entry
 */
public record RecordCashDepositCommand(Long toAccountId,
                                       BigDecimal amount,
                                       BigDecimal balanceAfter,
                                       String description,
                                       String idempotencyKey) {
}
