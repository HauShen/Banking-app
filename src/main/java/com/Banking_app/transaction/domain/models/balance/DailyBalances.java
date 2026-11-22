package com.Banking_app.transaction.domain.models.balance;

import com.Banking_app.transaction.domain.models.transaction.LedgerEntry;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Daily closing balances from an account's ledger. Every change of a balance is a ledger entry that records the
 * balance right after it, so a day's closing balance is the {@code balanceAfter} of the last entry posted that
 * day, and a day without entries closes at the previous day's balance. Nothing is stored for this.
 */
public final class DailyBalances {

    private DailyBalances() {
    }

    /**
     * One closing balance per day from {@code firstDay} to {@code lastDay}, both included.
     *
     * @param balanceAtStart the balance when {@code firstDay} begins
     * @param entries        the account's entries posted from the start of {@code firstDay}, oldest first
     *                       (entries after {@code lastDay} are ignored)
     * @param zone           where a day begins and ends
     * @return empty when {@code firstDay} is after {@code lastDay}
     */
    public static List<DailyBalance> closingBalances(BigDecimal balanceAtStart, List<LedgerEntry> entries,
                                                     LocalDate firstDay, LocalDate lastDay, ZoneId zone) {
        Objects.requireNonNull(balanceAtStart, "balanceAtStart");
        Objects.requireNonNull(entries, "entries");
        Objects.requireNonNull(zone, "zone");

        List<DailyBalance> days = new ArrayList<>();
        BigDecimal balance = balanceAtStart;
        int next = 0;
        for (LocalDate day = firstDay; !day.isAfter(lastDay); day = day.plusDays(1)) {
            Instant dayEnds = day.plusDays(1).atStartOfDay(zone).toInstant();
            while (next < entries.size() && entries.get(next).postedAt().isBefore(dayEnds)) {
                balance = entries.get(next).balanceAfter();
                next++;
            }
            days.add(new DailyBalance(day, balance));
        }
        return days;
    }
}
