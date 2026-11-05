package com.Banking_app.transaction.application.service;

import com.Banking_app.shared.Requester;
import com.Banking_app.transaction.application.exception.TransactionAccessDeniedException;
import com.Banking_app.transaction.application.exception.TransactionNotFoundException;
import com.Banking_app.transaction.application.port.in.result.TransactionView;
import com.Banking_app.transaction.application.port.in.usecase.GetTransactionUseCase;
import com.Banking_app.transaction.application.port.out.AccountDirectoryPort;
import com.Banking_app.transaction.application.port.out.AccountRef;
import com.Banking_app.transaction.application.port.out.LoadTransactionPort;
import com.Banking_app.transaction.application.port.out.TransactionSummary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
public class GetTransactionService implements GetTransactionUseCase {

    private final LoadTransactionPort loadTransactionPort;
    private final AccountDirectoryPort accountDirectoryPort;

    public GetTransactionService(LoadTransactionPort loadTransactionPort, AccountDirectoryPort accountDirectoryPort) {
        this.loadTransactionPort = loadTransactionPort;
        this.accountDirectoryPort = accountDirectoryPort;
    }

    @Override
    @Transactional(readOnly = true)
    public TransactionView getByReference(String referenceNumber, Requester requester) {
        TransactionSummary row = loadTransactionPort.findByReferenceNumber(referenceNumber)
                .orElseThrow(TransactionNotFoundException::new);
        Map<Long, AccountRef> accounts = accountDirectoryPort.findByIds(TransactionViews.accountIdsOf(List.of(row)));

        // The sender and the recipient may both see a transfer; admins see everything.
        // A cash deposit has no sender, so only the recipient side applies there.
        boolean isSender = row.fromAccountId() != null
                && TransactionViews.account(accounts, row.fromAccountId()).ownerId().equals(requester.userId());
        boolean isRecipient = TransactionViews.account(accounts, row.toAccountId()).ownerId().equals(requester.userId());
        if (!requester.admin() && !isSender && !isRecipient) {
            throw new TransactionAccessDeniedException("You can only view your own transactions");
        }

        return TransactionViews.toView(row, accounts);
    }
}
