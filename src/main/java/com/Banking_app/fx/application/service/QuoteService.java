package com.Banking_app.fx.application.service;

import com.Banking_app.fx.application.port.in.query.QuoteQuery;
import com.Banking_app.fx.application.port.in.result.Quote;
import com.Banking_app.fx.application.port.in.usecase.QuoteUseCase;
import com.Banking_app.fx.application.port.out.ExchangeRateSourcePort;
import com.Banking_app.fx.domain.models.ExchangeRate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class QuoteService implements QuoteUseCase {

    private final ExchangeRateSourcePort source;

    public QuoteService(ExchangeRateSourcePort source) {
        this.source = source;
    }

    @Override
    public Quote quote(QuoteQuery query) {
        ExchangeRate rate = RateLookup.require(source, query.currencyCode());
        BigDecimal myrAmount = rate.toMyr(query.amount(), query.direction());
        return new Quote(rate.currency().getCurrencyCode(), query.amount(), query.direction(),
                rate.rateFor(query.direction()), myrAmount, rate.asOf());
    }
}
