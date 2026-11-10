package com.Banking_app.fx.application.port.in.command;

import java.math.BigDecimal;

/**
 * "Fix today's rate for paying this amount": the requester locks the bank's sell rate for a short time.
 *
 * @param requesterUserId the logged-in user; only they can use the quote
 * @param currencyCode    ISO 4217 code, e.g. "USD" (the bank must offer it)
 * @param amount          in that currency
 */
public record LockQuoteCommand(String requesterUserId, String currencyCode, BigDecimal amount) {
}
