package com.Banking_app.account.adapter.out.ledger;

import com.Banking_app.account.application.port.out.AccountHistoryPort;
import com.Banking_app.transaction.application.port.in.usecase.AccountHistoryUseCase;
import org.springframework.stereotype.Component;

/**
 * Outbound adapter: answers AccountHistoryPort through the transaction module's inbound AccountHistoryUseCase
 * (replaces LegacyAccountHistoryAdapter). Used by DeleteAccountService: an account with history → 409.
 */
@Component
public class TransactionModuleHistoryAdapter implements AccountHistoryPort {

    private final AccountHistoryUseCase accountHistoryUseCase;

    public TransactionModuleHistoryAdapter(AccountHistoryUseCase accountHistoryUseCase) {
        this.accountHistoryUseCase = accountHistoryUseCase;
    }

    @Override
    public boolean hasTransactions(Long accountId) {
        return accountHistoryUseCase.hasTransactions(accountId);
    }
}
