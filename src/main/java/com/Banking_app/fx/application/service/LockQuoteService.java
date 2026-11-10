package com.Banking_app.fx.application.service;

import com.Banking_app.fx.application.port.in.command.LockQuoteCommand;
import com.Banking_app.fx.application.port.in.result.LockedQuoteView;
import com.Banking_app.fx.application.port.in.usecase.LockQuoteUseCase;
import com.Banking_app.fx.application.port.out.ExchangeRateSourcePort;
import com.Banking_app.fx.application.port.out.SaveLockedQuotePort;
import com.Banking_app.fx.domain.models.ExchangeRate;
import com.Banking_app.fx.domain.models.LockedQuote;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.util.UUID;

/**
 * Locks today's sell rate: same currency and amount checks as a quote (RateLookup, ExchangeRate.toMyr), then the
 * quote is stored with an expiry. How long it stays valid comes from {@code fx.quote-validity} (default 60 s).
 */
@Service
public class LockQuoteService implements LockQuoteUseCase {

    private final ExchangeRateSourcePort source;
    private final SaveLockedQuotePort saveLockedQuotePort;
    private final Clock clock;
    private final Duration validity;

    public LockQuoteService(ExchangeRateSourcePort source,
                            SaveLockedQuotePort saveLockedQuotePort,
                            Clock clock,
                            @Value("${fx.quote-validity:60s}") Duration validity) {
        this.source = source;
        this.saveLockedQuotePort = saveLockedQuotePort;
        this.clock = clock;
        this.validity = validity;
    }

    @Override
    @Transactional
    public LockedQuoteView lock(LockQuoteCommand command) {
        ExchangeRate rate = RateLookup.require(source, command.currencyCode());
        LockedQuote quote = LockedQuote.lock(UUID.randomUUID(), command.requesterUserId(), rate, command.amount(),
                clock.instant(), validity);

        saveLockedQuotePort.save(quote);

        return new LockedQuoteView(quote.getId(), quote.getCurrency().getCurrencyCode(), quote.getAmount(),
                quote.getExchangeRate(), quote.getMyrAmount(), quote.getExpiresAt());
    }
}
