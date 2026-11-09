package com.Banking_app.transaction.domain.models.transaction;

import com.Banking_app.transaction.domain.exception.InvalidCurrencyConversionException;
import com.Banking_app.transaction.domain.exception.InvalidTransactionAmountException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Currency;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Plain unit tests for converting a foreign amount to MYR. */
class CurrencyConversionTest {

    private static CurrencyConversion convert(String currency, String amount, String rate) {
        return CurrencyConversion.toMyr(currency, new BigDecimal(amount), new BigDecimal(rate));
    }

    @Nested
    @DisplayName("converting")
    class Converting {

        @Test
        @DisplayName("100.00 USD at 4.75 → RM 475.00; the original currency, amount and rate are kept")
        void usdToMyr() {
            CurrencyConversion conversion = convert("USD", "100.00", "4.75");

            assertThat(conversion.originalCurrency()).isEqualTo(Currency.getInstance("USD"));
            assertThat(conversion.originalAmount()).isEqualByComparingTo("100.00");
            assertThat(conversion.exchangeRate()).isEqualByComparingTo("4.75");
            assertThat(conversion.myrAmount()).isEqualByComparingTo("475.00");
            assertThat(conversion.myrAmount().scale()).isEqualTo(2);
        }

        @Test
        @DisplayName("rounds HALF_UP to 2 decimals: 1.00 × 4.715 → 4.72, 1.00 × 4.714 → 4.71")
        void roundsHalfUp() {
            assertThat(convert("USD", "1.00", "4.715").myrAmount()).isEqualByComparingTo("4.72");
            assertThat(convert("USD", "1.00", "4.714").myrAmount()).isEqualByComparingTo("4.71");
        }

        @Test
        @DisplayName("JPY has no decimals: 10000 × 0.0305 → RM 305.00")
        void jpy() {
            assertThat(convert("JPY", "10000", "0.0305").myrAmount()).isEqualByComparingTo("305.00");
        }

        @Test
        @DisplayName("a rate with 8 decimals is fine")
        void eightDecimalRate() {
            assertThat(convert("USD", "100.00", "4.71234567").myrAmount()).isEqualByComparingTo("471.23");
        }

        @Test
        @DisplayName("exactly half a sen rounds up to RM 0.01 and is allowed")
        void halfASenRoundsUp() {
            assertThat(convert("JPY", "1", "0.005").myrAmount()).isEqualByComparingTo("0.01");
        }
    }

    @Nested
    @DisplayName("refusing")
    class Refusing {

        @Test
        @DisplayName("more decimals than the currency has, as written: USD 10.005, USD 10.500, JPY 1000.5 / 1000.0")
        void tooManyDecimals() {
            assertThatThrownBy(() -> convert("USD", "10.005", "4.75"))
                    .isInstanceOf(InvalidCurrencyConversionException.class)
                    .hasMessage("USD amounts can have at most 2 decimal places");
            assertThatThrownBy(() -> convert("USD", "10.500", "4.75"))
                    .isInstanceOf(InvalidCurrencyConversionException.class);
            assertThatThrownBy(() -> convert("JPY", "1000.5", "0.0305"))
                    .isInstanceOf(InvalidCurrencyConversionException.class)
                    .hasMessage("JPY amounts cannot have decimal places");
            assertThatThrownBy(() -> convert("JPY", "1000.0", "0.0305"))
                    .isInstanceOf(InvalidCurrencyConversionException.class);
        }

        @Test
        @DisplayName("MYR itself needs no conversion")
        void myr() {
            assertThatThrownBy(() -> convert("MYR", "10.00", "1"))
                    .isInstanceOf(InvalidCurrencyConversionException.class)
                    .hasMessage("MYR needs no conversion");
        }

        @Test
        @DisplayName("unknown codes, empty codes and currencies with more than 2 decimals (KWD) are not supported")
        void notSupported() {
            assertThatThrownBy(() -> convert("XYZ", "10.00", "1"))
                    .isInstanceOf(InvalidCurrencyConversionException.class)
                    .hasMessage("Currency XYZ is not supported");
            assertThatThrownBy(() -> convert("usd", "10.00", "1"))
                    .isInstanceOf(InvalidCurrencyConversionException.class);
            assertThatThrownBy(() -> CurrencyConversion.toMyr(null, BigDecimal.TEN, BigDecimal.ONE))
                    .isInstanceOf(InvalidCurrencyConversionException.class);
            assertThatThrownBy(() -> convert("KWD", "10.000", "15"))
                    .isInstanceOf(InvalidCurrencyConversionException.class)
                    .hasMessage("Currency KWD is not supported");
        }

        @Test
        @DisplayName("zero, negative or missing amount")
        void badAmount() {
            for (String amount : new String[]{"0", "-1.00"}) {
                assertThatThrownBy(() -> convert("USD", amount, "4.75"))
                        .as("amount %s", amount)
                        .isInstanceOf(InvalidTransactionAmountException.class);
            }
            assertThatThrownBy(() -> CurrencyConversion.toMyr("USD", null, new BigDecimal("4.75")))
                    .isInstanceOf(InvalidTransactionAmountException.class);
        }

        @Test
        @DisplayName("zero, negative, missing rate, or a rate with more than 8 decimals")
        void badRate() {
            for (String rate : new String[]{"0", "-4.75", "4.712345678"}) {
                assertThatThrownBy(() -> convert("USD", "10.00", rate))
                        .as("rate %s", rate)
                        .isInstanceOf(InvalidCurrencyConversionException.class)
                        .hasMessage("Exchange rate must be greater than 0 with at most 8 decimal places");
            }
            assertThatThrownBy(() -> CurrencyConversion.toMyr("USD", BigDecimal.TEN, null))
                    .isInstanceOf(InvalidCurrencyConversionException.class);
        }

        @Test
        @DisplayName("a result below RM 0.01 (1 JPY × 0.0049 = 0.0049 → 0.00)")
        void belowOneSen() {
            assertThatThrownBy(() -> convert("JPY", "1", "0.0049"))
                    .isInstanceOf(InvalidCurrencyConversionException.class)
                    .hasMessage("The converted amount is less than RM 0.01");
        }
    }

    @Test
    @DisplayName("a value object: equal when all parts are equal")
    void equality() {
        assertThat(convert("USD", "100.00", "4.75")).isEqualTo(convert("USD", "100.00", "4.75"));
        assertThat(convert("USD", "100.00", "4.75")).isNotEqualTo(convert("USD", "100.00", "4.76"));
    }

    @Test
    @DisplayName("the constructor (used when reading from the database) rejects non-positive values")
    void constructorSanity() {
        assertThatThrownBy(() -> new CurrencyConversion(Currency.getInstance("USD"), BigDecimal.ZERO, BigDecimal.ONE,
                BigDecimal.ONE)).isInstanceOf(IllegalArgumentException.class);
    }
}
