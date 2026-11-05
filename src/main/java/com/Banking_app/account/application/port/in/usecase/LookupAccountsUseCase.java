package com.Banking_app.account.application.port.in.usecase;

import com.Banking_app.account.application.port.in.result.AccountSnapshot;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * INTERNAL use case, not exposed over HTTP: lets the transaction module find accounts, e.g. to check who owns
 * an account before showing its history, or to show account numbers in a page of transactions.
 *
 * No authorization here (unlike GetAccountUseCase): the caller applies its own rules.
 */
public interface LookupAccountsUseCase {

    Optional<AccountSnapshot> findByAccountNumber(String accountNumber);

    /** One query for a whole page of transactions; unknown ids are skipped, order is not guaranteed. */
    List<AccountSnapshot> findByIds(Collection<Long> accountIds);
}
