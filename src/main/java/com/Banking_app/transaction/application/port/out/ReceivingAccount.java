package com.Banking_app.transaction.application.port.out;

/**
 * What a remittance needs to know about the receiving account. The account module's status enum stays out of here.
 * The owner is passed on in the remittance's event, so a consumer (e.g. notifications) never asks the account module.
 */
public record ReceivingAccount(Long accountId, String accountNumber, String ownerId, boolean locked) {
}
