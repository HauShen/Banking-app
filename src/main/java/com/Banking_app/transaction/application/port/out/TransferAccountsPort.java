package com.Banking_app.transaction.application.port.out;

import java.math.BigDecimal;

/**
 * The account side of a transfer. Implemented in step 8 by an adapter that calls the account module's
 * MoveFundsUseCase. Must be called inside the transfer's database transaction: the locks last until it commits.
 */
public interface TransferAccountsPort {

    /**
     * Locks both accounts (lower account number first).
     * Unknown account → 404 "From account not found" / "To account not found" (thrown by the account module).
     */
    TransferParties lockPair(String fromAccountNumber, String toAccountNumber);

    /**
     * Moves the money and returns the new balances.
     * Balance would go below 20.00 → 400 "Insufficient balance. Minimum balance of $20.00 must be maintained."
     */
    TransferBalances move(Long fromAccountId, Long toAccountId, BigDecimal amount);
}
