package com.Banking_app.account.application.service;

import com.Banking_app.account.application.exception.AccountNotFoundException;
import com.Banking_app.account.application.port.in.command.DeleteAccountCommand;
import com.Banking_app.account.application.port.in.usecase.DeleteAccountUseCase;
import com.Banking_app.account.application.port.out.DeleteAccountPort;
import com.Banking_app.account.application.port.out.LoadAccountPort;
import org.springframework.transaction.annotation.Transactional;

// @Service is added in step 8, once every port has an adapter (otherwise Spring would fail to start).
@Transactional
public class DeleteAccountService implements DeleteAccountUseCase {

    private final LoadAccountPort loadAccountPort;
    private final DeleteAccountPort deleteAccountPort;

    public DeleteAccountService(LoadAccountPort loadAccountPort, DeleteAccountPort deleteAccountPort) {
        this.loadAccountPort = loadAccountPort;
        this.deleteAccountPort = deleteAccountPort;
    }

    @Override
    public void delete(DeleteAccountCommand command) {
        if (loadAccountPort.findById(command.accountId()).isEmpty()) {
            throw new AccountNotFoundException("Account not found with id: " + command.accountId());
        }
        deleteAccountPort.deleteById(command.accountId());
    }
}
