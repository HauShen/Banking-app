package com.Banking_app.account.application.service;

import com.Banking_app.account.application.port.in.result.AccountSnapshot;
import com.Banking_app.account.application.port.in.usecase.LookupAccountsUseCase;
import com.Banking_app.account.application.port.out.LoadAccountPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Service
public class LookupAccountsService implements LookupAccountsUseCase {

    private final LoadAccountPort loadAccountPort;

    public LookupAccountsService(LoadAccountPort loadAccountPort) {
        this.loadAccountPort = loadAccountPort;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AccountSnapshot> findByAccountNumber(String accountNumber) {
        return loadAccountPort.findByAccountNumber(accountNumber).map(AccountSnapshot::of);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AccountSnapshot> findByIds(Collection<Long> accountIds) {
        if (accountIds.isEmpty()) {
            return List.of();   // skip the query for an empty page
        }
        return loadAccountPort.findAllByIds(accountIds).stream().map(AccountSnapshot::of).toList();
    }
}
