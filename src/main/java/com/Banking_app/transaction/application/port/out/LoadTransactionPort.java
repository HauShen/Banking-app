package com.Banking_app.transaction.application.port.out;

import com.Banking_app.shared.PageResult;

import java.util.Optional;

/** Read side of the transactions table. */
public interface LoadTransactionPort {

    Optional<TransactionSummary> findByReferenceNumber(String referenceNumber);

    Optional<TransactionSummary> findByIdempotencyKey(String idempotencyKey);

    /**
     * Transactions the account sent or received, newest first.
     * Invalid page/size (negative page, size < 1) → IllegalArgumentException (400), as before.
     */
    PageResult<TransactionSummary> findPageInvolvingAccount(Long accountId, int page, int size);

    boolean existsInvolvingAccount(Long accountId);
}
