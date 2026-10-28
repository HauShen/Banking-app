package com.Banking_app.account.application.service;

import com.Banking_app.account.application.exception.AccountOwnerNotFoundException;
import com.Banking_app.account.application.port.in.usecase.GetAccountSummaryUseCase;
import com.Banking_app.account.application.port.out.AccountOwnerLookupPort;
import com.Banking_app.account.application.port.out.LoadAccountPort;
import com.Banking_app.account.domain.models.account.Account;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class GetAccountSummaryService implements GetAccountSummaryUseCase {

    private final LoadAccountPort loadAccountPort;
    private final AccountOwnerLookupPort accountOwnerLookupPort;

    public GetAccountSummaryService(LoadAccountPort loadAccountPort, AccountOwnerLookupPort accountOwnerLookupPort) {
        this.loadAccountPort = loadAccountPort;
        this.accountOwnerLookupPort = accountOwnerLookupPort;
    }

    @Override
    public AccountSummary getSummary(String ownerId) {
        if (!accountOwnerLookupPort.ownerExists(ownerId)) {
            throw new AccountOwnerNotFoundException(ownerId);
        }
        List<Account> accounts = loadAccountPort.findAllByOwnerId(ownerId);
        BigDecimal totalBalance = accounts.stream()
                .map(Account::getBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new AccountSummary(totalBalance, accounts);
    }
}
