package com.Banking_app.account.application.port.out;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Records a Cash Deposit Machine top-up as a transaction + CREDIT ledger entry.
 * Implemented for now by a legacy adapter over the old Transaction/LedgerEntry repositories (step 8).
 */
public interface RecordCashDepositPort {
    void recordCashDeposit(Long accountId, BigDecimal amount, BigDecimal balanceAfter, Instant depositedAt);
}
