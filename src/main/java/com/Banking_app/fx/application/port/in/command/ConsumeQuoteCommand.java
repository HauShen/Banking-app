package com.Banking_app.fx.application.port.in.command;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * "Pay this amount at the rate I locked": uses up a locked quote.
 *
 * @param requesterUserId must be the user who locked the quote
 * @param currencyCode    and {@code amount}: the payment, which must be exactly what the quote was locked for
 */
public record ConsumeQuoteCommand(UUID quoteId, String requesterUserId, String currencyCode, BigDecimal amount) {
}
