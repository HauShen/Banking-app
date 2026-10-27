package com.Banking_app.account.application.port.in.usecase;

import com.Banking_app.account.application.port.in.command.OpenAccountCommand;
import com.Banking_app.account.domain.models.account.Account;

public interface OpenAccountUseCase {
    Account open(OpenAccountCommand command);
}
