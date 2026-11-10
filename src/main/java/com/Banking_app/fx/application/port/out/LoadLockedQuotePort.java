package com.Banking_app.fx.application.port.out;

import com.Banking_app.fx.domain.models.LockedQuote;

import java.util.Optional;
import java.util.UUID;

/** Reads a locked quote and locks its row until the caller's transaction ends. */
public interface LoadLockedQuotePort {

    Optional<LockedQuote> loadForUpdate(UUID quoteId);
}
