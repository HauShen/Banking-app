package com.Banking_app.transaction.application.port.in.query;

/** The closing balance of each of the last {@code days} days (today included) of one account. */
public record BalanceHistoryQuery(String accountNumber, int days) {
}
