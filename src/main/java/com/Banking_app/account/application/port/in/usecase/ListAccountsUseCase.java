package com.Banking_app.account.application.port.in.usecase;

import com.Banking_app.account.application.port.in.AccountRequester;
import com.Banking_app.account.application.port.in.query.ListAccountsQuery;
import com.Banking_app.account.domain.models.account.Account;

import java.util.List;

public interface ListAccountsUseCase {
    /** Every account in the bank. Admin only (enforced with @PreAuthorize on the endpoint). */
    List<Account> list(ListAccountsQuery query);

    /**
     * All accounts of one user. Owner or admin only.
     * Throws AccountAccessDeniedException, or AccountOwnerNotFoundException if the user does not exist.
     */
    List<Account> listByOwner(String ownerId, AccountRequester requester);
}
