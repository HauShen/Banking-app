package com.Banking_app.transaction.domain.enums;

/**
 * What kind of money movement a transaction is. Stored in transactions.transaction_type (V9).
 * Needed because "no from-account" no longer means only a cash deposit: a foreign remittance has none either.
 */
public enum TransactionType {
    /** Between two accounts of this bank (possibly paid in a foreign currency, converted to MYR). */
    TRANSFER,
    /** Top-up at the cash deposit machine: no from-account. */
    CASH_DEPOSIT,
    /** Money from abroad, converted to MYR: no from-account. (Phase 5B.) */
    FOREIGN_REMITTANCE
}
