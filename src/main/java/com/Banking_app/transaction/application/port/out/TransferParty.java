package com.Banking_app.transaction.application.port.out;

/** What the transfer needs to know about one account. The account module's status enum stays out of here. */
public record TransferParty(Long accountId, String accountNumber, String ownerId, boolean locked) {
}
