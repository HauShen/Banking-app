package com.Banking_app.transaction.application.service;

import com.Banking_app.transaction.application.port.out.TransactionReferencePort;

/** Predictable reference numbers for tests: TXN-000000000001, CDM-000000000002, RMT-..., (one shared counter). */
class SequentialReferences implements TransactionReferencePort {
    private int next = 1;

    @Override
    public String next() {
        return String.format("TXN-%012d", next++);
    }

    @Override
    public String nextCashDeposit() {
        return String.format("CDM-%012d", next++);
    }

    @Override
    public String nextForeignRemittance() {
        return String.format("RMT-%012d", next++);
    }
}
