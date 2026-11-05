package com.Banking_app.transaction.application.port.out;

import com.Banking_app.transaction.domain.enums.TransactionStatus;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * A stored transaction WITHOUT its ledger entries: what every read needs, in one row per transaction.
 * Reads never load the full Transaction aggregate, so a page of history can't trigger one extra query per row.
 *
 * @param fromAccountId null for a cash deposit
 */
public record TransactionSummary(Long id,
                                 String referenceNumber,
                                 Long fromAccountId,
                                 Long toAccountId,
                                 BigDecimal amount,
                                 String description,
                                 TransactionStatus status,
                                 Instant createdAt,
                                 Instant successAt) {

    /** True if the money left this account (never for a cash deposit). */
    public boolean isSentFrom(Long accountId) {
        return fromAccountId != null && fromAccountId.equals(accountId);
    }
}
