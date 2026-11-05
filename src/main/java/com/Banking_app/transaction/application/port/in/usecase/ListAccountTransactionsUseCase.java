package com.Banking_app.transaction.application.port.in.usecase;

import com.Banking_app.shared.PageResult;
import com.Banking_app.shared.Requester;
import com.Banking_app.transaction.application.port.in.query.ListAccountTransactionsQuery;
import com.Banking_app.transaction.application.port.in.result.TransactionView;

public interface ListAccountTransactionsUseCase {
    /** Allowed for the account's owner and admins. Newest first. */
    PageResult<TransactionView> list(ListAccountTransactionsQuery query, Requester requester);
}
