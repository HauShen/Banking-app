package com.Banking_app.account.application.port.in.command;

import com.Banking_app.account.domain.exception.InvalidAmountException;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * The amount is checked here, before the account is loaded, so an invalid amount
 * gives 400 even for an unknown account id (same order as the old AccountServiceImpl).
 */
public record TopUpAccountCommand(Long accountId, BigDecimal amount, String requestingUserId) {
    public TopUpAccountCommand {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidAmountException("Top-up amount must be greater than zero");
        }
        Objects.requireNonNull(accountId, "accountId is required");
        Objects.requireNonNull(requestingUserId, "requestingUserId is required");
    }
}
