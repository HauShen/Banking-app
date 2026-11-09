package com.Banking_app.fx.application.service;

import com.Banking_app.fx.application.port.in.result.ExchangeRateView;
import com.Banking_app.fx.application.port.in.usecase.GetExchangeRateUseCase;
import com.Banking_app.fx.application.port.out.ExchangeRateSourcePort;
import com.Banking_app.fx.domain.models.ExchangeRate;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Service
public class GetExchangeRateService implements GetExchangeRateUseCase {

    private final ExchangeRateSourcePort source;

    public GetExchangeRateService(ExchangeRateSourcePort source) {
        this.source = source;
    }

    @Override
    public ExchangeRateView getRate(String currencyCode) {
        return toView(RateLookup.require(source, currencyCode));
    }

    @Override
    public List<ExchangeRateView> listRates() {
        return source.allRates().stream()
                .sorted(Comparator.comparing(rate -> rate.currency().getCurrencyCode()))
                .map(GetExchangeRateService::toView)
                .toList();
    }

    private static ExchangeRateView toView(ExchangeRate rate) {
        return new ExchangeRateView(rate.currency().getCurrencyCode(), rate.buyRate(), rate.sellRate(), rate.asOf());
    }
}
