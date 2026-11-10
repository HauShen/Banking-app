package com.Banking_app.fx.application.service;

import com.Banking_app.fx.application.port.in.command.ConsumeQuoteCommand;
import com.Banking_app.fx.application.port.in.result.Quote;
import com.Banking_app.fx.application.port.out.LoadLockedQuotePort;
import com.Banking_app.fx.application.port.out.SaveLockedQuotePort;
import com.Banking_app.fx.domain.enums.ConversionDirection;
import com.Banking_app.fx.domain.exception.InvalidQuoteException;
import com.Banking_app.fx.domain.models.ExchangeRate;
import com.Banking_app.fx.domain.models.LockedQuote;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Use-case test with an in-memory quote store (no Spring). The MANDATORY transaction and the row lock are Spring and
 * database features; LockedQuotePersistenceAdapterTest and LockedQuoteTransferTest cover them.
 */
class ConsumeQuoteServiceTest {

    private static final Instant LOCKED_AT = Instant.parse("2026-11-10T08:00:00Z");
    private static final ExchangeRate USD = new ExchangeRate(java.util.Currency.getInstance("USD"),
            new BigDecimal("4.60"), new BigDecimal("4.75"), LOCKED_AT);

    private final Map<UUID, LockedQuote> rows = new HashMap<>();
    private final List<UUID> lockedIds = new ArrayList<>();
    private final LoadLockedQuotePort load = id -> {
        lockedIds.add(id);
        return Optional.ofNullable(rows.get(id));
    };
    private final SaveLockedQuotePort save = quote -> rows.put(quote.getId(), quote);

    private ConsumeQuoteService serviceAt(Instant now) {
        return new ConsumeQuoteService(load, save, Clock.fixed(now, ZoneOffset.UTC));
    }

    private UUID storedQuote(String owner, String amount) {
        LockedQuote quote = LockedQuote.lock(UUID.randomUUID(), owner, USD, new BigDecimal(amount), LOCKED_AT,
                Duration.ofSeconds(60));
        rows.put(quote.getId(), quote);
        return quote.getId();
    }

    @Test
    @DisplayName("uses the quote: returns the locked rate and MYR amount as a PAY quote, stores it as used")
    void consumes() {
        UUID id = storedQuote("alice", "100.00");

        Quote quote = serviceAt(LOCKED_AT.plusSeconds(30))
                .consume(new ConsumeQuoteCommand(id, "alice", "USD", new BigDecimal("100.00")));

        assertThat(quote.currency()).isEqualTo("USD");
        assertThat(quote.amount()).isEqualByComparingTo("100.00");
        assertThat(quote.direction()).isEqualTo(ConversionDirection.PAY);
        assertThat(quote.exchangeRate()).isEqualByComparingTo("4.75");
        assertThat(quote.myrAmount()).isEqualByComparingTo("475.00");
        assertThat(quote.asOf()).isEqualTo(LOCKED_AT);
        assertThat(rows.get(id).getUsedAt()).isEqualTo(LOCKED_AT.plusSeconds(30));
        assertThat(lockedIds).containsExactly(id);   // loaded with the row lock
    }

    @Test
    @DisplayName("unknown quote id → 'Quote not found'")
    void unknown() {
        assertThatThrownBy(() -> serviceAt(LOCKED_AT).consume(
                new ConsumeQuoteCommand(UUID.randomUUID(), "alice", "USD", BigDecimal.TEN)))
                .isInstanceOf(InvalidQuoteException.class)
                .hasMessage("Quote not found");
    }

    @Test
    @DisplayName("the second use → 'The quote has already been used'")
    void secondUse() {
        UUID id = storedQuote("alice", "100.00");
        ConsumeQuoteCommand command = new ConsumeQuoteCommand(id, "alice", "USD", new BigDecimal("100.00"));
        serviceAt(LOCKED_AT.plusSeconds(1)).consume(command);

        assertThatThrownBy(() -> serviceAt(LOCKED_AT.plusSeconds(2)).consume(command))
                .hasMessage("The quote has already been used");
    }

    @Test
    @DisplayName("a refused quote is not stored as used")
    void refusedNotStored() {
        UUID id = storedQuote("alice", "100.00");

        assertThatThrownBy(() -> serviceAt(LOCKED_AT.plusSeconds(60)).consume(
                new ConsumeQuoteCommand(id, "alice", "USD", new BigDecimal("100.00"))))
                .hasMessage("The quote has expired");
        assertThatThrownBy(() -> serviceAt(LOCKED_AT).consume(
                new ConsumeQuoteCommand(id, "bob", "USD", new BigDecimal("100.00"))))
                .hasMessage("Quote not found");
        assertThat(rows.get(id).isUsed()).isFalse();
    }
}
