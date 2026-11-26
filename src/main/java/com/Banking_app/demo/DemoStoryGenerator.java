package com.Banking_app.demo;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;

import static com.Banking_app.demo.DemoCast.AHMAD_CURRENT;
import static com.Banking_app.demo.DemoCast.AHMAD_SAVINGS_LOCKED;
import static com.Banking_app.demo.DemoCast.MAIN_CURRENT;
import static com.Banking_app.demo.DemoCast.MAIN_SAVINGS;
import static com.Banking_app.demo.DemoCast.MEI_LING_SAVINGS;
import static com.Banking_app.demo.DemoCast.PRIYA_SAVINGS;

/**
 * Makes up the history of the demo accounts (plan step 3): opening deposits, salaries, rent, daily-life transfers
 * between the demo customers, own-account moves, foreign remittances and one large transfer.
 *
 * <p>Plain Java, no Spring and no database. The same cast, day and seed always give the same story, because
 * every random draw happens in a fixed order: day by day, template by template, account by account. The time of
 * day of {@code now} only cuts events off at the end; it never changes the draws.
 *
 * <p>Rules the rest of the seeding relies on:
 * <ul>
 *   <li>Every event is at least {@link #LATEST_BEFORE_NOW} before {@code now}, so the warm-up transactions
 *       (stamped "now", step 5b) are always the newest rows.</li>
 *   <li>Nothing happens on an account before or on its opening day except its opening deposit, nothing on a locked
 *       account from its lock day on, and nothing at all on jason.k's new account.</li>
 *   <li>Each account starts at RM 20.00 (like {@code Account.open}, without a ledger row). An event that the sender
 *       can't afford is left out, so no balance ever goes below 0.</li>
 *   <li>ahmad.f's current account never goes below {@link #AHMAD_RESERVE}: the warm-up sends RM 10,500 from it
 *       to raise a LARGE_TRANSFER alert, and must not fail.</li>
 * </ul>
 * The templates are written for {@link DemoCast#standard(int)}; accounts it doesn't know only receive money.
 */
public class DemoStoryGenerator {

    static final BigDecimal OPENING_BALANCE = new BigDecimal("20.00");
    static final BigDecimal AHMAD_RESERVE = new BigDecimal("11000.00");
    static final Duration LATEST_BEFORE_NOW = Duration.ofMinutes(1);

    /** Fixed, not random: the balances people see should look like the screenshots in the README. */
    private static final Map<String, BigDecimal> OPENING_DEPOSITS = Map.of(
            MAIN_SAVINGS, money("11000.00"),
            MAIN_CURRENT, money("1500.00"),
            MEI_LING_SAVINGS, money("3200.00"),
            AHMAD_CURRENT, money("13500.00"),     // above AHMAD_RESERVE from the first day
            AHMAD_SAVINGS_LOCKED, money("600.00"),
            PRIYA_SAVINGS, money("6500.00"));

    private record Salary(String account, BigDecimal amount, String employer) { }

    private static final List<Salary> SALARIES = List.of(
            new Salary(MAIN_SAVINGS, money("5200.00"), "Mutiara Software Sdn Bhd"),
            new Salary(MEI_LING_SAVINGS, money("4300.00"), "Cahaya Dental Clinic"),
            new Salary(AHMAD_CURRENT, money("7800.00"), "Jaya Logistics Sdn Bhd"),
            new Salary(PRIYA_SAVINGS, money("6100.00"), "Teratai Design Studio"));

    private record Rent(String tenant, String landlord, BigDecimal amount) { }

    /** ahmad.f is everyone's landlord, which also keeps his current account well above the reserve. */
    private static final List<Rent> RENTS = List.of(
            new Rent(MAIN_SAVINGS, AHMAD_CURRENT, money("1200.00")),
            new Rent(PRIYA_SAVINGS, AHMAD_CURRENT, money("950.00")),
            new Rent(MEI_LING_SAVINGS, AHMAD_CURRENT, money("850.00")));

    private record Sender(String name, String country, String currency, String purpose) { }

    private static final List<Sender> REMITTANCE_SENDERS = List.of(
            new Sender("Lee Chong", "Singapore", "SGD", "Salary bonus"),
            new Sender("Daniel Wong", "United States", "USD", "Freelance payment"),
            new Sender("Siti Rahman", "Singapore", "SGD", "Family support"));

    /** Accounts that spend in daily life (2-4 times a week each) and receive cash deposits. */
    private static final List<String> SPENDERS =
            List.of(MAIN_SAVINGS, MAIN_CURRENT, MEI_LING_SAVINGS, AHMAD_CURRENT, PRIYA_SAVINGS);
    private static final List<String> CASH_DEPOSITORS =
            List.of(MAIN_SAVINGS, MEI_LING_SAVINGS, AHMAD_CURRENT, PRIYA_SAVINGS);
    /** Where daily-life money goes: one account per person, never jason.k's new one. */
    private static final List<String> DAILY_LIFE_RECEIVERS =
            List.of(MAIN_SAVINGS, MEI_LING_SAVINGS, AHMAD_CURRENT, PRIYA_SAVINGS);
    private static final List<String> DAILY_LIFE = List.of(
            "Grab ride share", "Dinner split", "Badminton court", "Mamak supper", "Movie tickets",
            "Groceries split", "Coffee", "Durian feast", "Birthday gift share", "Petrol share");

    private static final double DAILY_LIFE_CHANCE = 0.4;    // per spender per day: about 2.8 a week
    private static final double CASH_DEPOSIT_CHANCE = 0.1;  // per account per day: about 3 a month

    private final ZoneId zone;
    private final Map<String, BigDecimal> buyRates;

    /**
     * @param zone     the bank's time zone (bank.time-zone): days, salaries on the 25th and times of day are local
     * @param buyRates MYR per 1 unit, the bank's buy rates (fx.rates.&lt;CCY&gt;.buy); USD and SGD are needed
     */
    public DemoStoryGenerator(ZoneId zone, Map<String, BigDecimal> buyRates) {
        this.zone = Objects.requireNonNull(zone, "zone");
        Objects.requireNonNull(buyRates, "buyRates");
        for (Sender sender : REMITTANCE_SENDERS) {
            BigDecimal rate = buyRates.get(sender.currency());
            if (rate == null || rate.signum() <= 0) {
                throw new IllegalArgumentException("A buy rate for " + sender.currency() + " is needed");
            }
        }
        this.buyRates = Map.copyOf(buyRates);
    }

    /** The currencies the remittances use: the constructor needs a buy rate for each. */
    public static Set<String> requiredCurrencies() {
        Set<String> currencies = new TreeSet<>();
        REMITTANCE_SENDERS.forEach(sender -> currencies.add(sender.currency()));
        return Set.copyOf(currencies);
    }

    public DemoStory generate(DemoCast cast, Instant now, Random random) {
        LocalDate today = LocalDate.ofInstant(now, zone);
        int windowDays = cast.accounts().stream().mapToInt(DemoAccount::openedDaysAgo).max().orElse(0) + 1;
        LocalDate firstDay = today.minusDays(windowDays - 1L);
        LocalDate largeTransferDay = today.minusDays(windowDays / 3 + 2L);

        // 1. Draw the whole window, today included, so the draws never depend on the time of day.
        List<DemoEvent> drafts = new ArrayList<>();
        for (LocalDate day = firstDay; !day.isAfter(today); day = day.plusDays(1)) {
            Day d = new Day(day, today, cast, random, drafts);
            d.openingDeposits();
            d.salaries();
            d.rents();
            d.ownAccountMoves();
            d.lockedSavingsTopUp();
            if (day.equals(largeTransferDay)) {
                d.largeTransfer();
            }
            d.remittances();
            d.cashDeposits();
            d.dailyLife();
        }

        // 2. Oldest first, each on its own second, then cut off at now - 1 minute.
        drafts.sort(Comparator.comparing(DemoEvent::at));   // stable: same instant keeps the drawing order
        List<DemoEvent> ordered = new ArrayList<>(drafts.size());
        for (DemoEvent event : drafts) {
            if (!ordered.isEmpty() && !event.at().isAfter(ordered.get(ordered.size() - 1).at())) {
                event = event.withTime(ordered.get(ordered.size() - 1).at().plusSeconds(1));
            }
            ordered.add(event);
        }
        Instant latest = now.minus(LATEST_BEFORE_NOW);

        // 3. Replay with running balances; leave out what a sender can't afford.
        Map<String, BigDecimal> balances = new LinkedHashMap<>();
        cast.accounts().forEach(a -> balances.put(a.accountNumber(), OPENING_BALANCE));
        List<DemoEvent> kept = new ArrayList<>();
        for (DemoEvent event : ordered) {
            if (event.at().isAfter(latest)) {
                break;
            }
            if (event.fromAccount() != null) {
                BigDecimal after = balances.get(event.fromAccount()).subtract(event.amount());
                if (after.compareTo(floorOf(event.fromAccount())) < 0) {
                    continue;
                }
                balances.put(event.fromAccount(), after);
            }
            balances.merge(event.toAccount(), event.amount(), BigDecimal::add);
            kept.add(event);
        }
        return new DemoStory(kept, balances);
    }

    private static BigDecimal floorOf(String accountNumber) {
        return AHMAD_CURRENT.equals(accountNumber) ? AHMAD_RESERVE : BigDecimal.ZERO;
    }

    /** The templates for one day. Each method draws its random numbers in a fixed order. */
    private final class Day {
        private final LocalDate day;
        private final LocalDate today;
        private final DemoCast cast;
        private final Random random;
        private final List<DemoEvent> out;

        Day(LocalDate day, LocalDate today, DemoCast cast, Random random, List<DemoEvent> out) {
            this.day = day;
            this.today = today;
            this.cast = cast;
            this.random = random;
            this.out = out;
        }

        void openingDeposits() {
            for (DemoAccount account : cast.accounts()) {
                BigDecimal deposit = OPENING_DEPOSITS.get(account.accountNumber());
                if (deposit != null && openingDay(account).equals(day)) {
                    out.add(DemoEvent.cashDeposit(time(9, 180), account.accountNumber(), deposit,
                            "Opening deposit"));
                }
            }
        }

        void salaries() {
            if (day.getDayOfMonth() != 25) {
                return;
            }
            for (Salary salary : SALARIES) {
                if (active(salary.account())) {
                    out.add(DemoEvent.cashDeposit(time(8, 120), salary.account(), salary.amount(),
                            "Salary — " + salary.employer()));
                }
            }
        }

        void rents() {
            if (day.getDayOfMonth() != 1) {
                return;
            }
            String month = day.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH);
            for (Rent rent : RENTS) {
                if (active(rent.tenant()) && active(rent.landlord())) {
                    out.add(DemoEvent.transfer(time(9, 180), rent.tenant(), rent.landlord(), rent.amount(),
                            "Rent — " + month));
                }
            }
        }

        /** weiming moves part of his salary to his current account, and tops up his savings mid-month. */
        void ownAccountMoves() {
            if (!active(MAIN_SAVINGS) || !active(MAIN_CURRENT)) {
                return;
            }
            if (day.getDayOfMonth() == 26) {
                out.add(DemoEvent.transfer(time(10, 600), MAIN_SAVINGS, MAIN_CURRENT, amount(800, 1500, 50),
                        "Move to current account"));
            } else if (day.getDayOfMonth() == 10) {
                out.add(DemoEvent.transfer(time(10, 600), MAIN_CURRENT, MAIN_SAVINGS, amount(100, 300, 10),
                        "Savings top-up"));
            }
        }

        /** Weekly top-up of ahmad.f's savings until it's locked: the locked account still has a history. */
        void lockedSavingsTopUp() {
            DemoAccount savings = cast.account(AHMAD_SAVINGS_LOCKED);
            long daysOpen = ChronoUnit.DAYS.between(openingDay(savings), day);
            if (daysOpen > 0 && daysOpen % 7 == 0 && active(AHMAD_SAVINGS_LOCKED) && active(AHMAD_CURRENT)) {
                out.add(DemoEvent.transfer(time(20, 120), AHMAD_CURRENT, AHMAD_SAVINGS_LOCKED,
                        amount(100, 250, 10), "Savings top-up"));
            }
        }

        /** One bigger payment, so weiming's balance chart has a visible drop. Under the RM 10,000 alert limit. */
        void largeTransfer() {
            if (active(MAIN_SAVINGS) && active(MEI_LING_SAVINGS)) {
                out.add(DemoEvent.transfer(time(14, 360), MAIN_SAVINGS, MEI_LING_SAVINGS, amount(1950, 2400, 10),
                        "Holiday trip share"));
            }
        }

        /** Money from abroad: weiming every 15th, priya.n on the 20th of even months. */
        void remittances() {
            if (day.getDayOfMonth() == 15) {
                remittanceTo(MAIN_SAVINGS);
            } else if (day.getDayOfMonth() == 20 && day.getMonthValue() % 2 == 0) {
                remittanceTo(PRIYA_SAVINGS);
            }
        }

        private void remittanceTo(String account) {
            if (!active(account)) {
                return;
            }
            Sender sender = REMITTANCE_SENDERS.get(random.nextInt(REMITTANCE_SENDERS.size()));
            BigDecimal original = amount(50, 400, 10);
            BigDecimal rate = buyRates.get(sender.currency());
            // Same conversion as the fx module's ExchangeRate: amount × rate, HALF_UP to 2 decimals.
            BigDecimal myr = original.multiply(rate).setScale(2, RoundingMode.HALF_UP);
            out.add(DemoEvent.remittance(time(11, 300), account, myr,
                    new DemoEvent.Fx(sender.currency(), original, rate),
                    sender.purpose() + " — from " + sender.name() + " (" + sender.country() + ")"));
        }

        void cashDeposits() {
            for (String account : CASH_DEPOSITORS) {
                if (active(account) && random.nextDouble() < CASH_DEPOSIT_CHANCE) {
                    out.add(DemoEvent.cashDeposit(time(10, 600), account, amount(50, 600, 10), "Cash deposit"));
                }
            }
        }

        void dailyLife() {
            for (String from : SPENDERS) {
                if (!active(from) || random.nextDouble() >= DAILY_LIFE_CHANCE) {
                    continue;
                }
                String owner = cast.account(from).ownerUsername();
                List<String> receivers = DAILY_LIFE_RECEIVERS.stream()
                        .filter(to -> !cast.account(to).ownerUsername().equals(owner))
                        .filter(this::active)
                        .toList();
                if (receivers.isEmpty()) {
                    continue;
                }
                String to = receivers.get(random.nextInt(receivers.size()));
                String description = DAILY_LIFE.get(random.nextInt(DAILY_LIFE.size()));
                out.add(DemoEvent.transfer(time(7, 900), from, to, cents(800, 12000, 10), description));
            }
        }

        /**
         * True from the day after opening until the day before the lock day. Unknown accounts (not in the cast) and
         * jason.k's account are never active, so they get no events.
         */
        private boolean active(String accountNumber) {
            if (DemoCast.JASON_SAVINGS_NEW.equals(accountNumber)) {
                return false;
            }
            DemoAccount account = cast.accounts().stream()
                    .filter(a -> a.accountNumber().equals(accountNumber)).findFirst().orElse(null);
            if (account == null || !day.isAfter(openingDay(account))) {
                return false;
            }
            return account.lockedDaysAgoIfLocked().stream()
                    .noneMatch(lockedDaysAgo -> !day.isBefore(today.minusDays(lockedDaysAgo)));
        }

        private LocalDate openingDay(DemoAccount account) {
            return today.minusDays(account.openedDaysAgo());
        }

        /** A random second within {@code spanMinutes} after {@code fromHour}:00 on this day, bank time. */
        private Instant time(int fromHour, int spanMinutes) {
            return day.atTime(LocalTime.of(fromHour, 0))
                    .plusMinutes(random.nextInt(spanMinutes))
                    .plusSeconds(random.nextInt(60))
                    .atZone(zone).toInstant();
        }

        /** Whole ringgit from min to max (inclusive) in steps of {@code step}. */
        private BigDecimal amount(int min, int max, int step) {
            return cents(min * 100, max * 100, step * 100);
        }

        /** Cents from min to max (inclusive) in steps of {@code step}, as RM with 2 decimals. */
        private BigDecimal cents(int min, int max, int step) {
            int steps = (max - min) / step;
            return BigDecimal.valueOf(min + (long) random.nextInt(steps + 1) * step, 2);
        }
    }

    private static BigDecimal money(String value) {
        return new BigDecimal(value);
    }
}
