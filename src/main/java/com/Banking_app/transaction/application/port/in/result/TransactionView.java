package com.Banking_app.transaction.application.port.in.result;

import com.Banking_app.transaction.domain.enums.TransactionStatus;
import com.Banking_app.transaction.domain.enums.TransactionType;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * A transaction as the outside world sees it: account numbers instead of internal ids.
 *
 * @param fromAccountNumber null for a cash deposit (the web adapter shows "Cash Deposit Machine")
 * @param currency          the from-account's currency, or the to-account's for a cash deposit (as before)
 */
public record TransactionView(String referenceNumber,
                              TransactionType type,
                              String fromAccountNumber,
                              String toAccountNumber,
                              BigDecimal amount,
                              String currency,
                              TransactionStatus status,
                              String description,
                              Instant createdAt,
                              Instant successAt) {
}
