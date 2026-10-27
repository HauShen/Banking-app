package com.Banking_app.account.application.service;

import com.Banking_app.account.application.exception.AccountAccessDeniedException;
import com.Banking_app.account.application.exception.AccountNotFoundException;
import com.Banking_app.account.application.port.in.command.TopUpAccountCommand;
import com.Banking_app.account.application.port.in.usecase.TopUpAccountUseCase;
import com.Banking_app.account.application.port.out.LoadAccountPort;
import com.Banking_app.account.application.port.out.RecordCashDepositPort;
import com.Banking_app.account.application.port.out.SaveAccountPort;
import com.Banking_app.account.domain.models.account.Account;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

// @Service is added in step 8, once every port has an adapter (otherwise Spring would fail to start).
@Transactional
public class TopUpAccountService implements TopUpAccountUseCase {

    private final LoadAccountPort loadAccountPort;
    private final SaveAccountPort saveAccountPort;
    private final RecordCashDepositPort recordCashDepositPort;
    private final Clock clock;

    public TopUpAccountService(LoadAccountPort loadAccountPort, SaveAccountPort saveAccountPort,
                               RecordCashDepositPort recordCashDepositPort, Clock clock) {
        this.loadAccountPort = loadAccountPort;
        this.saveAccountPort = saveAccountPort;
        this.recordCashDepositPort = recordCashDepositPort;
        this.clock = clock;
    }

    @Override
    public Account topUp(TopUpAccountCommand command) {
        Account account = loadAccountPort.findById(command.accountId())
                .orElseThrow(() -> new AccountNotFoundException("Account not found: " + command.accountId()));

        if (!account.isOwnedBy(command.requestingUserId())) {
            throw new AccountAccessDeniedException("You can only top up your own accounts");
        }

        account.deposit(command.amount());          // domain rule: amount > 0, account ACTIVE
        Account saved = saveAccountPort.save(account);

        recordCashDepositPort.recordCashDeposit(saved.getId(), command.amount(), saved.getBalance(), clock.instant());
        return saved;
    }
}
