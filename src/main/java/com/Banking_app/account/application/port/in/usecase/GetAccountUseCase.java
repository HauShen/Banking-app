package com.Banking_app.account.application.port.in.usecase;

import com.Banking_app.account.application.port.in.AccountRequester;
import com.Banking_app.account.domain.models.account.Account;

public interface GetAccountUseCase {
    /** Owner or admin only. Throws AccountNotFoundException / AccountAccessDeniedException. */
    Account getById(Long accountId, AccountRequester requester);

    /** Owner or admin only. Throws AccountNotFoundException / AccountAccessDeniedException. */
    Account getByAccountNumber(String accountNumber, AccountRequester requester);
}
