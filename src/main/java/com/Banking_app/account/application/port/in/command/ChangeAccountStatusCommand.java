package com.Banking_app.account.application.port.in.command;

import com.Banking_app.account.domain.enums.AccountStatus;

import java.util.Objects;

public record ChangeAccountStatusCommand(Long accountId, AccountStatus status) {
    public ChangeAccountStatusCommand {
        Objects.requireNonNull(accountId, "accountId is required");
        if (status == null) {
            throw new IllegalArgumentException("status is required");
        }
    }
}
