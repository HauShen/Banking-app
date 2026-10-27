package com.Banking_app.account.application.port.in.usecase;

import com.Banking_app.account.application.port.in.command.ChangeAccountStatusCommand;
import com.Banking_app.account.domain.models.account.Account;

public interface ChangeAccountStatusUseCase {
    Account changeStatus(ChangeAccountStatusCommand command);
}
