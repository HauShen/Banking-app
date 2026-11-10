package com.Banking_app.fx.adapter.in.web.mapper;

import com.Banking_app.fx.adapter.in.web.dto.LockQuoteRequestBody;
import com.Banking_app.fx.adapter.in.web.dto.LockedQuoteResponseBody;
import com.Banking_app.fx.application.port.in.command.LockQuoteCommand;
import com.Banking_app.fx.application.port.in.query.QuoteQuery;
import com.Banking_app.fx.application.port.in.result.LockedQuoteView;
import com.Banking_app.fx.domain.enums.ConversionDirection;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Parsing the quote's query parameters (plain unit test). */
class FxWebMapperTest {

    private final FxWebMapper mapper = new FxWebMapper();

    @Test
    @DisplayName("text parameters become a query; the amount keeps its decimals as written")
    void parses() {
        QuoteQuery query = mapper.toQuery("USD", "100.00", "RECEIVE");

        assertThat(query.currencyCode()).isEqualTo("USD");
        assertThat(query.amount()).isEqualByComparingTo("100.00");
        assertThat(query.amount().scale()).isEqualTo(2);
        assertThat(query.direction()).isEqualTo(ConversionDirection.RECEIVE);
    }

    @Test
    @DisplayName("no direction → PAY")
    void defaultDirection() {
        assertThat(mapper.toQuery("USD", "1", null).direction()).isEqualTo(ConversionDirection.PAY);
        assertThat(mapper.toQuery("USD", "1", "").direction()).isEqualTo(ConversionDirection.PAY);
    }

    @Test
    @DisplayName("missing or malformed parameters → IllegalArgumentException (400) with a clear message")
    void refused() {
        assertThatThrownBy(() -> mapper.toQuery(null, "1", "PAY")).hasMessage("currency is required");
        assertThatThrownBy(() -> mapper.toQuery(" ", "1", "PAY")).hasMessage("currency is required");
        assertThatThrownBy(() -> mapper.toQuery("USD", null, "PAY")).hasMessage("amount is required");
        assertThatThrownBy(() -> mapper.toQuery("USD", "abc", "PAY"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("amount must be a number");
        assertThatThrownBy(() -> mapper.toQuery("USD", "1", "pay")).hasMessage("direction must be PAY or RECEIVE");
        assertThatThrownBy(() -> mapper.toQuery("USD", "1", "SELL")).hasMessage("direction must be PAY or RECEIVE");
    }

    @Test
    @DisplayName("more than 17 digits before the point is refused, also written as an exponent (1e999999999)")
    void tooLarge() {
        assertThat(mapper.toQuery("USD", "99999999999999999", null).amount()).isEqualByComparingTo("99999999999999999");
        assertThatThrownBy(() -> mapper.toQuery("USD", "100000000000000000", null))
                .hasMessage("amount can have at most 17 digits before the decimal point");
        assertThatThrownBy(() -> mapper.toQuery("USD", "1e999999999", null))
                .hasMessage("amount can have at most 17 digits before the decimal point");
    }

    @Test
    @DisplayName("lock request + logged-in user → command; locked quote → reply")
    void lockQuote() {
        LockQuoteCommand command = mapper.toCommand(new LockQuoteRequestBody("USD", new BigDecimal("100.00")), "alice");
        assertThat(command.requesterUserId()).isEqualTo("alice");
        assertThat(command.currencyCode()).isEqualTo("USD");
        assertThat(command.amount()).isEqualByComparingTo("100.00");

        UUID id = UUID.randomUUID();
        Instant expiresAt = Instant.parse("2026-11-10T08:01:00Z");
        LockedQuoteResponseBody body = mapper.toResponse(new LockedQuoteView(id, "USD", new BigDecimal("100.00"),
                new BigDecimal("4.75"), new BigDecimal("475.00"), expiresAt));
        assertThat(body.quoteId()).isEqualTo(id);
        assertThat(body.currency()).isEqualTo("USD");
        assertThat(body.amount()).isEqualByComparingTo("100.00");
        assertThat(body.exchangeRate()).isEqualByComparingTo("4.75");
        assertThat(body.myrAmount()).isEqualByComparingTo("475.00");
        assertThat(body.expiresAt()).isEqualTo(expiresAt);
    }
}
