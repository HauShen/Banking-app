package com.Banking_app.fx.application.port.in.result;

import com.Banking_app.fx.domain.enums.ConversionDirection;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * What a foreign amount is worth in MYR right now. Informational: the transfer uses the rate at the time it runs and
 * records it (locking a quote is Phase 5C).
 *
 * @param exchangeRate the rate used: the sell rate for PAY, the buy rate for RECEIVE
 * @param myrAmount    amount × rate, rounded HALF_UP to 2 decimals
 */
public record Quote(String currency,
                    BigDecimal amount,
                    ConversionDirection direction,
                    BigDecimal exchangeRate,
                    BigDecimal myrAmount,
                    Instant asOf) {
}
