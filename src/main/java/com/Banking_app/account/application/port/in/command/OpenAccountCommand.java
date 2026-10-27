package com.Banking_app.account.application.port.in.command;

import com.Banking_app.account.domain.enums.AccountType;

public record OpenAccountCommand(String userId, AccountType accountType) {
    public OpenAccountCommand {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("userId is required");
        }
        if (accountType == null) {
            throw new IllegalArgumentException("accountType is required");
        }
    }
}
