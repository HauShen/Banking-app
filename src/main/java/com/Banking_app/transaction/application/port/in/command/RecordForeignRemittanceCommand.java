package com.Banking_app.transaction.application.port.in.command;

import java.math.BigDecimal;

/**
 * Money from abroad arrives for one of the bank's MYR accounts (simulates the bank's payment gateway; admin only).
 *
 * @param currency       the currency it was sent in, e.g. "USD" (not MYR: that's a top-up)
 * @param amount         in that currency
 * @param senderName     who sent it, shown in the recipient's history
 * @param idempotencyKey the gateway's id for this payment: the same key again returns the original remittance
 */
public record RecordForeignRemittanceCommand(String toAccountNumber,
                                             String currency,
                                             BigDecimal amount,
                                             String senderName,
                                             String idempotencyKey) {
}
