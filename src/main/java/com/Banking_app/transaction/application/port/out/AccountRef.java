package com.Banking_app.transaction.application.port.out;

/** What reads need to know about an account: its number for display, owner for access checks, currency. */
public record AccountRef(Long accountId, String accountNumber, String ownerId, String currency) {
}
