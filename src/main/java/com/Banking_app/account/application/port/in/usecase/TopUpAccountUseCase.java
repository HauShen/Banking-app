package com.Banking_app.account.application.port.in.usecase;

import com.Banking_app.account.application.port.in.command.TopUpAccountCommand;
import com.Banking_app.account.domain.models.account.Account;

public interface TopUpAccountUseCase {
    Account topUp(TopUpAccountCommand command);
}
