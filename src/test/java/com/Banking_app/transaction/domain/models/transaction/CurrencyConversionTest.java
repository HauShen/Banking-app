package com.Banking_app.transaction.domain.models.transaction;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Currency;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The booked conversion is a plain record; the conversion rules are tested in the fx module (ExchangeRateTest,
 * QuoteServiceTest) and the hand-over in FxQuoteConversionAdapterTest.
 */
class CurrencyConversionTest {

    private static final Currency USD = Currency.getInstance("USD");
    private static final Currency JPY = Currency.getInstance("JPY");

    private static CurrencyConversion conversion(Currency currency, String amount, String rate, String myr) {
        return new CurrencyConversion(currency, new BigDecimal(amount), new BigDecimal(rate), new BigDecimal(myr));
    }

    @Test
    @DisplayName("keeps the original currency, amount, rate and the MYR amount as given")
    void keepsValues() {
        CurrencyConversion c = conversion(USD, "100.00", "4.75", "475.00");

        assertThat(c.originalCurrency()).isEqualTo(USD);
        assertThat(c.originalAmount()).isEqualByComparingTo("100.00");
        assertThat(c.exchangeRate()).isEqualByComparingTo("4.75");
        assertThat(c.myrAmount()).isEqualByComparingTo("475.00");
    }

    @Test
    @DisplayName("as read back from the database: JPY amount with 2 decimals, rate with 8")
    void asStored() {
        CurrencyConversion c = conversion(JPY, "10000.00", "0.03050000", "305.00");

        assertThat(c.originalAmount()).isEqualByComparingTo("10000");
        assertThat(c.exchangeRate()).isEqualByComparingTo("0.0305");
    }

    @Test
    @DisplayName("refuses missing values, MYR, and zero or negative amounts or rate")
    void refusesNonsense() {
        assertThatThrownBy(() -> new CurrencyConversion(null, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new CurrencyConversion(USD, BigDecimal.ONE, null, BigDecimal.ONE))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> conversion(Currency.getInstance("MYR"), "10.00", "1", "10.00"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("A currency conversion needs a foreign currency, not MYR");
        for (String[] values : new String[][]{{"0", "4.75", "1.00"}, {"1.00", "-4.75", "1.00"}, {"1.00", "4.75", "0"}}) {
            assertThatThrownBy(() -> conversion(USD, values[0], values[1], values[2]))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("A currency conversion needs positive amounts and rate");
        }
    }

    @Test
    @DisplayName("refuses values the transactions table can't store as they are (no silent rounding)")
    void refusesUnstorable() {
        for (String[] values : new String[][]{
                {"100.001", "4.75", "475.00"}, {"100.00", "4.750000001", "475.00"}, {"100.00", "4.75", "475.001"}}) {
            assertThatThrownBy(() -> conversion(USD, values[0], values[1], values[2]))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("A currency conversion stores amounts with at most 2 decimals and the rate with at most 8");
        }
    }

    @Test
    @DisplayName("a value object: equal when all parts are equal")
    void equality() {
        assertThat(conversion(USD, "100.00", "4.75", "475.00")).isEqualTo(conversion(USD, "100.00", "4.75", "475.00"));
        assertThat(conversion(USD, "100.00", "4.75", "475.00")).isNotEqualTo(conversion(USD, "100.00", "4.76", "476.00"));
    }
}
