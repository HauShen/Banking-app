package com.Banking_app.transaction.application.port.in.result;

import com.Banking_app.transaction.domain.enums.TransactionStatus;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Outcome of a remittance.
 *
 * @param amount    the MYR amount credited (converted at the bank's buy rate)
 * @param original  the foreign currency, amount and rate
 * @param duplicate true when the idempotency key was already used for this remittance: nothing new happened and
 *                  these are the original values
 */
public record RemittanceResult(String transactionReference,
                               TransactionStatus status,
                               Instant createdAt,
                               String toAccountNumber,
                               BigDecimal amount,
                               ForeignAmountView original,
                               boolean duplicate) {
}
