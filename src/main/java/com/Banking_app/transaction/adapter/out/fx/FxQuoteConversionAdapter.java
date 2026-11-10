package com.Banking_app.transaction.adapter.out.fx;

import com.Banking_app.fx.application.port.in.command.ConsumeQuoteCommand;
import com.Banking_app.fx.application.port.in.query.QuoteQuery;
import com.Banking_app.fx.application.port.in.result.Quote;
import com.Banking_app.fx.application.port.in.usecase.ConsumeQuoteUseCase;
import com.Banking_app.fx.application.port.in.usecase.QuoteUseCase;
import com.Banking_app.fx.domain.enums.ConversionDirection;
import com.Banking_app.transaction.application.port.out.CurrencyConversionPort;
import com.Banking_app.transaction.domain.models.transaction.CurrencyConversion;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.UUID;

/**
 * Outbound adapter to the fx module: a conversion is the fx quote taken at the moment the transaction runs, or a
 * quote the customer locked earlier. Only this class knows the fx module (through its inbound ports QuoteUseCase and
 * ConsumeQuoteUseCase), like AccountModuleTransferAdapter for accounts. fx's exceptions (400s with the
 * client-facing messages) pass through unchanged.
 */
@Component
public class FxQuoteConversionAdapter implements CurrencyConversionPort {

    private final QuoteUseCase quoteUseCase;
    private final ConsumeQuoteUseCase consumeQuoteUseCase;

    public FxQuoteConversionAdapter(QuoteUseCase quoteUseCase, ConsumeQuoteUseCase consumeQuoteUseCase) {
        this.quoteUseCase = quoteUseCase;
        this.consumeQuoteUseCase = consumeQuoteUseCase;
    }

    @Override
    public CurrencyConversion convertPayment(String currencyCode, BigDecimal amount) {
        return convert(currencyCode, amount, ConversionDirection.PAY);
    }

    @Override
    public CurrencyConversion convertIncoming(String currencyCode, BigDecimal amount) {
        return convert(currencyCode, amount, ConversionDirection.RECEIVE);
    }

    @Override
    public CurrencyConversion convertWithQuote(String quoteId, String requesterUserId, String currencyCode,
                                               BigDecimal amount) {
        // quoteId is checked to be a UUID by the request validation
        return toConversion(consumeQuoteUseCase.consume(
                new ConsumeQuoteCommand(UUID.fromString(quoteId), requesterUserId, currencyCode, amount)));
    }

    private CurrencyConversion convert(String currencyCode, BigDecimal amount, ConversionDirection direction) {
        return toConversion(quoteUseCase.quote(new QuoteQuery(currencyCode, amount, direction)));
    }

    private static CurrencyConversion toConversion(Quote quote) {
        return new CurrencyConversion(Currency.getInstance(quote.currency()), quote.amount(), quote.exchangeRate(),
                quote.myrAmount());
    }
}
