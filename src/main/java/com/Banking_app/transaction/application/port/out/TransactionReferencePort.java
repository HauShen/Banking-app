package com.Banking_app.transaction.application.port.out;

/** Creates new reference numbers, e.g. "TXN-3F9A0C12B7D4". A port so tests can use predictable ones. */
public interface TransactionReferencePort {
    String next();
}
