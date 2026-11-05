package com.Banking_app.transaction.adapter.out.account;

import com.Banking_app.account.application.port.in.result.AccountSnapshot;
import com.Banking_app.account.application.port.in.result.BalancesAfter;
import com.Banking_app.account.application.port.in.result.TransferAccounts;
import com.Banking_app.account.application.port.in.usecase.MoveFundsUseCase;
import com.Banking_app.transaction.application.port.out.TransferAccountsPort;
import com.Banking_app.transaction.application.port.out.TransferBalances;
import com.Banking_app.transaction.application.port.out.TransferParties;
import com.Banking_app.transaction.application.port.out.TransferParty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Outbound adapter: the transaction module's TransferAccountsPort, implemented by calling the account module's
 * inbound MoveFundsUseCase (never its repositories). Only this class knows both modules' types.
 *
 * Exceptions from the account module pass through unchanged: unknown account → 404, minimum balance → 400.
 */
@Component
public class AccountModuleTransferAdapter implements TransferAccountsPort {

    private final MoveFundsUseCase moveFundsUseCase;

    public AccountModuleTransferAdapter(MoveFundsUseCase moveFundsUseCase) {
        this.moveFundsUseCase = moveFundsUseCase;
    }

    @Override
    public TransferParties lockPair(String fromAccountNumber, String toAccountNumber) {
        TransferAccounts locked = moveFundsUseCase.lockPairForTransfer(fromAccountNumber, toAccountNumber);
        return new TransferParties(toParty(locked.from()), toParty(locked.to()));
    }

    @Override
    public TransferBalances move(Long fromAccountId, Long toAccountId, BigDecimal amount) {
        BalancesAfter after = moveFundsUseCase.move(fromAccountId, toAccountId, amount);
        return new TransferBalances(after.fromBalanceAfter(), after.toBalanceAfter());
    }

    private static TransferParty toParty(AccountSnapshot account) {
        return new TransferParty(account.id(), account.accountNumber(), account.ownerId(), account.isLocked());
    }
}
