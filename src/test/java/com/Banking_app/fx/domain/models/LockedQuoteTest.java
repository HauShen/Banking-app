package com.Banking_app.fx.domain.models;

import com.Banking_app.fx.domain.exception.InvalidForeignAmountException;
import com.Banking_app.fx.domain.exception.InvalidQuoteException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Currency;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Plain unit tests: locking a rate, expiry, rebuilding a stored quote. */
class LockedQuoteTest {

    private static final Instant NOW = Instant.parse("2026-11-10T08:00:00Z");
    private static final Duration SIXTY_SECONDS = Duration.ofSeconds(60);
    private static final UUID ID = UUID.fromString("3f9a0c12-b7d4-4e5f-8a6b-1c2d3e4f5a6b");

    private static final ExchangeRate USD = new ExchangeRate(Currency.getInstance("USD"),
            new BigDecimal("4.60"), new BigDecimal("4.75"), NOW);
    private static final ExchangeRate JPY = new ExchangeRate(Currency.getInstance("JPY"),
            new BigDecimal("0.0290"), new BigDecimal("0.0305"), NOW);

    private static LockedQuote lockUsd(String amount) {
        return LockedQuote.lock(ID, "alice", USD, new BigDecimal(amount), NOW, SIXTY_SECONDS);
    }

    @Test
    @DisplayName("locks the SELL rate: 100.00 USD at 4.75 → RM 475.00, valid until now + 60 s, not used yet")
    void locksSellRate() {
        LockedQuote quote = lockUsd("100.00");

        assertThat(quote.getId()).isEqualTo(ID);
        assertThat(quote.getOwnerId()).isEqualTo("alice");
        assertThat(quote.getCurrency()).isEqualTo(Currency.getInstance("USD"));
        assertThat(quote.getAmount()).isEqualByComparingTo("100.00");
        assertThat(quote.getExchangeRate()).isEqualByComparingTo("4.75");
        assertThat(quote.getMyrAmount()).isEqualByComparingTo("475.00");
        assertThat(quote.getCreatedAt()).isEqualTo(NOW);
        assertThat(quote.getExpiresAt()).isEqualTo(NOW.plusSeconds(60));
        assertThat(quote.isUsed()).isFalse();
        assertThat(quote.getUsedAt()).isNull();
    }

    @Test
    @DisplayName("expired from expiresAt on: valid at second 59.999, expired at second 60")
    void expiry() {
        LockedQuote quote = lockUsd("100.00");

        assertThat(quote.isExpired(NOW)).isFalse();
        assertThat(quote.isExpired(NOW.plusMillis(59_999))).isFalse();
        assertThat(quote.isExpired(NOW.plusSeconds(60))).isTrue();
        assertThat(quote.isExpired(NOW.plusSeconds(3600))).isTrue();
    }

    @Test
    @DisplayName("the amount follows the conversion rules: JPY without decimals, nothing below RM 0.01")
    void amountRules() {
        assertThat(LockedQuote.lock(ID, "alice", JPY, new BigDecimal("10000"), NOW, SIXTY_SECONDS).getMyrAmount())
                .isEqualByComparingTo("305.00");
        assertThatThrownBy(() -> LockedQuote.lock(ID, "alice", JPY, new BigDecimal("1000.5"), NOW, SIXTY_SECONDS))
                .isInstanceOf(InvalidForeignAmountException.class)
                .hasMessage("JPY amounts cannot have decimal places");
        assertThatThrownBy(() -> lockUsd("0"))
                .isInstanceOf(InvalidForeignAmountException.class)
                .hasMessage("Amount must be greater than 0");
    }

    @Test
    @DisplayName("refuses a zero or negative validity and missing parts")
    void refusesNonsense() {
        assertThatThrownBy(() -> LockedQuote.lock(ID, "alice", USD, BigDecimal.TEN, NOW, Duration.ZERO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("A quote must be valid for a positive time");
        assertThatThrownBy(() -> LockedQuote.lock(ID, "alice", USD, BigDecimal.TEN, NOW, Duration.ofSeconds(-1)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> LockedQuote.lock(null, "alice", USD, BigDecimal.TEN, NOW, SIXTY_SECONDS))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> LockedQuote.lock(ID, null, USD, BigDecimal.TEN, NOW, SIXTY_SECONDS))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("rehydrate keeps every value, including used; a stored quote must expire after it was created")
    void rehydrate() {
        LockedQuote stored = LockedQuote.rehydrate(ID, "alice", Currency.getInstance("JPY"), new BigDecimal("10000.00"),
                new BigDecimal("0.03050000"), new BigDecimal("305.00"), NOW, NOW.plusSeconds(60), NOW.plusSeconds(10));

        assertThat(stored.isUsed()).isTrue();
        assertThat(stored.getUsedAt()).isEqualTo(NOW.plusSeconds(10));
        assertThat(stored.getAmount()).isEqualByComparingTo("10000");
        assertThatThrownBy(() -> LockedQuote.rehydrate(ID, "alice", Currency.getInstance("USD"), BigDecimal.ONE,
                BigDecimal.ONE, BigDecimal.ONE, NOW, NOW, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("A quote must expire after it was created");
    }

    @Nested
    @DisplayName("consume (a transfer uses the quote)")
    class Consume {

        @Test
        @DisplayName("the owner paying exactly the quoted amount before expiry → a used copy; the original is unchanged")
        void consumes() {
            LockedQuote quote = lockUsd("100.00");

            LockedQuote used = quote.consume("alice", "USD", new BigDecimal("100"), NOW.plusSeconds(30));

            assertThat(used.isUsed()).isTrue();
            assertThat(used.getUsedAt()).isEqualTo(NOW.plusSeconds(30));
            assertThat(used.getExchangeRate()).isEqualByComparingTo("4.75");
            assertThat(used.getMyrAmount()).isEqualByComparingTo("475.00");
            assertThat(quote.isUsed()).isFalse();
        }

        @Test
        @DisplayName("someone else's quote → 'Quote not found' (the same as an unknown one)")
        void notTheOwner() {
            assertThatThrownBy(() -> lockUsd("100.00").consume("bob", "USD", new BigDecimal("100.00"), NOW))
                    .isInstanceOf(InvalidQuoteException.class)
                    .isInstanceOf(IllegalArgumentException.class)   // → 400
                    .hasMessage("Quote not found");
        }

        @Test
        @DisplayName("already used → 'The quote has already been used' (also when it has expired since)")
        void alreadyUsed() {
            LockedQuote used = lockUsd("100.00").consume("alice", "USD", new BigDecimal("100.00"), NOW);

            assertThatThrownBy(() -> used.consume("alice", "USD", new BigDecimal("100.00"), NOW.plusSeconds(1)))
                    .hasMessage("The quote has already been used");
            assertThatThrownBy(() -> used.consume("alice", "USD", new BigDecimal("100.00"), NOW.plusSeconds(600)))
                    .hasMessage("The quote has already been used");
        }

        @Test
        @DisplayName("at or after expiresAt → 'The quote has expired'")
        void expired() {
            assertThatThrownBy(() -> lockUsd("100.00").consume("alice", "USD", new BigDecimal("100.00"),
                    NOW.plusSeconds(60)))
                    .isInstanceOf(InvalidQuoteException.class)
                    .hasMessage("The quote has expired");
        }

        @Test
        @DisplayName("another currency or amount → 'The quote is for a different currency or amount'")
        void differentPayment() {
            LockedQuote quote = lockUsd("100.00");

            for (Object[] payment : new Object[][]{{"SGD", "100.00"}, {"USD", "100.01"}, {"USD", null}}) {
                assertThatThrownBy(() -> quote.consume("alice", (String) payment[0],
                        payment[1] == null ? null : new BigDecimal((String) payment[1]), NOW))
                        .hasMessage("The quote is for a different currency or amount");
            }
        }

        @Test
        @DisplayName("a stored JPY quote (10000.00 from the database) matches a payment of 10000")
        void storedJpy() {
            LockedQuote stored = LockedQuote.rehydrate(ID, "alice", Currency.getInstance("JPY"),
                    new BigDecimal("10000.00"), new BigDecimal("0.03050000"), new BigDecimal("305.00"), NOW,
                    NOW.plusSeconds(60), null);

            assertThat(stored.consume("alice", "JPY", new BigDecimal("10000"), NOW).isUsed()).isTrue();
        }
    }
}
