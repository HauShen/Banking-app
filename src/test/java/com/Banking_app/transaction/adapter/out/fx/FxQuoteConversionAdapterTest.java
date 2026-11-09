package com.Banking_app.transaction.adapter.out.fx;

import com.Banking_app.fx.application.exception.UnsupportedCurrencyException;
import com.Banking_app.fx.application.port.in.query.QuoteQuery;
import com.Banking_app.fx.application.port.in.result.Quote;
import com.Banking_app.fx.application.port.in.usecase.QuoteUseCase;
import com.Banking_app.fx.domain.enums.ConversionDirection;
import com.Banking_app.transaction.domain.models.transaction.CurrencyConversion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Currency;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Mapping only: the adapter asks fx for the right kind of quote and turns it into the booked conversion. */
class FxQuoteConversionAdapterTest {

    private QuoteQuery askedFor;

    /** Sell 4.75, buy 4.60 for every currency; "XYZ" is not offered. */
    private final QuoteUseCase fakeFxModule = query -> {
        askedFor = query;
        if (query.currencyCode().equals("XYZ")) {
            throw UnsupportedCurrencyException.notSupported("XYZ");
        }
        BigDecimal rate = new BigDecimal(query.direction() == ConversionDirection.PAY ? "4.75" : "4.60");
        return new Quote(query.currencyCode(), query.amount(), query.direction(), rate,
                query.amount().multiply(rate).setScale(2), Instant.EPOCH);
    };

    private final FxQuoteConversionAdapter adapter = new FxQuoteConversionAdapter(fakeFxModule);

    @Test
    @DisplayName("a payment asks for a PAY quote (sell rate) and keeps currency, amount, rate and MYR amount")
    void payment() {
        CurrencyConversion conversion = adapter.convertPayment("USD", new BigDecimal("100.00"));

        assertThat(askedFor.direction()).isEqualTo(ConversionDirection.PAY);
        assertThat(conversion.originalCurrency()).isEqualTo(Currency.getInstance("USD"));
        assertThat(conversion.originalAmount()).isEqualByComparingTo("100.00");
        assertThat(conversion.exchangeRate()).isEqualByComparingTo("4.75");
        assertThat(conversion.myrAmount()).isEqualByComparingTo("475.00");
    }

    @Test
    @DisplayName("incoming money asks for a RECEIVE quote (buy rate)")
    void incoming() {
        CurrencyConversion conversion = adapter.convertIncoming("USD", new BigDecimal("100.00"));

        assertThat(askedFor.direction()).isEqualTo(ConversionDirection.RECEIVE);
        assertThat(conversion.exchangeRate()).isEqualByComparingTo("4.60");
        assertThat(conversion.myrAmount()).isEqualByComparingTo("460.00");
    }

    @Test
    @DisplayName("fx's refusal (a 400 with the client-facing message) passes through unchanged")
    void refusalPassesThrough() {
        assertThatThrownBy(() -> adapter.convertPayment("XYZ", BigDecimal.ONE))
                .isInstanceOf(UnsupportedCurrencyException.class)
                .hasMessage("Currency XYZ is not supported");
    }
}
