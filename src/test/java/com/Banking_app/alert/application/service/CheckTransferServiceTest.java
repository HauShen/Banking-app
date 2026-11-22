package com.Banking_app.alert.application.service;

import com.Banking_app.alert.application.port.in.command.CheckTransferCommand;
import com.Banking_app.alert.application.port.out.TransferLogPort;
import com.Banking_app.alert.domain.enums.AlertRule;
import com.Banking_app.alert.domain.models.AlertRules;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** The use case with fakes: no Spring, no database, no Kafka. Limits: RM 10,000; more than 5 in 10 minutes. */
class CheckTransferServiceTest {

    private static final Instant T0 = Instant.parse("2026-11-21T08:00:00Z");
    private static final String ALICE_ACC = "1000000001";

    private record Logged(UUID eventId, String account, Instant at) {
    }

    private final FakeAlertStore alerts = new FakeAlertStore();
    private final Set<UUID> processed = new HashSet<>();
    private final List<Logged> log = new ArrayList<>();

    private final TransferLogPort transferLog = new TransferLogPort() {
        @Override
        public void record(UUID eventId, String accountNumber, Instant occurredAt) {
            log.add(new Logged(eventId, accountNumber, occurredAt));
        }

        @Override
        public long countInWindow(String accountNumber, Instant after, Instant upTo) {
            return log.stream().filter(l -> l.account().equals(accountNumber) && l.at().isAfter(after)
                    && !l.at().isAfter(upTo)).count();
        }

        @Override
        public void forgetUpTo(String accountNumber, Instant cutoff) {
            log.removeIf(l -> l.account().equals(accountNumber) && !l.at().isAfter(cutoff));
        }
    };

    private final CheckTransferService service = new CheckTransferService(
            new AlertRules(new BigDecimal("10000.00"), 5, Duration.ofMinutes(10)),
            (consumer, eventId, at) -> processed.add(eventId), transferLog, alerts, alerts,
            Clock.fixed(T0, ZoneOffset.UTC));

    private static CheckTransferCommand transfer(String amount, Instant at) {
        return new CheckTransferCommand(UUID.randomUUID(), at, "TXN-" + at.getEpochSecond(), ALICE_ACC, "alice",
                "1000000002", new BigDecimal(amount));
    }

    /** {@code count} transfers of RM 10, one minute apart, starting at {@code start}. */
    private void transfers(int count, Instant start) {
        for (int i = 0; i < count; i++) {
            service.check(transfer("10.00", start.plus(Duration.ofMinutes(i))));
        }
    }

    @Test
    @DisplayName("a transfer of RM 12,000 → one LARGE_TRANSFER alert; RM 500 → none")
    void largeTransfer() {
        service.check(transfer("500.00", T0));
        service.check(transfer("12000.00", T0.plusSeconds(1)));

        assertThat(alerts.rows).singleElement().satisfies(a -> {
            assertThat(a.getRule()).isEqualTo(AlertRule.LARGE_TRANSFER);
            assertThat(a.getAmount()).isEqualByComparingTo("12000.00");
            assertThat(a.getAccountNumber()).isEqualTo(ALICE_ACC);
        });
    }

    @Test
    @DisplayName("5 transfers in 10 minutes → nothing; the 6th → one RAPID_TRANSFERS alert; the 7th and 8th → no more")
    void rapidTransfers() {
        transfers(5, T0);
        assertThat(alerts.rows).isEmpty();

        transfers(3, T0.plus(Duration.ofMinutes(5)));

        assertThat(alerts.ofRule(AlertRule.RAPID_TRANSFERS)).singleElement()
                .satisfies(a -> assertThat(a.getDetail()).startsWith("6 transfers"));
    }

    @Test
    @DisplayName("transfers spread out (one every 3 minutes) never reach 6 in a window, and the log keeps only the window")
    void spreadOut() {
        for (int i = 0; i < 10; i++) {
            service.check(transfer("10.00", T0.plus(Duration.ofMinutes(3L * i))));
        }

        assertThat(alerts.rows).isEmpty();
        assertThat(log).hasSizeLessThanOrEqualTo(4);   // only the last 10 minutes are remembered
    }

    @Test
    @DisplayName("a new burst after the window has passed → a new alert")
    void secondBurst() {
        transfers(6, T0);
        transfers(6, T0.plus(Duration.ofHours(1)));

        assertThat(alerts.ofRule(AlertRule.RAPID_TRANSFERS)).hasSize(2);
    }

    @Test
    @DisplayName("the same event delivered again → not counted twice, no second alert")
    void redelivery() {
        CheckTransferCommand large = transfer("15000.00", T0);

        service.check(large);
        service.check(large);

        assertThat(alerts.rows).hasSize(1);
        assertThat(log).hasSize(1);
    }

    @Test
    @DisplayName("a transfer that is both large and the 6th in a burst → two alerts, one per rule")
    void bothRules() {
        transfers(5, T0);

        service.check(transfer("20000.00", T0.plus(Duration.ofMinutes(5))));

        assertThat(alerts.rows).extracting(a -> a.getRule())
                .containsExactlyInAnyOrder(AlertRule.LARGE_TRANSFER, AlertRule.RAPID_TRANSFERS);
    }

    @Test
    @DisplayName("each account counts on its own")
    void perAccount() {
        for (int i = 0; i < 6; i++) {
            String account = "100000001" + i;
            service.check(new CheckTransferCommand(UUID.randomUUID(), T0.plusSeconds(i), "TXN-" + i, account, "u" + i,
                    "1000000099", new BigDecimal("10.00")));
        }

        assertThat(alerts.rows).isEmpty();
    }
}
