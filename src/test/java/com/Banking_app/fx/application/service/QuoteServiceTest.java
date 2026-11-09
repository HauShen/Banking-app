package com.Banking_app.fx.application.service;

import com.Banking_app.fx.application.exception.UnsupportedCurrencyException;
import com.Banking_app.fx.application.port.in.query.QuoteQuery;
import com.Banking_app.fx.application.port.in.result.Quote;
import com.Banking_app.fx.domain.enums.ConversionDirection;
import com.Banking_app.fx.domain.exception.InvalidForeignAmountException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Use-case test with fake rates (no Spring). */
class QuoteServiceTest {

    private final QuoteService service = new QuoteService(new FakeExchangeRateSource()
            .with("USD", "4.60", "4.75")
            .with("JPY", "0.0290", "0.0305"));

    private Quote quote(String currency, String amount, ConversionDirection direction) {
        return service.quote(new QuoteQuery(currency, new BigDecimal(amount), direction));
    }

    @Test
    @DisplayName("PAY 100.00 USD → sell rate 4.75, RM 475.00; the query and the rate's time are echoed")
    void pay() {
        Quote quote = quote("USD", "100.00", ConversionDirection.PAY);

        assertThat(quote.currency()).isEqualTo("USD");
        assertThat(quote.amount()).isEqualByComparingTo("100.00");
        assertThat(quote.direction()).isEqualTo(ConversionDirection.PAY);
        assertThat(quote.exchangeRate()).isEqualByComparingTo("4.75");
        assertThat(quote.myrAmount()).isEqualByComparingTo("475.00");
        assertThat(quote.asOf()).isEqualTo(FakeExchangeRateSource.AS_OF);
    }

    @Test
    @DisplayName("RECEIVE 100.00 USD → buy rate 4.60, RM 460.00")
    void receive() {
        Quote quote = quote("USD", "100.00", ConversionDirection.RECEIVE);

        assertThat(quote.exchangeRate()).isEqualByComparingTo("4.60");
        assertThat(quote.myrAmount()).isEqualByComparingTo("460.00");
    }

    @Test
    @DisplayName("PAY 10000 JPY → RM 305.00")
    void jpy() {
        assertThat(quote("JPY", "10000", ConversionDirection.PAY).myrAmount()).isEqualByComparingTo("305.00");
    }

    @Test
    @DisplayName("currency not offered or MYR → UnsupportedCurrencyException; bad amount → InvalidForeignAmountException")
    void refused() {
        assertThatThrownBy(() -> quote("SGD", "10.00", ConversionDirection.PAY))
                .isInstanceOf(UnsupportedCurrencyException.class)
                .hasMessage("Currency SGD is not supported");
        assertThatThrownBy(() -> quote("MYR", "10.00", ConversionDirection.PAY))
                .hasMessage("MYR needs no conversion");
        assertThatThrownBy(() -> quote("JPY", "1.5", ConversionDirection.PAY))
                .isInstanceOf(InvalidForeignAmountException.class)
                .hasMessage("JPY amounts cannot have decimal places");
        assertThatThrownBy(() -> service.quote(new QuoteQuery("USD", null, ConversionDirection.PAY)))
                .isInstanceOf(InvalidForeignAmountException.class);
    }

    @Test
    @DisplayName("the currency is checked before the amount (an unsupported currency's amount can't be judged)")
    void currencyFirst() {
        assertThatThrownBy(() -> quote("XYZ", "-1", ConversionDirection.PAY))
                .hasMessage("Currency XYZ is not supported");
    }
}
