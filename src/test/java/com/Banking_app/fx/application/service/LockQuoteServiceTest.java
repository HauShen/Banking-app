package com.Banking_app.fx.application.service;

import com.Banking_app.fx.application.exception.UnsupportedCurrencyException;
import com.Banking_app.fx.application.port.in.command.LockQuoteCommand;
import com.Banking_app.fx.application.port.in.result.LockedQuoteView;
import com.Banking_app.fx.application.port.out.SaveLockedQuotePort;
import com.Banking_app.fx.domain.exception.InvalidForeignAmountException;
import com.Banking_app.fx.domain.models.LockedQuote;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Use-case test with fake rates and an in-memory store (no Spring), fixed clock. */
class LockQuoteServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-10T08:00:00Z");

    private final List<LockedQuote> saved = new ArrayList<>();
    private final SaveLockedQuotePort store = saved::add;
    private final LockQuoteService service = new LockQuoteService(
            new FakeExchangeRateSource().with("USD", "4.60", "4.75").with("JPY", "0.0290", "0.0305"),
            store, Clock.fixed(NOW, ZoneOffset.UTC), Duration.ofSeconds(60));

    private LockedQuoteView lock(String currency, String amount) {
        return service.lock(new LockQuoteCommand("alice", currency, new BigDecimal(amount)));
    }

    @Test
    @DisplayName("locks the sell rate for the requester, stores it, and returns id, rate, MYR amount and expiry")
    void locks() {
        LockedQuoteView view = lock("USD", "100.00");

        assertThat(view.quoteId()).isNotNull();
        assertThat(view.currency()).isEqualTo("USD");
        assertThat(view.amount()).isEqualByComparingTo("100.00");
        assertThat(view.exchangeRate()).isEqualByComparingTo("4.75");
        assertThat(view.myrAmount()).isEqualByComparingTo("475.00");
        assertThat(view.expiresAt()).isEqualTo(NOW.plusSeconds(60));

        assertThat(saved).hasSize(1);
        assertThat(saved.get(0).getId()).isEqualTo(view.quoteId());
        assertThat(saved.get(0).getOwnerId()).isEqualTo("alice");
        assertThat(saved.get(0).isUsed()).isFalse();
    }

    @Test
    @DisplayName("every lock is a new quote with its own id")
    void newIdEachTime() {
        assertThat(lock("USD", "100.00").quoteId()).isNotEqualTo(lock("USD", "100.00").quoteId());
        assertThat(saved).hasSize(2);
    }

    @Test
    @DisplayName("currency not offered, MYR, or a bad amount → refused and nothing stored")
    void refused() {
        assertThatThrownBy(() -> lock("SGD", "10.00"))
                .isInstanceOf(UnsupportedCurrencyException.class)
                .hasMessage("Currency SGD is not supported");
        assertThatThrownBy(() -> lock("MYR", "10.00"))
                .hasMessage("MYR needs no conversion");
        assertThatThrownBy(() -> lock("JPY", "1000.5"))
                .isInstanceOf(InvalidForeignAmountException.class)
                .hasMessage("JPY amounts cannot have decimal places");
        assertThat(saved).isEmpty();
    }
}
