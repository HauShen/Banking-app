package com.Banking_app.transaction.application.service;

import com.Banking_app.shared.PageResult;
import com.Banking_app.shared.Requester;
import com.Banking_app.transaction.application.exception.TransactionAccessDeniedException;
import com.Banking_app.transaction.application.exception.TransactionAccountNotFoundException;
import com.Banking_app.transaction.application.port.in.query.ListAccountTransactionsQuery;
import com.Banking_app.transaction.application.port.in.result.TransactionView;
import com.Banking_app.transaction.application.port.in.usecase.ListAccountTransactionsUseCase;
import com.Banking_app.transaction.application.port.out.AccountDirectoryPort;
import com.Banking_app.transaction.application.port.out.AccountRef;
import com.Banking_app.transaction.application.port.out.LoadTransactionPort;
import com.Banking_app.transaction.application.port.out.TransactionSummary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
public class ListAccountTransactionsService implements ListAccountTransactionsUseCase {

    private final LoadTransactionPort loadTransactionPort;
    private final AccountDirectoryPort accountDirectoryPort;

    public ListAccountTransactionsService(LoadTransactionPort loadTransactionPort,
                                          AccountDirectoryPort accountDirectoryPort) {
        this.loadTransactionPort = loadTransactionPort;
        this.accountDirectoryPort = accountDirectoryPort;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<TransactionView> list(ListAccountTransactionsQuery query, Requester requester) {
        AccountRef account = accountDirectoryPort.findByAccountNumber(query.accountNumber())
                .orElseThrow(() -> new TransactionAccountNotFoundException(query.accountNumber()));

        // Same rule as GetAccountService: the owner or an admin.
        if (!requester.canAccessAccountsOf(account.ownerId())) {
            throw new TransactionAccessDeniedException("You can only view the history of your own accounts");
        }

        PageResult<TransactionSummary> rows =
                loadTransactionPort.findPageInvolvingAccount(account.accountId(), query.page(), query.size());

        // One lookup for every account on the page (the old mapper loaded them one row at a time).
        Map<Long, AccountRef> accounts = accountDirectoryPort.findByIds(TransactionViews.accountIdsOf(rows.content()));
        return rows.map(row -> TransactionViews.toView(row, accounts));
    }
}
