package com.Banking_app.account.application.service;

import com.Banking_app.account.application.exception.AccountNotFoundException;
import com.Banking_app.account.application.port.in.usecase.GetAccountUseCase;
import com.Banking_app.account.application.port.out.LoadAccountPort;
import com.Banking_app.account.domain.models.account.Account;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class GetAccountService implements GetAccountUseCase {

    private final LoadAccountPort loadAccountPort;

    public GetAccountService(LoadAccountPort loadAccountPort) {
        this.loadAccountPort = loadAccountPort;
    }

    @Override
    public Account getById(Long accountId) {
        return loadAccountPort.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException("Account not found with id: " + accountId));
    }

    @Override
    public Account getByAccountNumber(String accountNumber) {
        return loadAccountPort.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException(
                        "Account not found with account number: " + accountNumber));
    }
}
