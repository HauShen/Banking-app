package com.Banking_app.fx.domain.models;

import com.Banking_app.fx.domain.enums.ConversionDirection;
import com.Banking_app.fx.domain.exception.InvalidExchangeRateException;
import com.Banking_app.fx.domain.exception.InvalidForeignAmountException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Currency;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Plain unit tests: an exchange rate's rules and the conversion to MYR. */
class ExchangeRateTest {

    private static final Instant AS_OF = Instant.parse("2026-11-09T00:00:00Z");

    private static ExchangeRate rate(String currency, String buy, String sell) {
        return new ExchangeRate(Currency.getInstance(currency),
                buy == null ? null : new BigDecimal(buy), sell == null ? null : new BigDecimal(sell), AS_OF);
    }

    private static final ExchangeRate USD = rate("USD", "4.60", "4.75");
    private static final ExchangeRate JPY = rate("JPY", "0.0290", "0.0305");

    @Nested
    @DisplayName("a valid rate")
    class Valid {

        @Test
        @DisplayName("PAY uses the sell rate (the bank sells), RECEIVE the buy rate (the bank buys)")
        void rateForDirection() {
            assertThat(USD.rateFor(ConversionDirection.PAY)).isEqualByComparingTo("4.75");
            assertThat(USD.rateFor(ConversionDirection.RECEIVE)).isEqualByComparingTo("4.60");
        }

        @Test
        @DisplayName("buy rate = sell rate is allowed (no margin)")
        void equalRates() {
            assertThat(rate("SGD", "3.50", "3.50").sellRate()).isEqualByComparingTo("3.50");
        }

        @Test
        @DisplayName("a rate with 8 decimals is fine")
        void eightDecimals() {
            assertThat(rate("JPY", "0.02912345", "0.03012345").buyRate()).isEqualByComparingTo("0.02912345");
        }
    }

    @Nested
    @DisplayName("refused rates (a wrong configuration stops startup)")
    class Invalid {

        @Test
        @DisplayName("the buy rate above the sell rate")
        void buyAboveSell() {
            assertThatThrownBy(() -> rate("USD", "4.80", "4.75"))
                    .isInstanceOf(InvalidExchangeRateException.class)
                    .hasMessage("USD: the buy rate (4.80) must not be above the sell rate (4.75)");
        }

        @Test
        @DisplayName("missing, zero, negative, or more than 8 decimals")
        void badRates() {
            assertThatThrownBy(() -> rate("USD", null, "4.75"))
                    .isInstanceOf(InvalidExchangeRateException.class)
                    .hasMessage("USD: the buy rate is missing");
            assertThatThrownBy(() -> rate("USD", "4.60", null))
                    .hasMessage("USD: the sell rate is missing");
            assertThatThrownBy(() -> rate("USD", "0", "4.75"))
                    .hasMessage("USD: the buy rate (0) must be greater than 0 with at most 8 decimal places");
            assertThatThrownBy(() -> rate("USD", "-4.60", "4.75"))
                    .isInstanceOf(InvalidExchangeRateException.class);
            assertThatThrownBy(() -> rate("USD", "4.60", "4.750000001"))
                    .hasMessage("USD: the sell rate (4.750000001) must be greater than 0 with at most 8 decimal places");
        }

        @Test
        @DisplayName("MYR against itself, a currency with 3 decimals (KWD), no currency")
        void badCurrency() {
            assertThatThrownBy(() -> rate("MYR", "1", "1"))
                    .isInstanceOf(InvalidExchangeRateException.class)
                    .hasMessage("MYR has no exchange rate against itself");
            assertThatThrownBy(() -> rate("KWD", "15", "15.5"))
                    .isInstanceOf(InvalidExchangeRateException.class)
                    .hasMessage("KWD is not supported: only currencies with at most 2 decimal places can be converted");
            assertThatThrownBy(() -> new ExchangeRate(null, BigDecimal.ONE, BigDecimal.ONE, AS_OF))
                    .isInstanceOf(InvalidExchangeRateException.class);
        }
    }

    @Nested
    @DisplayName("toMyr")
    class ToMyr {

        @Test
        @DisplayName("100.00 USD: paid → RM 475.00 (sell), received → RM 460.00 (buy)")
        void usd() {
            assertThat(USD.toMyr(new BigDecimal("100.00"), ConversionDirection.PAY)).isEqualByComparingTo("475.00");
            assertThat(USD.toMyr(new BigDecimal("100.00"), ConversionDirection.RECEIVE)).isEqualByComparingTo("460.00");
            assertThat(USD.toMyr(new BigDecimal("100.00"), ConversionDirection.PAY).scale()).isEqualTo(2);
        }

        @Test
        @DisplayName("rounds HALF_UP to 2 decimals: 0.99 USD × 4.75 = 4.7025 → 4.70; 0.03 × 4.75 = 0.1425 → 0.14")
        void roundsHalfUp() {
            assertThat(USD.toMyr(new BigDecimal("0.99"), ConversionDirection.PAY)).isEqualByComparingTo("4.70");
            assertThat(USD.toMyr(new BigDecimal("0.03"), ConversionDirection.PAY)).isEqualByComparingTo("0.14");
            assertThat(rate("USD", "4.715", "4.715").toMyr(new BigDecimal("1.00"), ConversionDirection.PAY))
                    .isEqualByComparingTo("4.72");
        }

        @Test
        @DisplayName("JPY has no decimals: 10000 → RM 305.00; 1000.5 and 1000.0 are refused")
        void jpy() {
            assertThat(JPY.toMyr(new BigDecimal("10000"), ConversionDirection.PAY)).isEqualByComparingTo("305.00");
            assertThatThrownBy(() -> JPY.toMyr(new BigDecimal("1000.5"), ConversionDirection.PAY))
                    .isInstanceOf(InvalidForeignAmountException.class)
                    .hasMessage("JPY amounts cannot have decimal places");
            assertThatThrownBy(() -> JPY.toMyr(new BigDecimal("1000.0"), ConversionDirection.PAY))
                    .isInstanceOf(InvalidForeignAmountException.class);
        }

        @Test
        @DisplayName("USD with 3 decimals as written (10.005, 10.500) is refused")
        void usdDecimals() {
            assertThatThrownBy(() -> USD.toMyr(new BigDecimal("10.005"), ConversionDirection.PAY))
                    .isInstanceOf(InvalidForeignAmountException.class)
                    .hasMessage("USD amounts can have at most 2 decimal places");
            assertThatThrownBy(() -> USD.toMyr(new BigDecimal("10.500"), ConversionDirection.PAY))
                    .isInstanceOf(InvalidForeignAmountException.class);
        }

        @Test
        @DisplayName("zero, negative or missing amount")
        void badAmount() {
            for (String amount : new String[]{"0", "-1.00"}) {
                assertThatThrownBy(() -> USD.toMyr(new BigDecimal(amount), ConversionDirection.PAY))
                        .as("amount %s", amount)
                        .isInstanceOf(InvalidForeignAmountException.class)
                        .hasMessage("Amount must be greater than 0");
            }
            assertThatThrownBy(() -> USD.toMyr(null, ConversionDirection.PAY))
                    .isInstanceOf(InvalidForeignAmountException.class);
        }

        @Test
        @DisplayName("below RM 0.01 is refused (1 JPY × 0.0049 → 0.00); exactly half a sen rounds up and is allowed")
        void belowOneSen() {
            ExchangeRate tiny = rate("JPY", "0.0049", "0.005");
            assertThatThrownBy(() -> tiny.toMyr(BigDecimal.ONE, ConversionDirection.RECEIVE))
                    .isInstanceOf(InvalidForeignAmountException.class)
                    .hasMessage("The converted amount is less than RM 0.01");
            assertThat(tiny.toMyr(BigDecimal.ONE, ConversionDirection.PAY)).isEqualByComparingTo("0.01");
        }
    }

    @Test
    @DisplayName("a value object: equal when all parts are equal")
    void equality() {
        assertThat(rate("USD", "4.60", "4.75")).isEqualTo(USD);
        assertThat(rate("USD", "4.60", "4.76")).isNotEqualTo(USD);
    }
}
