package com.Banking_app.account.application.service;

import com.Banking_app.account.application.exception.AccountNotFoundException;
import com.Banking_app.account.application.port.in.command.ChangeAccountStatusCommand;
import com.Banking_app.account.application.port.in.usecase.ChangeAccountStatusUseCase;
import com.Banking_app.account.application.port.out.LoadAccountPort;
import com.Banking_app.account.application.port.out.SaveAccountPort;
import com.Banking_app.account.domain.models.account.Account;
import org.springframework.transaction.annotation.Transactional;

// @Service is added in step 8, once every port has an adapter (otherwise Spring would fail to start).
@Transactional
public class ChangeAccountStatusService implements ChangeAccountStatusUseCase {

    private final LoadAccountPort loadAccountPort;
    private final SaveAccountPort saveAccountPort;

    public ChangeAccountStatusService(LoadAccountPort loadAccountPort, SaveAccountPort saveAccountPort) {
        this.loadAccountPort = loadAccountPort;
        this.saveAccountPort = saveAccountPort;
    }

    @Override
    public Account changeStatus(ChangeAccountStatusCommand command) {
        Account account = loadAccountPort.findById(command.accountId())
                .orElseThrow(() -> new AccountNotFoundException(
                        "Account not found with account id: " + command.accountId()));
        account.changeStatus(command.status());
        return saveAccountPort.save(account);
    }
}
