package com.Banking_app.transaction.application.port.out;

/** Creates new reference numbers. A port so tests can use predictable ones. */
public interface TransactionReferencePort {

    /** For transfers, e.g. "TXN-3F9A0C12B7D4". */
    String next();

    /** For cash deposits (top-ups), e.g. "CDM-3F9A0C12B7D4" — the prefix the old top-up code used. */
    String nextCashDeposit();
}
