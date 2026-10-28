package com.Banking_app.account.application.service;

import com.Banking_app.account.application.exception.AccountOwnerNotFoundException;
import com.Banking_app.account.application.port.in.command.OpenAccountCommand;
import com.Banking_app.account.application.port.in.usecase.OpenAccountUseCase;
import com.Banking_app.account.application.port.out.AccountOwnerLookupPort;
import com.Banking_app.account.application.port.out.LoadAccountPort;
import com.Banking_app.account.application.port.out.SaveAccountPort;
import com.Banking_app.account.domain.models.account.Account;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.concurrent.ThreadLocalRandom;

@Service
@Transactional
public class OpenAccountService implements OpenAccountUseCase {

    // 10-digit account numbers: 1000000000 .. 9999999999
    private static final long MIN_ACCOUNT_NUMBER = 1_000_000_000L;
    private static final long MAX_ACCOUNT_NUMBER_EXCLUSIVE = 10_000_000_000L;

    private final LoadAccountPort loadAccountPort;
    private final SaveAccountPort saveAccountPort;
    private final AccountOwnerLookupPort accountOwnerLookupPort;
    private final Clock clock;

    public OpenAccountService(LoadAccountPort loadAccountPort, SaveAccountPort saveAccountPort,
                              AccountOwnerLookupPort accountOwnerLookupPort, Clock clock) {
        this.loadAccountPort = loadAccountPort;
        this.saveAccountPort = saveAccountPort;
        this.accountOwnerLookupPort = accountOwnerLookupPort;
        this.clock = clock;
    }

    @Override
    public Account open(OpenAccountCommand command) {
        if (!accountOwnerLookupPort.ownerExists(command.userId())) {
            throw new AccountOwnerNotFoundException(command.userId());
        }
        Account account = Account.open(command.userId(), generateUniqueAccountNumber(),
                command.accountType(), clock.instant());
        return saveAccountPort.save(account);
    }

    private String generateUniqueAccountNumber() {
        String accountNumber;
        do {
            accountNumber = String.valueOf(
                    ThreadLocalRandom.current().nextLong(MIN_ACCOUNT_NUMBER, MAX_ACCOUNT_NUMBER_EXCLUSIVE));
        } while (loadAccountPort.existsByAccountNumber(accountNumber));
        return accountNumber;
    }
}
