package com.Banking_app.account.application.port.in.usecase;

import com.Banking_app.account.application.port.in.result.AccountSnapshot;

import java.math.BigDecimal;

/**
 * INTERNAL use case, not exposed over HTTP: lets the transaction module credit money that arrives from outside the
 * bank (a foreign remittance, already converted to MYR) while this module stays in charge of balances and their rules
 * (Account.receive).
 *
 * How the remittance service uses it, all inside ONE database transaction (same shape as MoveFundsUseCase):
 * <pre>
 *   lockForReceiving(accountNumber)   // row lock held until the caller's transaction ends
 *   ... caller checks idempotency and the locked status ...
 *   receive(accountId, amount)        // Account.receive + save, returns the new balance
 * </pre>
 * Locking first means two identical requests for one account run one after the other, so the second finds the
 * first by its idempotency key. Both methods require a running transaction (Propagation.MANDATORY): without one the
 * lock would be released straight away, so calling them outside a transaction fails instead of silently racing.
 *
 * No authorization here: the caller decides who may credit an account (an admin, for remittances).
 */
public interface ReceiveFundsUseCase {

    /**
     * Locks the account (SELECT ... FOR UPDATE) and returns a read-only copy, so the caller can check it (e.g.
     * locked) before anything changes.
     *
     * @throws com.Banking_app.account.application.exception.TransferAccountNotFoundException 404,
     *         "To account not found" (the receiving side, like a transfer's recipient)
     */
    AccountSnapshot lockForReceiving(String accountNumber);

    /**
     * Adds the MYR amount to the account and saves it. Call {@link #lockForReceiving} first in the same transaction.
     *
     * @return the balance after the credit (the transaction module writes it into the ledger entry)
     * @throws com.Banking_app.account.domain.exception.AccountNotActiveException 400, the account is not ACTIVE
     *         (the caller normally checks the snapshot first and answers with its own message)
     * @throws com.Banking_app.account.domain.exception.InvalidAmountException    400, not positive or > 2 decimals
     */
    BigDecimal receive(Long accountId, BigDecimal amount);
}
