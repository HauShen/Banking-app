package com.Banking_app.fx.adapter.in.web.controllers;

import com.Banking_app.fx.adapter.in.web.dto.ExchangeRateResponseBody;
import com.Banking_app.fx.adapter.in.web.dto.QuoteResponseBody;
import com.Banking_app.fx.adapter.in.web.mapper.FxWebMapper;
import com.Banking_app.fx.application.port.in.usecase.GetExchangeRateUseCase;
import com.Banking_app.fx.application.port.in.usecase.QuoteUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Exchange rates and quotes, for any logged-in user (SecurityConfig: anyRequest().authenticated()).
 * Quotes are informational: a transfer uses the rate at the time it runs and records it.
 */
@RestController
@RequestMapping("/api/fx")
public class FxController {

    private final GetExchangeRateUseCase getExchangeRateUseCase;
    private final QuoteUseCase quoteUseCase;
    private final FxWebMapper mapper;

    public FxController(GetExchangeRateUseCase getExchangeRateUseCase, QuoteUseCase quoteUseCase, FxWebMapper mapper) {
        this.getExchangeRateUseCase = getExchangeRateUseCase;
        this.quoteUseCase = quoteUseCase;
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
}
