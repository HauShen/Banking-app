package com.Banking_app.fx.adapter.in.web.mapper;

import com.Banking_app.fx.application.port.in.query.QuoteQuery;
import com.Banking_app.fx.domain.enums.ConversionDirection;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

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
}
