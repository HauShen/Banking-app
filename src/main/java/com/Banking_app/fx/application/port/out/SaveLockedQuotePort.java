package com.Banking_app.fx.application.port.out;

import com.Banking_app.fx.domain.models.LockedQuote;

/** Stores a new locked quote (table fx_quotes). */
public interface SaveLockedQuotePort {

    void save(LockedQuote quote);
}
