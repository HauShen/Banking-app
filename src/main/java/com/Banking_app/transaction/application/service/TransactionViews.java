package com.Banking_app.transaction.application.service;

import com.Banking_app.transaction.application.port.in.result.TransactionView;
import com.Banking_app.transaction.application.port.out.AccountRef;
import com.Banking_app.transaction.application.port.out.TransactionSummary;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** Turns stored rows into views, for GetTransactionService and ListAccountTransactionsService. */
final class TransactionViews {

    private TransactionViews() {
    }

    /** Every account id the rows mention (from-account ids of cash deposits are null and skipped). */
    static Set<Long> accountIdsOf(Iterable<TransactionSummary> rows) {
        Stream.Builder<Long> ids = Stream.builder();
        for (TransactionSummary row : rows) {
            if (row.fromAccountId() != null) {
                ids.add(row.fromAccountId());
            }
            ids.add(row.toAccountId());
        }
        return ids.build().collect(Collectors.toSet());
    }

    static TransactionView toView(TransactionSummary row, Map<Long, AccountRef> accounts) {
        AccountRef from = row.fromAccountId() == null ? null : account(accounts, row.fromAccountId());
        AccountRef to = account(accounts, row.toAccountId());
        return new TransactionView(
                row.referenceNumber(),
                from == null ? null : from.accountNumber(),
                to.accountNumber(),
                row.amount(),
                from != null ? from.currency() : to.currency(),
                row.status(),
                row.description(),
                row.createdAt(),
                row.successAt());
    }

    static AccountRef account(Map<Long, AccountRef> accounts, Long accountId) {
        AccountRef account = accounts.get(accountId);
        if (account == null) {
            // Can't happen while the foreign keys exist (accounts with transactions can't be deleted).
            throw new IllegalStateException("Account " + accountId + " of a stored transaction was not found");
        }
        return account;
    }
}
