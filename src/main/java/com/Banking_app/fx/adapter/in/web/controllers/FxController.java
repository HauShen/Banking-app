package com.Banking_app.fx.adapter.in.web.controllers;

import com.Banking_app.fx.adapter.in.web.dto.ExchangeRateResponseBody;
import com.Banking_app.fx.adapter.in.web.dto.LockQuoteRequestBody;
import com.Banking_app.fx.adapter.in.web.dto.LockedQuoteResponseBody;
import com.Banking_app.fx.adapter.in.web.dto.QuoteResponseBody;
import com.Banking_app.fx.adapter.in.web.mapper.FxWebMapper;
import com.Banking_app.fx.application.port.in.usecase.GetExchangeRateUseCase;
import com.Banking_app.fx.application.port.in.usecase.LockQuoteUseCase;
import com.Banking_app.fx.application.port.in.usecase.QuoteUseCase;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Exchange rates and quotes, for any logged-in user (SecurityConfig: anyRequest().authenticated()).
 * GET /quote is informational (the rate right now); POST /quotes locks the sell rate for a short time, for one
 * transfer by the same user.
 */
@RestController
@RequestMapping("/api/fx")
public class FxController {

    private final GetExchangeRateUseCase getExchangeRateUseCase;
    private final QuoteUseCase quoteUseCase;
    private final LockQuoteUseCase lockQuoteUseCase;
    private final FxWebMapper mapper;

    public FxController(GetExchangeRateUseCase getExchangeRateUseCase, QuoteUseCase quoteUseCase,
                        LockQuoteUseCase lockQuoteUseCase, FxWebMapper mapper) {
        this.getExchangeRateUseCase = getExchangeRateUseCase;
        this.quoteUseCase = quoteUseCase;
        this.lockQuoteUseCase = lockQuoteUseCase;
        this.mapper = mapper;
    }

    /** GET /api/fx/rates → every offered currency with its buy and sell rate, sorted by code. */
    @GetMapping("/rates")
    public ResponseEntity<List<ExchangeRateResponseBody>> rates() {
        return ResponseEntity.ok(getExchangeRateUseCase.listRates().stream().map(mapper::toResponse).toList());
    }

    /** GET /api/fx/quote?currency=USD&amount=100.00&direction=PAY|RECEIVE (direction defaults to PAY). */
    @GetMapping("/quote")
    public ResponseEntity<QuoteResponseBody> quote(@RequestParam(required = false) String currency,
                                                   @RequestParam(required = false) String amount,
                                                   @RequestParam(required = false) String direction) {
        return ResponseEntity.ok(mapper.toResponse(quoteUseCase.quote(mapper.toQuery(currency, amount, direction))));
    }

    /** POST /api/fx/quotes {currency, amount} → 201: the sell rate locked for this user until expiresAt. */
    @PostMapping("/quotes")
    public ResponseEntity<LockedQuoteResponseBody> lockQuote(
            @Valid @RequestBody LockQuoteRequestBody body,
            @AuthenticationPrincipal(expression = "userId") String currentUserId) {
        var quote = lockQuoteUseCase.lock(mapper.toCommand(body, currentUserId));
        return ResponseEntity.status(HttpStatus.CREATED).body(mapper.toResponse(quote));
    }
}
