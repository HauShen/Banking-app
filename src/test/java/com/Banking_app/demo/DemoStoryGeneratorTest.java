package com.Banking_app.demo;

import com.Banking_app.transaction.domain.enums.TransactionType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** The demo story: deterministic, affordable, inside the accounts' lifetimes and believable. No database. */
class DemoStoryGeneratorTest {

    private static final ZoneId KL = ZoneId.of("Asia/Kuala_Lumpur");
    private static final Map<String, BigDecimal> BUY_RATES =
            Map.of("USD", new BigDecimal("4.1650"), "SGD", new BigDecimal("3.2100"));
    private static final LocalDate TODAY = LocalDate.of(2026, 11, 26);
    private static final Instant NOON = at(TODAY, 12, 0);

    private final DemoStoryGenerator generator = new DemoStoryGenerator(KL, BUY_RATES);

    @Test
    void sameSeedAndDay_giveTheSameStory() {
        assertThat(story(90, NOON, 42)).isEqualTo(story(90, NOON, 42));
    }

    @Test
    void anotherSeed_givesAnotherStory() {
        assertThat(story(90, NOON, 42)).isNotEqualTo(story(90, NOON, 43));
    }

    @Test
    void timeOfDay_onlyCutsOffTheEnd() {
        // The nightly reset runs at 04:00; a restart could happen at any time. Same day → same draws.
        List<DemoEvent> early = story(90, at(TODAY, 4, 0), 42).events();
        List<DemoEvent> late = story(90, at(TODAY, 23, 30), 42).events();

        assertThat(late.size()).isGreaterThanOrEqualTo(early.size());
        assertThat(late.subList(0, early.size())).isEqualTo(early);
    }

    @ParameterizedTest
    @ValueSource(strings = {"00:30", "04:00", "12:00", "23:59"})
    void everyEvent_isAtLeastOneMinuteBeforeNow(String time) {
        Instant now = TODAY.atTime(LocalTime.parse(time)).atZone(KL).toInstant();

        for (DemoEvent event : story(90, now, 42).events()) {
            assertThat(event.at()).isBeforeOrEqualTo(now.minusSeconds(60));
        }
    }

    @Test
    void events_areOldestFirst_eachOnItsOwnSecond_insideDaytime() {
        List<DemoEvent> events = story(90, NOON, 42).events();

        for (int i = 1; i < events.size(); i++) {
            assertThat(events.get(i).at()).isAfter(events.get(i - 1).at());
        }
        for (DemoEvent event : events) {
            LocalTime local = event.at().atZone(KL).toLocalTime();
            assertThat(local).isBetween(LocalTime.of(7, 0), LocalTime.of(22, 1));
        }
    }

    @Test
    void replayingFromTwentyRinggit_neverGoesBelowZero_andEndsAtTheFinalBalances() {
        for (int days : new int[] {7, 30, 90, 366}) {
            for (long seed = 1; seed <= 10; seed++) {
                DemoCast cast = DemoCast.standard(days);
                DemoStory story = generator.generate(cast, NOON, new Random(seed));

                Map<String, BigDecimal> balances = new HashMap<>();
                cast.accounts().forEach(a -> balances.put(a.accountNumber(), new BigDecimal("20.00")));
                for (DemoEvent event : story.events()) {
                    if (event.fromAccount() != null) {
                        BigDecimal after = balances.get(event.fromAccount()).subtract(event.amount());
                        assertThat(after.signum()).as("%s days, seed %d: %s", days, seed, event).isNotNegative();
                        balances.put(event.fromAccount(), after);
                    }
                    balances.merge(event.toAccount(), event.amount(), BigDecimal::add);
                }
                assertThat(balances).as("%s days, seed %d", days, seed).isEqualTo(story.finalBalances());
            }
        }
    }

    @Test
    void ahmadsCurrentAccount_canAlwaysPayTheWarmUpLargeTransfer() {
        // Step 5b sends RM 10,500 from it to raise a LARGE_TRANSFER alert; that must never fail.
        for (int days : new int[] {7, 8, 30, 90, 366}) {
            for (long seed = 1; seed <= 20; seed++) {
                BigDecimal balance = story(days, NOON, seed).finalBalance(DemoCast.AHMAD_CURRENT);
                assertThat(balance).as("%s days, seed %d", days, seed)
                        .isGreaterThanOrEqualTo(new BigDecimal("11000.00"));
            }
        }
    }

    @Test
    void accounts_startWithTheirOpeningDeposit_andHaveNothingOutsideTheirLifetime() {
        DemoCast cast = DemoCast.standard(90);
        DemoStory story = generator.generate(cast, NOON, new Random(42));

        for (DemoAccount account : cast.accounts()) {
            List<DemoEvent> events = story.eventsOf(account.accountNumber());
            if (account.accountNumber().equals(DemoCast.JASON_SAVINGS_NEW)) {
                assertThat(events).isEmpty();
                assertThat(story.finalBalance(account.accountNumber())).isEqualByComparingTo("20.00");
                continue;
            }
            DemoEvent first = events.get(0);
            assertThat(first.description()).isEqualTo("Opening deposit");
            assertThat(first.at().atZone(KL).toLocalDate()).isEqualTo(TODAY.minusDays(account.openedDaysAgo()));
            assertThat(events).allSatisfy(e -> assertThat(e.at()).isAfter(account.openedAt(TODAY, KL)));

            account.lockedDaysAgoIfLocked().ifPresent(lockedDaysAgo -> assertThat(events).allSatisfy(e ->
                    assertThat(e.at().atZone(KL).toLocalDate()).isBefore(TODAY.minusDays(lockedDaysAgo))));
        }
    }

    @Test
    void remittances_convertLikeTheFxModule() {
        List<DemoEvent> remittances = story(90, NOON, 42).events().stream()
                .filter(e -> e.type() == DemoEvent.Type.FOREIGN_REMITTANCE).toList();

        assertThat(remittances).isNotEmpty();
        for (DemoEvent event : remittances) {
            DemoEvent.Fx fx = event.fx();
            assertThat(fx.currency()).isIn("USD", "SGD");
            assertThat(fx.rate()).isEqualTo(BUY_RATES.get(fx.currency()));
            assertThat(event.amount()).isEqualTo(fx.originalAmount().multiply(fx.rate())
                    .setScale(2, RoundingMode.HALF_UP));
            assertThat(event.description()).contains(" — from ");
        }
    }

    @Test
    void ninetyDays_lookLikeRealLife() {
        // Window 29 Aug - 26 Nov 2026: three 25ths (salaries), three 1sts (rent).
        DemoStory story = story(90, NOON, 42);
        List<DemoEvent> events = story.events();

        assertThat(events.size()).isBetween(150, 500);
        assertThat(events).filteredOn(e -> e.description().startsWith("Salary — ")).hasSize(12);
        assertThat(events).filteredOn(e -> e.description().startsWith("Rent — ")).hasSizeGreaterThanOrEqualTo(6);
        assertThat(events).extracting(DemoEvent::description).contains("Rent — November");
        assertThat(events).filteredOn(e -> e.description().equals("Holiday trip share")).hasSize(1);
        assertThat(events).filteredOn(e -> e.description().equals("Move to current account")).isNotEmpty();
        assertThat(events).filteredOn(e -> e.description().equals("Cash deposit")).isNotEmpty();
        assertThat(events).filteredOn(e -> e.type() == DemoEvent.Type.TRANSFER).hasSizeGreaterThan(100);

        // weiming's chart has a shape: his balance goes up and down.
        BigDecimal balance = new BigDecimal("20.00");
        BigDecimal lowestAfterOpening = null;
        BigDecimal highest = balance;
        for (DemoEvent event : story.eventsOf(DemoCast.MAIN_SAVINGS)) {
            balance = DemoCast.MAIN_SAVINGS.equals(event.fromAccount())
                    ? balance.subtract(event.amount()) : balance.add(event.amount());
            highest = highest.max(balance);
            lowestAfterOpening = lowestAfterOpening == null ? balance : lowestAfterOpening.min(balance);
        }
        assertThat(highest).isGreaterThan(lowestAfterOpening);
    }

    @Test
    void missingBuyRate_isRejected() {
        assertThatThrownBy(() -> new DemoStoryGenerator(KL, Map.of("USD", new BigDecimal("4.1650"))))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("SGD");
    }

    @Test
    void demoEventType_hasTheSameNamesAsTransactionType() {
        assertThat(Arrays.stream(DemoEvent.Type.values()).map(Enum::name).toList())
                .isEqualTo(Arrays.stream(TransactionType.values()).map(Enum::name).toList());
    }

    @Test
    void events_followTheTransactionTableRules() {
        Instant t = NOON;
        BigDecimal ten = new BigDecimal("10.00");
        DemoEvent.Fx fx = new DemoEvent.Fx("USD", new BigDecimal("10.00"), new BigDecimal("4.1650"));

        assertThatThrownBy(() -> new DemoEvent(t, DemoEvent.Type.TRANSFER, null, "1111111111", ten, "x", null))
                .hasMessageContaining("sender");
        assertThatThrownBy(() -> new DemoEvent(t, DemoEvent.Type.CASH_DEPOSIT, "2222222222", "1111111111", ten,
                "x", null)).hasMessageContaining("sender");
        assertThatThrownBy(() -> DemoEvent.transfer(t, "1111111111", "1111111111", ten, "x"))
                .hasMessageContaining("differ");
        assertThatThrownBy(() -> new DemoEvent(t, DemoEvent.Type.FOREIGN_REMITTANCE, null, "1111111111", ten,
                "x", null)).hasMessageContaining("foreign-currency");
        assertThatThrownBy(() -> new DemoEvent(t, DemoEvent.Type.CASH_DEPOSIT, null, "1111111111", ten, "x", fx))
                .hasMessageContaining("foreign-currency");
        assertThatThrownBy(() -> DemoEvent.cashDeposit(t, "1111111111", new BigDecimal("10.005"), "x"))
                .hasMessageContaining("2 decimals");
        assertThatThrownBy(() -> DemoEvent.cashDeposit(t, "1111111111", BigDecimal.ZERO, "x"))
                .hasMessageContaining("more than 0");
        assertThatThrownBy(() -> DemoEvent.cashDeposit(t, "1111111111", ten, " "))
                .hasMessageContaining("Description");

        assertThat(DemoEvent.cashDeposit(t, "1111111111", new BigDecimal("5"), "x").amount()).isEqualTo("5.00");
    }

    private DemoStory story(int historyDays, Instant now, long seed) {
        return generator.generate(DemoCast.standard(historyDays), now, new Random(seed));
    }

    private static Instant at(LocalDate day, int hour, int minute) {
        return ZonedDateTime.of(day, LocalTime.of(hour, minute), KL).toInstant();
    }
}
