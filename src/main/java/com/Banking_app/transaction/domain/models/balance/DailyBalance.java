package com.Banking_app.transaction.domain.models.balance;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

/** An account's balance at the end of one day (the bank's time zone): after the last entry posted that day. */
public record DailyBalance(LocalDate date, BigDecimal closingBalance) {

    public DailyBalance {
        Objects.requireNonNull(date, "date");
        Objects.requireNonNull(closingBalance, "closingBalance");
    }
}
