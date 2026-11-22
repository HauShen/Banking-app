package com.Banking_app.transaction.application.port.in.result;

import com.Banking_app.transaction.domain.models.balance.DailyBalance;

import java.util.List;

/**
 * An account's daily closing balances, oldest first, ending today.
 *
 * @param timeZone where the days begin and end (the bank's time zone), e.g. "Asia/Kuala_Lumpur"
 * @param days     starts at the account's opening day if it was opened inside the period
 */
public record BalanceHistoryView(String accountNumber, String currency, String timeZone, List<DailyBalance> days) {
}
