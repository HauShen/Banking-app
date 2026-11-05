package com.Banking_app.transaction.application.port.in.query;

/** One page of an account's history, newest first. */
public record ListAccountTransactionsQuery(String accountNumber, int page, int size) {
}
