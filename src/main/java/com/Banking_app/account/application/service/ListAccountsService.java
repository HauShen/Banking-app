package com.Banking_app.account.application.service;

import com.Banking_app.account.application.exception.AccountOwnerNotFoundException;
import com.Banking_app.account.application.port.in.query.ListAccountsQuery;
import com.Banking_app.account.application.port.in.usecase.ListAccountsUseCase;
import com.Banking_app.account.application.port.out.AccountOwnerLookupPort;
import com.Banking_app.account.application.port.out.LoadAccountPort;
import com.Banking_app.account.domain.models.account.Account;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// @Service is added in step 8, once every port has an adapter (otherwise Spring would fail to start).
@Transactional(readOnly = true)
public class ListAccountsService implements ListAccountsUseCase {

    private final LoadAccountPort loadAccountPort;
    private final AccountOwnerLookupPort accountOwnerLookupPort;

    public ListAccountsService(LoadAccountPort loadAccountPort, AccountOwnerLookupPort accountOwnerLookupPort) {
        this.loadAccountPort = loadAccountPort;
        this.accountOwnerLookupPort = accountOwnerLookupPort;
    }

    @Override
    public List<Account> list(ListAccountsQuery query) {
        return query.statusFilter()
                .map(loadAccountPort::findAllByStatus)
                .orElseGet(loadAccountPort::findAll);
    }

    @Override
    public List<Account> listByOwner(String ownerId) {
        if (!accountOwnerLookupPort.ownerExists(ownerId)) {
            throw new AccountOwnerNotFoundException(ownerId);
        }
        return loadAccountPort.findAllByOwnerId(ownerId);
    }
}
