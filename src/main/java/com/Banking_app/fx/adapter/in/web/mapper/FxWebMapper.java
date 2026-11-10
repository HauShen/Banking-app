package com.Banking_app.fx.adapter.in.web.mapper;

import com.Banking_app.fx.adapter.in.web.dto.ExchangeRateResponseBody;
import com.Banking_app.fx.adapter.in.web.dto.LockQuoteRequestBody;
import com.Banking_app.fx.adapter.in.web.dto.LockedQuoteResponseBody;
import com.Banking_app.fx.adapter.in.web.dto.QuoteResponseBody;
import com.Banking_app.fx.application.port.in.command.LockQuoteCommand;
import com.Banking_app.fx.application.port.in.query.QuoteQuery;
import com.Banking_app.fx.application.port.in.result.ExchangeRateView;
import com.Banking_app.fx.application.port.in.result.LockedQuoteView;
import com.Banking_app.fx.application.port.in.result.Quote;
import com.Banking_app.fx.domain.enums.ConversionDirection;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Query parameters → QuoteQuery, results → JSON bodies.
 *
 * The parameters arrive as text and are parsed here, so a missing or malformed value gets a 400 with a clear message
 * (Spring's own conversion errors would reach the catch-all handler as a 500).
 */
@Component
public class FxWebMapper {

    /** Same limit as transfer amounts (@Digits integer = 17); also stops "1e999999999" from building a huge number. */
    static final int MAX_INTEGER_DIGITS = 17;

    public QuoteQuery toQuery(String currency, String amount, String direction) {
        if (currency == null || currency.isBlank()) {
            throw new IllegalArgumentException("currency is required");
        }
        return new QuoteQuery(currency, parseAmount(amount), parseDirection(direction));
    }

    private static BigDecimal parseAmount(String amount) {
        if (amount == null || amount.isBlank()) {
            throw new IllegalArgumentException("amount is required");
        }
        BigDecimal value;
        try {
            value = new BigDecimal(amount.trim());
        } catch (NumberFormatException notANumber) {
            throw new IllegalArgumentException("amount must be a number");
        }
        if (value.precision() - value.scale() > MAX_INTEGER_DIGITS) {
            throw new IllegalArgumentException(
                    "amount can have at most " + MAX_INTEGER_DIGITS + " digits before the decimal point");
        }
        return value;
    }

    /** Absent → PAY (the customer's usual question: "what will this cost me?"). */
    private static ConversionDirection parseDirection(String direction) {
        if (direction == null || direction.isBlank()) {
            return ConversionDirection.PAY;
        }
        try {
            return ConversionDirection.valueOf(direction);
        } catch (IllegalArgumentException unknown) {
            throw new IllegalArgumentException("direction must be PAY or RECEIVE");
        }
    }

    public QuoteResponseBody toResponse(Quote quote) {
        return new QuoteResponseBody(quote.currency(), quote.amount(), quote.direction().name(),
                quote.exchangeRate(), quote.myrAmount(), quote.asOf());
    }

    public LockQuoteCommand toCommand(LockQuoteRequestBody body, String currentUserId) {
        return new LockQuoteCommand(currentUserId, body.currency(), body.amount());
    }

    public LockedQuoteResponseBody toResponse(LockedQuoteView quote) {
        return new LockedQuoteResponseBody(quote.quoteId(), quote.currency(), quote.amount(), quote.exchangeRate(),
                quote.myrAmount(), quote.expiresAt());
    }

    public ExchangeRateResponseBody toResponse(ExchangeRateView rate) {
        return new ExchangeRateResponseBody(rate.currency(), rate.buyRate(), rate.sellRate(), rate.asOf());
    }
}
