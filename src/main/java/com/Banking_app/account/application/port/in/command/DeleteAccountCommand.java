package com.Banking_app.account.application.port.in.command;

import java.util.Objects;

public record DeleteAccountCommand(Long accountId) {
    public DeleteAccountCommand {
        Objects.requireNonNull(accountId, "accountId is required");
    }
}
