package com.Banking_app.transaction.application.service;

import com.Banking_app.transaction.application.port.out.TransactionReferencePort;

/** Predictable reference numbers for tests: TXN-000000000001, TXN-000000000002, ... */
class SequentialReferences implements TransactionReferencePort {
    private int next = 1;

    @Override
    public String next() {
        return String.format("TXN-%012d", next++);
    }
}
