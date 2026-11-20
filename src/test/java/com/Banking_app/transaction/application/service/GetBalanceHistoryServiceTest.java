package com.Banking_app.transaction.application.service;

import com.Banking_app.shared.Requester;
import com.Banking_app.transaction.application.exception.TransactionAccessDeniedException;
import com.Banking_app.transaction.application.exception.TransactionAccountNotFoundException;
import com.Banking_app.transaction.application.port.in.query.BalanceHistoryQuery;
import com.Banking_app.transaction.application.port.in.result.BalanceHistoryView;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GetBalanceHistoryServiceTest {

    private static final ZoneId KL = ZoneId.of("Asia/Kuala_Lumpur");
    /** 20 Nov 2026, 01:00 in Kuala Lumpur — still 19 Nov in UTC. */
    private static final Instant NOW = Instant.parse("2026-11-19T17:00:00Z");

    private final FakeAccounts accounts = new FakeAccounts();
    private final FakeLedger ledger = new FakeLedger();
    private final GetBalanceHistoryService service =
            new GetBalanceHistoryService(accounts, ledger, Clock.fixed(NOW, ZoneOffset.UTC), KL);

    private static final Requester BOB = new Requester("bob", false);

    private BalanceHistoryView bobs(int days) {
        return service.get(new BalanceHistoryQuery("1000000002", days), BOB);
    }

    private static List<String> closings(BalanceHistoryView view) {
        return view.days().stream().map(d -> d.date() + " " + d.closingBalance().toPlainString()).toList();
    }

    @Test
    @DisplayName("the last N days up to today in the bank's time zone, starting from the last entry before them")
    void lastDaysFromTheLedger() {
        accounts.add(2L, "1000000002", "bob", false, "150.00");
        ledger.credit(2L, "80.00", "100.00", Instant.parse("2026-11-10T02:00:00Z"));   // before the period
        ledger.credit(2L, "60.00", "160.00", Instant.parse("2026-11-18T04:00:00Z"));   // 18 Nov 12:00 KL
        ledger.debit(2L, "10.00", "150.00", Instant.parse("2026-11-19T16:30:00Z"));    // 20 Nov 00:30 KL

        BalanceHistoryView view = bobs(4);

        assertThat(closings(view)).containsExactly(
                "2026-11-17 100.00", "2026-11-18 160.00", "2026-11-19 160.00", "2026-11-20 150.00");
        assertThat(view.accountNumber()).isEqualTo("1000000002");
        assertThat(view.currency()).isEqualTo("MYR");
        assertThat(view.timeZone()).isEqualTo("Asia/Kuala_Lumpur");
        assertThat(ledger.lastSince).isEqualTo(Instant.parse("2026-11-16T16:00:00Z"));   // 17 Nov 00:00 KL
    }

    @Test
    @DisplayName("an account opened inside the period starts at its opening day")
    void startsAtOpeningDay() {
        accounts.add(2L, "1000000002", "bob", false, "70.00", Instant.parse("2026-11-18T03:00:00Z"));
        ledger.credit(2L, "50.00", "70.00", Instant.parse("2026-11-19T03:00:00Z"));

        assertThat(closings(bobs(30))).containsExactly(
                "2026-11-18 20.00", "2026-11-19 70.00", "2026-11-20 70.00");
    }

    @Test
    @DisplayName("no entries at all: the current balance on every day since opening")
    void noEntries() {
        accounts.add(2L, "1000000002", "bob", false, "20.00", Instant.parse("2026-11-19T03:00:00Z"));

        assertThat(closings(bobs(7))).containsExactly("2026-11-19 20.00", "2026-11-20 20.00");
    }

    @Test
    @DisplayName("an admin may see any account; another customer gets 403")
    void ownerOrAdmin() {
        accounts.add(2L, "1000000002", "bob", false, "20.00");

        assertThat(service.get(new BalanceHistoryQuery("1000000002", 1), new Requester("admin-id", true)).days())
                .hasSize(1);
        assertThatThrownBy(() -> service.get(new BalanceHistoryQuery("1000000002", 1), new Requester("eve", false)))
                .isInstanceOf(TransactionAccessDeniedException.class)
                .hasMessage("You can only view the balance history of your own accounts");
    }

    @Test
    @DisplayName("unknown account → 404; days outside 1..366 → 400")
    void badRequests() {
        accounts.add(2L, "1000000002", "bob", false, "20.00");

        assertThatThrownBy(() -> service.get(new BalanceHistoryQuery("1000000009", 30), BOB))
                .isInstanceOf(TransactionAccountNotFoundException.class);
        for (int days : new int[]{0, -1, 367}) {
            assertThatThrownBy(() -> bobs(days)).as("days %d", days)
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("days must be between 1 and 366");
        }
        assertThat(bobs(366).days()).hasSize(366);
    }
}
