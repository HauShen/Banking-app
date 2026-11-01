package com.Banking_app.account.application.service;

import com.Banking_app.account.application.exception.AccountNotFoundException;
import com.Banking_app.account.application.port.in.command.ChangeAccountStatusCommand;
import com.Banking_app.account.application.port.in.usecase.ChangeAccountStatusUseCase;
import com.Banking_app.account.application.port.out.LoadAccountPort;
import com.Banking_app.account.application.port.out.SaveAccountPort;
import com.Banking_app.account.domain.models.account.Account;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
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
        // Locked too: save() writes every column, so an unlocked read could put back an old balance
        // if a top-up or transfer committed in between.
        Account account = loadAccountPort.loadForUpdate(command.accountId())
                .orElseThrow(() -> new AccountNotFoundException(
                        "Account not found with account id: " + command.accountId()));
        account.changeStatus(command.status());
        return saveAccountPort.save(account);
    }
}
