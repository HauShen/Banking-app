package com.Banking_app.fx.application.port.in.usecase;

import com.Banking_app.fx.application.port.in.command.ConsumeQuoteCommand;
import com.Banking_app.fx.application.port.in.result.Quote;

/**
 * Uses up a locked quote and returns its fixed rate and MYR amount, for a transfer paid in a foreign currency.
 * Called by the transaction module (through its CurrencyConversionPort) INSIDE the transfer's database transaction
 * (Propagation.MANDATORY): if the transfer fails afterwards, the quote is unused again and can be retried while it
 * is still valid. The quote row is locked (SELECT … FOR UPDATE), so two transfers racing for one quote get it once.
 */
public interface ConsumeQuoteUseCase {

    /**
     * @return the quote as used: direction PAY, the locked rate and MYR amount, {@code asOf} = when it was locked
     * @throws com.Banking_app.fx.domain.exception.InvalidQuoteException 400: not found (also someone else's), already
     *                                                                   used, expired, different currency or amount
     */
    Quote consume(ConsumeQuoteCommand command);
}
