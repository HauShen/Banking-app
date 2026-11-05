package com.Banking_app.transaction.adapter.out.account;

import com.Banking_app.account.application.port.in.result.AccountSnapshot;
import com.Banking_app.account.application.port.in.usecase.LookupAccountsUseCase;
import com.Banking_app.transaction.application.port.out.AccountDirectoryPort;
import com.Banking_app.transaction.application.port.out.AccountRef;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Outbound adapter: AccountDirectoryPort through the account module's inbound LookupAccountsUseCase. */
@Component
public class AccountModuleDirectoryAdapter implements AccountDirectoryPort {

    private final LookupAccountsUseCase lookupAccountsUseCase;

    public AccountModuleDirectoryAdapter(LookupAccountsUseCase lookupAccountsUseCase) {
        this.lookupAccountsUseCase = lookupAccountsUseCase;
    }

    @Override
    public Optional<AccountRef> findByAccountNumber(String accountNumber) {
        return lookupAccountsUseCase.findByAccountNumber(accountNumber).map(AccountModuleDirectoryAdapter::toRef);
    }

    @Override
    public Map<Long, AccountRef> findByIds(Collection<Long> accountIds) {
        return lookupAccountsUseCase.findByIds(accountIds).stream()
                .map(AccountModuleDirectoryAdapter::toRef)
                .collect(Collectors.toMap(AccountRef::accountId, Function.identity()));
    }

    private static AccountRef toRef(AccountSnapshot account) {
        return new AccountRef(account.id(), account.accountNumber(), account.ownerId(), account.currency().name());
    }
}
