package com.Banking_app.account.application.port.in.usecase;

import com.Banking_app.account.application.port.in.command.DeleteAccountCommand;

public interface DeleteAccountUseCase {
    void delete(DeleteAccountCommand command);
}
