package com.Banking_app.account.adapter.out.ledger;

import com.Banking_app.account.application.port.out.AccountHistoryPort;
import com.Banking_app.repositories.TransactionRepository;
import org.springframework.stereotype.Component;

/**
 * Answers AccountHistoryPort from the legacy transactions table.
 * Every ledger row belongs to a transaction that names this account, so checking
 * transactions covers ledgers too.
 * TODO: when the Transaction module goes hexagonal, ask its use case instead.
 */
@Component
public class LegacyAccountHistoryAdapter implements AccountHistoryPort {

    private final TransactionRepository transactionRepository;

    public LegacyAccountHistoryAdapter(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    @Override
    public boolean hasTransactions(Long accountId) {
        return transactionRepository.existsInvolvingAccount(accountId);
    }
}
