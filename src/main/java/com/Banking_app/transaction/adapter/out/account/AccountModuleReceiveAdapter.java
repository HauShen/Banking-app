package com.Banking_app.transaction.adapter.out.account;

import com.Banking_app.account.application.port.in.result.AccountSnapshot;
import com.Banking_app.account.application.port.in.usecase.ReceiveFundsUseCase;
import com.Banking_app.transaction.application.port.out.ReceivingAccount;
import com.Banking_app.transaction.application.port.out.ReceivingAccountPort;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Outbound adapter: the transaction module's ReceivingAccountPort, implemented by calling the account module's
 * inbound ReceiveFundsUseCase (never its repositories). Exceptions pass through unchanged: unknown account → 404.
 */
@Component
public class AccountModuleReceiveAdapter implements ReceivingAccountPort {

    private final ReceiveFundsUseCase receiveFundsUseCase;

    public AccountModuleReceiveAdapter(ReceiveFundsUseCase receiveFundsUseCase) {
        this.receiveFundsUseCase = receiveFundsUseCase;
    }

    @Override
    public ReceivingAccount lockForReceiving(String accountNumber) {
        AccountSnapshot account = receiveFundsUseCase.lockForReceiving(accountNumber);
        return new ReceivingAccount(account.id(), account.accountNumber(), account.isLocked());
    }

    @Override
    public BigDecimal receive(Long accountId, BigDecimal amount) {
        return receiveFundsUseCase.receive(accountId, amount);
    }
}
