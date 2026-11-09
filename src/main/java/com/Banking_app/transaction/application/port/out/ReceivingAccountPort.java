package com.Banking_app.transaction.application.port.out;

import java.math.BigDecimal;

/**
 * The account side of a remittance, implemented by an adapter that calls the account module's ReceiveFundsUseCase.
 * Must be called inside the remittance's database transaction: the lock lasts until it commits.
 */
public interface ReceivingAccountPort {

    /** Locks the account. Unknown → 404 "To account not found" (thrown by the account module). */
    ReceivingAccount lockForReceiving(String accountNumber);

    /** Credits the MYR amount and returns the balance after. */
    BigDecimal receive(Long accountId, BigDecimal amount);
}
