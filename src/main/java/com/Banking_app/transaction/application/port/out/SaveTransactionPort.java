package com.Banking_app.transaction.application.port.out;

import com.Banking_app.transaction.domain.models.transaction.Transaction;

public interface SaveTransactionPort {
    /**
     * Inserts the transaction together with its ledger entries; returns it with the generated ids.
     *
     * @throws DuplicateIdempotencyKeyException if a transaction with the same idempotency key already exists
     */
    Transaction save(Transaction transaction);
}
