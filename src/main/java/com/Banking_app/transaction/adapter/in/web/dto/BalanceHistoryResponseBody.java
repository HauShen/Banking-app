package com.Banking_app.transaction.adapter.in.web.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * GET /api/transfers/account/{accountNumber}/balance-history: one closing balance per day, oldest first, ending
 * today. Days are calendar days in {@code timeZone}.
 */
public record BalanceHistoryResponseBody(String accountNumber,
                                         String currency,
                                         String timeZone,
                                         List<Day> days) {

    /** {@code date} as "2026-11-20". */
    public record Day(LocalDate date, BigDecimal closingBalance) {
    }
}
