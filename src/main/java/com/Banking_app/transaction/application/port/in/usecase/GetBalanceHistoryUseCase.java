package com.Banking_app.transaction.application.port.in.usecase;

import com.Banking_app.shared.Requester;
import com.Banking_app.transaction.application.port.in.query.BalanceHistoryQuery;
import com.Banking_app.transaction.application.port.in.result.BalanceHistoryView;

public interface GetBalanceHistoryUseCase {

    /** Most days one request can ask for. */
    int MAX_DAYS = 366;

    /**
     * Allowed for the account's owner and admins, like the transaction history.
     * {@code days} outside 1..{@value #MAX_DAYS} → IllegalArgumentException (400).
     */
    BalanceHistoryView get(BalanceHistoryQuery query, Requester requester);
}
