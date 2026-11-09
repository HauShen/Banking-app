package com.Banking_app.fx.application.port.in.usecase;

import com.Banking_app.fx.application.port.in.query.QuoteQuery;
import com.Banking_app.fx.application.port.in.result.Quote;

/** Shows the MYR amount of a foreign amount before the customer confirms (GET /api/fx/quote). */
public interface QuoteUseCase {

    /**
     * @throws com.Banking_app.fx.application.exception.UnsupportedCurrencyException (400) currency not offered, or MYR
     * @throws com.Banking_app.fx.domain.exception.InvalidForeignAmountException     (400) bad amount, or below RM 0.01
     */
    Quote quote(QuoteQuery query);
}
