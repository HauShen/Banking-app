package com.Banking_app.transaction.application.port.out;

/** What a remittance needs to know about the receiving account. The account module's status enum stays out of here. */
public record ReceivingAccount(Long accountId, String accountNumber, boolean locked) {
}
