package com.Banking_app.transaction.domain.models.balance;

import com.Banking_app.transaction.domain.enums.LedgerType;
import com.Banking_app.transaction.domain.models.transaction.LedgerEntry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DailyBalancesTest {

    private static final ZoneId KL = ZoneId.of("Asia/Kuala_Lumpur");   // UTC+8
    private static final LocalDate NOV_1 = LocalDate.of(2026, 11, 1);

    private static LedgerEntry credit(String amount, String balanceAfter, String postedAt) {
        return new LedgerEntry(null, 1L, LedgerType.CREDIT, new BigDecimal(amount), new BigDecimal(balanceAfter),
                Instant.parse(postedAt));
    }

    private static LedgerEntry debit(String amount, String balanceAfter, String postedAt) {
        return new LedgerEntry(null, 1L, LedgerType.DEBIT, new BigDecimal(amount), new BigDecimal(balanceAfter),
                Instant.parse(postedAt));
    }

    private static List<String> closings(List<DailyBalance> days) {
        return days.stream().map(d -> d.date() + " " + d.closingBalance().toPlainString()).toList();
    }

    @Test
    @DisplayName("one balance per day; a day without entries closes at the day before's balance")
    void carriesBalanceOverQuietDays() {
        List<DailyBalance> days = DailyBalances.closingBalances(new BigDecimal("20.00"),
                List.of(credit("100.00", "120.00", "2026-11-02T03:00:00Z")),
                NOV_1, NOV_1.plusDays(3), KL);

        assertThat(closings(days)).containsExactly(
                "2026-11-01 20.00", "2026-11-02 120.00", "2026-11-03 120.00", "2026-11-04 120.00");
    }

    @Test
    @DisplayName("several entries on one day: the day closes at the last one")
    void lastEntryOfTheDayWins() {
        List<DailyBalance> days = DailyBalances.closingBalances(new BigDecimal("20.00"), List.of(
                        credit("100.00", "120.00", "2026-11-01T01:00:00Z"),
                        debit("30.00", "90.00", "2026-11-01T05:00:00Z"),
                        credit("10.00", "100.00", "2026-11-01T09:00:00Z")),
                NOV_1, NOV_1.plusDays(1), KL);

        assertThat(closings(days)).containsExactly("2026-11-01 100.00", "2026-11-02 100.00");
    }

    @Test
    @DisplayName("days end at midnight in the bank's time zone: 16:30 UTC is already the next day in Kuala Lumpur")
    void daysFollowTheBankTimeZone() {
        List<LedgerEntry> lateEvening = List.of(credit("50.00", "70.00", "2026-11-01T16:30:00Z"));

        assertThat(closings(DailyBalances.closingBalances(new BigDecimal("20.00"), lateEvening, NOV_1,
                NOV_1.plusDays(1), KL)))
                .containsExactly("2026-11-01 20.00", "2026-11-02 70.00");
        assertThat(closings(DailyBalances.closingBalances(new BigDecimal("20.00"), lateEvening, NOV_1,
                NOV_1.plusDays(1), ZoneId.of("UTC"))))
                .containsExactly("2026-11-01 70.00", "2026-11-02 70.00");
    }

    @Test
    @DisplayName("no entries: every day closes at the starting balance")
    void noEntries() {
        assertThat(closings(DailyBalances.closingBalances(new BigDecimal("20.00"), List.of(), NOV_1,
                NOV_1.plusDays(2), KL)))
                .containsExactly("2026-11-01 20.00", "2026-11-02 20.00", "2026-11-03 20.00");
    }

    @Test
    @DisplayName("entries after the last day are left out; a first day after the last day gives no days")
    void boundaries() {
        List<DailyBalance> days = DailyBalances.closingBalances(new BigDecimal("20.00"),
                List.of(credit("5.00", "25.00", "2026-11-03T02:00:00Z")), NOV_1, NOV_1.plusDays(1), KL);

        assertThat(closings(days)).containsExactly("2026-11-01 20.00", "2026-11-02 20.00");
        assertThat(DailyBalances.closingBalances(BigDecimal.ONE, List.of(), NOV_1.plusDays(1), NOV_1, KL)).isEmpty();
    }
}
