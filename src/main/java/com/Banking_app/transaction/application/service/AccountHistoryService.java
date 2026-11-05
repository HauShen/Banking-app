package com.Banking_app.transaction.application.service;

import com.Banking_app.transaction.application.port.in.usecase.AccountHistoryUseCase;
import com.Banking_app.transaction.application.port.out.LoadTransactionPort;
import org.springframework.transaction.annotation.Transactional;

// @Service is added in step 8.
public class AccountHistoryService implements AccountHistoryUseCase {

    private final LoadTransactionPort loadTransactionPort;

    public AccountHistoryService(LoadTransactionPort loadTransactionPort) {
        this.loadTransactionPort = loadTransactionPort;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasTransactions(Long accountId) {
        return loadTransactionPort.existsInvolvingAccount(accountId);
    }
}
