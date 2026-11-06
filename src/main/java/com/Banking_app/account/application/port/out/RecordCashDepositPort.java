package com.Banking_app.account.application.port.out;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Records a Cash Deposit Machine top-up as a transaction + CREDIT ledger entry.
 * Implemented by adapter/out/ledger/TransactionModuleCashDepositAdapter (calls the transaction module).
 */
public interface RecordCashDepositPort {
    void recordCashDeposit(Long accountId, BigDecimal amount, BigDecimal balanceAfter, Instant depositedAt);
}
