package com.Banking_app.account.application.service;

import com.Banking_app.account.application.port.out.LoadAccountPort;
import com.Banking_app.account.application.port.out.SaveAccountPort;
import com.Banking_app.account.domain.enums.AccountCurrency;
import com.Banking_app.account.domain.enums.AccountStatus;
import com.Banking_app.account.domain.enums.AccountType;
import com.Banking_app.account.domain.models.account.Account;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Fake persistence for use-case tests: keeps accounts in a map instead of PostgreSQL.
 * Like a real database it hands out copies, so changing a loaded Account does nothing until save() is called.
 * It also records every "FOR UPDATE" load, so tests can check the lock order.
 */
class InMemoryAccountStore implements LoadAccountPort, SaveAccountPort {

    private final Map<Long, Account> rows = new LinkedHashMap<>();
    final List<String> lockedAccountNumbers = new ArrayList<>();
    int saveCount;

    Account add(Long id, String ownerId, String accountNumber, AccountStatus status, String balance) {
        Account account = Account.rehydrate(id, ownerId, accountNumber, AccountType.values()[0], status,
                AccountCurrency.MYR, new BigDecimal(balance), Instant.parse("2026-11-05T10:00:00Z"));
        rows.put(id, account);
        return copy(account);
    }

    BigDecimal balanceOf(Long id) {
        return rows.get(id).getBalance();
    }

    private static Account copy(Account a) {
        return Account.rehydrate(a.getId(), a.getOwnerId(), a.getAccountNumber(), a.getType(), a.getStatus(),
                a.getCurrency(), a.getBalance(), a.getCreatedAt());
    }

    private Optional<Account> byNumber(String accountNumber) {
        return rows.values().stream().filter(a -> a.getAccountNumber().equals(accountNumber)).findFirst()
                .map(InMemoryAccountStore::copy);
    }

    @Override
    public Optional<Account> findById(Long accountId) {
        return Optional.ofNullable(rows.get(accountId)).map(InMemoryAccountStore::copy);
    }

    @Override
    public Optional<Account> loadForUpdate(Long accountId) {
        Optional<Account> account = findById(accountId);
        account.ifPresent(a -> lockedAccountNumbers.add(a.getAccountNumber()));
        return account;
    }

    @Override
    public Optional<Account> findByAccountNumber(String accountNumber) {
        return byNumber(accountNumber);
    }

    @Override
    public Optional<Account> loadByAccountNumberForUpdate(String accountNumber) {
        lockedAccountNumbers.add(accountNumber);
        return byNumber(accountNumber);
    }

    @Override
    public List<Account> findAllByIds(Collection<Long> accountIds) {
        return accountIds.stream().filter(rows::containsKey).map(rows::get).map(InMemoryAccountStore::copy).toList();
    }

    @Override
    public List<Account> findAll() {
        return rows.values().stream().map(InMemoryAccountStore::copy).toList();
    }

    @Override
    public List<Account> findAllByStatus(AccountStatus status) {
        return rows.values().stream().filter(a -> a.getStatus() == status).map(InMemoryAccountStore::copy).toList();
    }

    @Override
    public List<Account> findAllByOwnerId(String ownerId) {
        return rows.values().stream().filter(a -> a.isOwnedBy(ownerId)).map(InMemoryAccountStore::copy).toList();
    }

    @Override
    public boolean existsByAccountNumber(String accountNumber) {
        return byNumber(accountNumber).isPresent();
    }

    @Override
    public Account save(Account account) {
        saveCount++;
        rows.put(account.getId(), copy(account));
        return copy(account);
    }
}
