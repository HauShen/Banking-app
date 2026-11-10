package com.Banking_app.fx.application.port.in.usecase;

import com.Banking_app.fx.application.port.in.command.LockQuoteCommand;
import com.Banking_app.fx.application.port.in.result.LockedQuoteView;

/** Locks the bank's sell rate for an amount, for a short time (POST /api/fx/quotes). */
public interface LockQuoteUseCase {

    /**
     * @throws com.Banking_app.fx.application.exception.UnsupportedCurrencyException (400) currency not offered, or MYR
     * @throws com.Banking_app.fx.domain.exception.InvalidForeignAmountException     (400) bad amount, or below RM 0.01
     */
    LockedQuoteView lock(LockQuoteCommand command);
}
