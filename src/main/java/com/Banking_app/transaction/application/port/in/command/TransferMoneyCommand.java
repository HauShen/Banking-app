package com.Banking_app.transaction.application.port.in.command;

import java.math.BigDecimal;

/**
 * Send money from one account to another. Accounts are MYR only; the customer may state the amount in a foreign
 * currency (e.g. 100.00 USD), and the MYR equivalent at the bank's sell rate moves.
 *
 * @param requesterUserId the logged-in user; must own the from-account
 * @param amount          in {@code currency}
 * @param currency        the currency of {@code amount}; absent means MYR (as before this field was used)
 */
public record TransferMoneyCommand(String requesterUserId,
                                   String fromAccountNumber,
                                   String toAccountNumber,
                                   BigDecimal amount,
                                   String currency,
                                   String description,
                                   String idempotencyKey) {

    public static final String MYR = "MYR";

    public TransferMoneyCommand {
        if (currency == null) {
            currency = MYR;
        }
    }

    /** True when the amount is in another currency and has to be converted to MYR. */
    public boolean isForeignCurrency() {
        return !MYR.equals(currency);
    }
}
