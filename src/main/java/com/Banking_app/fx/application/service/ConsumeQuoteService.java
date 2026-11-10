package com.Banking_app.fx.application.service;

import com.Banking_app.fx.application.port.in.command.ConsumeQuoteCommand;
import com.Banking_app.fx.application.port.in.result.Quote;
import com.Banking_app.fx.application.port.in.usecase.ConsumeQuoteUseCase;
import com.Banking_app.fx.application.port.out.LoadLockedQuotePort;
import com.Banking_app.fx.application.port.out.SaveLockedQuotePort;
import com.Banking_app.fx.domain.enums.ConversionDirection;
import com.Banking_app.fx.domain.exception.InvalidQuoteException;
import com.Banking_app.fx.domain.models.LockedQuote;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

@Service
public class ConsumeQuoteService implements ConsumeQuoteUseCase {

    private final LoadLockedQuotePort loadLockedQuotePort;
    private final SaveLockedQuotePort saveLockedQuotePort;
    private final Clock clock;

    public ConsumeQuoteService(LoadLockedQuotePort loadLockedQuotePort, SaveLockedQuotePort saveLockedQuotePort,
                               Clock clock) {
        this.loadLockedQuotePort = loadLockedQuotePort;
        this.saveLockedQuotePort = saveLockedQuotePort;
        this.clock = clock;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public Quote consume(ConsumeQuoteCommand command) {
        LockedQuote quote = loadLockedQuotePort.loadForUpdate(command.quoteId())
                .orElseThrow(InvalidQuoteException::notFound);

        LockedQuote used = quote.consume(command.requesterUserId(), command.currencyCode(), command.amount(),
                clock.instant());
        saveLockedQuotePort.save(used);

        // The amount as the customer sent it (e.g. JPY 10000), not as stored (10000.00): same value, checked above.
        return new Quote(used.getCurrency().getCurrencyCode(), command.amount(), ConversionDirection.PAY,
                used.getExchangeRate(), used.getMyrAmount(), used.getCreatedAt());
    }
}
