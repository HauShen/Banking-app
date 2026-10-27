package com.Banking_app.account.application.port.in.usecase;

import com.Banking_app.account.application.port.in.query.ListAccountsQuery;
import com.Banking_app.account.domain.models.account.Account;

import java.util.List;

public interface ListAccountsUseCase {
    List<Account> list(ListAccountsQuery query);

    /** All accounts of one user. Throws AccountOwnerNotFoundException if the user does not exist. */
    List<Account> listByOwner(String ownerId);
}
