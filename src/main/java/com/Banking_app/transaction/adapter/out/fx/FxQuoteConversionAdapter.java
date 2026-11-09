package com.Banking_app.transaction.adapter.out.fx;

import com.Banking_app.fx.application.port.in.query.QuoteQuery;
import com.Banking_app.fx.application.port.in.result.Quote;
import com.Banking_app.fx.application.port.in.usecase.QuoteUseCase;
import com.Banking_app.fx.domain.enums.ConversionDirection;
import com.Banking_app.transaction.application.port.out.CurrencyConversionPort;
import com.Banking_app.transaction.domain.models.transaction.CurrencyConversion;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Currency;

/**
 * Outbound adapter to the fx module: a conversion is the fx quote taken at the moment the transaction runs.
 * Only this class knows the fx module (through its inbound port QuoteUseCase), like AccountModuleTransferAdapter for
 * accounts. fx's exceptions (400s with the client-facing messages) pass through unchanged.
 */
@Component
public class FxQuoteConversionAdapter implements CurrencyConversionPort {

    private final QuoteUseCase quoteUseCase;

    public FxQuoteConversionAdapter(QuoteUseCase quoteUseCase) {
        this.quoteUseCase = quoteUseCase;
    }

    @Override
    public CurrencyConversion convertPayment(String currencyCode, BigDecimal amount) {
        return convert(currencyCode, amount, ConversionDirection.PAY);
    }

    @Override
    public CurrencyConversion convertIncoming(String currencyCode, BigDecimal amount) {
        return convert(currencyCode, amount, ConversionDirection.RECEIVE);
    }

    private CurrencyConversion convert(String currencyCode, BigDecimal amount, ConversionDirection direction) {
        Quote quote = quoteUseCase.quote(new QuoteQuery(currencyCode, amount, direction));
        return new CurrencyConversion(Currency.getInstance(quote.currency()), quote.amount(), quote.exchangeRate(),
                quote.myrAmount());
    }
}
