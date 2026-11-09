package com.Banking_app.fx.application.service;

import com.Banking_app.fx.application.exception.UnsupportedCurrencyException;
import com.Banking_app.fx.application.port.in.result.ExchangeRateView;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Use-case test with fake rates (no Spring). */
class GetExchangeRateServiceTest {

    private final FakeExchangeRateSource source = new FakeExchangeRateSource()
            .with("USD", "4.60", "4.75")
            .with("JPY", "0.0290", "0.0305")
            .with("EUR", "4.90", "5.10");
    private final GetExchangeRateService service = new GetExchangeRateService(source);

    @Test
    @DisplayName("getRate returns the buy and sell rate of an offered currency")
    void getRate() {
        ExchangeRateView usd = service.getRate("USD");

        assertThat(usd.currency()).isEqualTo("USD");
        assertThat(usd.buyRate()).isEqualByComparingTo("4.60");
        assertThat(usd.sellRate()).isEqualByComparingTo("4.75");
        assertThat(usd.asOf()).isEqualTo(FakeExchangeRateSource.AS_OF);
    }

    @Test
    @DisplayName("a known currency without a rate, an unknown code, lower case, blank or null → 'not supported'")
    void notSupported() {
        assertThatThrownBy(() -> service.getRate("SGD"))
                .isInstanceOf(UnsupportedCurrencyException.class)
                .isInstanceOf(IllegalArgumentException.class)   // → 400
                .hasMessage("Currency SGD is not supported");
        assertThatThrownBy(() -> service.getRate("XYZ")).hasMessage("Currency XYZ is not supported");
        assertThatThrownBy(() -> service.getRate("usd")).hasMessage("Currency usd is not supported");
        assertThatThrownBy(() -> service.getRate(" ")).isInstanceOf(UnsupportedCurrencyException.class);
        assertThatThrownBy(() -> service.getRate(null)).isInstanceOf(UnsupportedCurrencyException.class);
    }

    @Test
    @DisplayName("MYR → 'MYR needs no conversion'")
    void myr() {
        assertThatThrownBy(() -> service.getRate("MYR"))
                .isInstanceOf(UnsupportedCurrencyException.class)
                .hasMessage("MYR needs no conversion");
    }

    @Test
    @DisplayName("listRates returns every offered currency, sorted by code")
    void listRates() {
        List<ExchangeRateView> rates = service.listRates();

        assertThat(rates.stream().map(ExchangeRateView::currency).toList()).containsExactly("EUR", "JPY", "USD");
    }

    @Test
    @DisplayName("listRates with no rates configured is an empty list")
    void noRates() {
        assertThat(new GetExchangeRateService(new FakeExchangeRateSource()).listRates()).isEmpty();
    }
}
