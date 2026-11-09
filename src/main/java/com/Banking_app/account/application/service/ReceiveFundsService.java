package com.Banking_app.account.application.service;

import com.Banking_app.account.application.exception.TransferAccountNotFoundException;
import com.Banking_app.account.application.port.in.result.AccountSnapshot;
import com.Banking_app.account.application.port.in.usecase.ReceiveFundsUseCase;
import com.Banking_app.account.application.port.out.LoadAccountPort;
import com.Banking_app.account.application.port.out.SaveAccountPort;
import com.Banking_app.account.domain.models.account.Account;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class ReceiveFundsService implements ReceiveFundsUseCase {

    private final LoadAccountPort loadAccountPort;
    private final SaveAccountPort saveAccountPort;

    public ReceiveFundsService(LoadAccountPort loadAccountPort, SaveAccountPort saveAccountPort) {
        this.loadAccountPort = loadAccountPort;
        this.saveAccountPort = saveAccountPort;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public AccountSnapshot lockForReceiving(String accountNumber) {
        Account account = loadAccountPort.loadByAccountNumberForUpdate(accountNumber)
                .orElseThrow(TransferAccountNotFoundException::to);
        return AccountSnapshot.of(account);
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public BigDecimal receive(Long accountId, BigDecimal amount) {
        // Already locked by lockForReceiving in this transaction, so this SELECT ... FOR UPDATE returns at once.
        Account account = loadAccountPort.loadForUpdate(accountId).orElseThrow(TransferAccountNotFoundException::to);

        account.receive(amount);   // amount > 0, at most 2 decimals, ACTIVE

        return saveAccountPort.save(account).getBalance();
    }
}
