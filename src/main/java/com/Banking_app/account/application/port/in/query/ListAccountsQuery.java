package com.Banking_app.account.application.port.in.query;

import com.Banking_app.account.domain.enums.AccountStatus;

import java.util.Optional;

/** List all accounts, optionally filtered by status (null = no filter). */
public record ListAccountsQuery(AccountStatus status) {

    public static ListAccountsQuery all() {
        return new ListAccountsQuery(null);
    }

    public static ListAccountsQuery byStatus(AccountStatus status) {
        return new ListAccountsQuery(status);
    }

    public Optional<AccountStatus> statusFilter() {
        return Optional.ofNullable(status);
    }
}
