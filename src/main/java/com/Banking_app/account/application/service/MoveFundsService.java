package com.Banking_app.account.application.service;

import com.Banking_app.account.application.exception.TransferAccountNotFoundException;
import com.Banking_app.account.application.port.in.result.AccountSnapshot;
import com.Banking_app.account.application.port.in.result.BalancesAfter;
import com.Banking_app.account.application.port.in.result.TransferAccounts;
import com.Banking_app.account.application.port.in.usecase.MoveFundsUseCase;
import com.Banking_app.account.application.port.out.LoadAccountPort;
import com.Banking_app.account.application.port.out.SaveAccountPort;
import com.Banking_app.account.domain.models.account.Account;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.function.Supplier;

@Service
public class MoveFundsService implements MoveFundsUseCase {

    private final LoadAccountPort loadAccountPort;
    private final SaveAccountPort saveAccountPort;

    public MoveFundsService(LoadAccountPort loadAccountPort, SaveAccountPort saveAccountPort) {
        this.loadAccountPort = loadAccountPort;
        this.saveAccountPort = saveAccountPort;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public TransferAccounts lockPairForTransfer(String fromAccountNumber, String toAccountNumber) {
        if (fromAccountNumber.equals(toAccountNumber)) {
            // The transfer service checks this first; repeated here so the lock order below is always defined.
            throw new IllegalArgumentException("Cannot transfer to the same account");
        }

        // Same order as the old TransactionServiceImpl: lower account number first.
        Account from;
        Account to;
        if (fromAccountNumber.compareTo(toAccountNumber) < 0) {
            from = lock(fromAccountNumber, TransferAccountNotFoundException::from);
            to = lock(toAccountNumber, TransferAccountNotFoundException::to);
        } else {
            to = lock(toAccountNumber, TransferAccountNotFoundException::to);
            from = lock(fromAccountNumber, TransferAccountNotFoundException::from);
        }
        return new TransferAccounts(AccountSnapshot.of(from), AccountSnapshot.of(to));
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public BalancesAfter move(Long fromAccountId, Long toAccountId, BigDecimal amount) {
        // Both rows are already locked by lockPairForTransfer in this transaction, so these
        // SELECT ... FOR UPDATE return at once; they just give us the domain objects to change.
        Account from = loadAccountPort.loadForUpdate(fromAccountId).orElseThrow(TransferAccountNotFoundException::from);
        Account to = loadAccountPort.loadForUpdate(toAccountId).orElseThrow(TransferAccountNotFoundException::to);

        from.withdraw(amount);   // amount > 0, ACTIVE, minimum balance
        to.deposit(amount);      // amount > 0, ACTIVE

        Account savedFrom = saveAccountPort.save(from);
        Account savedTo = saveAccountPort.save(to);
        return new BalancesAfter(savedFrom.getBalance(), savedTo.getBalance());
    }

    private Account lock(String accountNumber, Supplier<TransferAccountNotFoundException> notFound) {
        return loadAccountPort.loadByAccountNumberForUpdate(accountNumber).orElseThrow(notFound);
    }
}
