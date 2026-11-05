package com.Banking_app.account.application.port.in.usecase;

import com.Banking_app.account.application.port.in.result.BalancesAfter;
import com.Banking_app.account.application.port.in.result.TransferAccounts;

import java.math.BigDecimal;

/**
 * INTERNAL use case, not exposed over HTTP: lets the transaction module move money between two accounts
 * while this module stays in charge of balances and their rules (Account.withdraw / deposit).
 *
 * How the transfer service uses it, all inside ONE database transaction:
 * <pre>
 *   lockPairForTransfer(from, to)   // row locks held until the caller's transaction ends
 *   ... caller checks ownership, idempotency, locked status ...
 *   move(fromId, toId, amount)      // withdraw + deposit + save, returns the new balances
 * </pre>
 * Both methods require a running transaction (Propagation.MANDATORY): without one the locks would be
 * released straight away, so calling them outside a transaction fails instead of silently racing.
 *
 * No authorization here: the caller decides who may move money (only the from-account's owner).
 */
public interface MoveFundsUseCase {

    /**
     * Locks both accounts (SELECT ... FOR UPDATE), always the lower account number first, so A→B and B→A at the
     * same moment can't deadlock.
     *
     * @throws com.Banking_app.account.application.exception.TransferAccountNotFoundException 404,
     *         "From account not found" / "To account not found" (whichever is looked up first)
     */
    TransferAccounts lockPairForTransfer(String fromAccountNumber, String toAccountNumber);

    /**
     * Takes the amount from one account and adds it to the other, then saves both.
     * Call {@link #lockPairForTransfer} first in the same transaction.
     *
     * @throws com.Banking_app.account.domain.exception.InsufficientBalanceException 400, balance would go below 20.00
     */
    BalancesAfter move(Long fromAccountId, Long toAccountId, BigDecimal amount);
}
