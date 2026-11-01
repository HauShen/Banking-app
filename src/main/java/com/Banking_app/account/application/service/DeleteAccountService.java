package com.Banking_app.account.application.service;

import com.Banking_app.account.application.exception.AccountHasTransactionsException;
import com.Banking_app.account.application.exception.AccountNotFoundException;
import com.Banking_app.account.application.port.in.command.DeleteAccountCommand;
import com.Banking_app.account.application.port.in.usecase.DeleteAccountUseCase;
import com.Banking_app.account.application.port.out.AccountHistoryPort;
import com.Banking_app.account.application.port.out.DeleteAccountPort;
import com.Banking_app.account.application.port.out.LoadAccountPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class DeleteAccountService implements DeleteAccountUseCase {

    private final LoadAccountPort loadAccountPort;
    private final DeleteAccountPort deleteAccountPort;
    private final AccountHistoryPort accountHistoryPort;

    public DeleteAccountService(LoadAccountPort loadAccountPort, DeleteAccountPort deleteAccountPort,
                                AccountHistoryPort accountHistoryPort) {
        this.loadAccountPort = loadAccountPort;
        this.deleteAccountPort = deleteAccountPort;
        this.accountHistoryPort = accountHistoryPort;
    }

    @Override
    public void delete(DeleteAccountCommand command) {
        Long accountId = command.accountId();
        // Lock the row: a top-up or transfer (which lock it too) can't add a transaction
        // between the history check and the delete.
        if (loadAccountPort.loadForUpdate(accountId).isEmpty()) {
            throw new AccountNotFoundException("Account not found with id: " + accountId);
        }
        if (accountHistoryPort.hasTransactions(accountId)) {
            throw new AccountHasTransactionsException(accountId);
        }
        deleteAccountPort.deleteById(accountId);
    }
}
