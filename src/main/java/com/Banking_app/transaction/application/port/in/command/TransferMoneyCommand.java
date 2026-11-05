package com.Banking_app.transaction.application.port.in.command;

import java.math.BigDecimal;

/**
 * Send money from one account to another.
 * The request's {@code currency} field is not here: it was never used (see the plan, Phase 4 item 16).
 *
 * @param requesterUserId the logged-in user; must own the from-account
 */
public record TransferMoneyCommand(String requesterUserId,
                                   String fromAccountNumber,
                                   String toAccountNumber,
                                   BigDecimal amount,
                                   String description,
                                   String idempotencyKey) {
}
