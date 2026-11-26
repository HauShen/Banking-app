package com.Banking_app.demo;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.SqlParameterValue;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.sql.Types;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;

/**
 * Inserts the demo cast and its story into the database (plan step 4), straight into the tables with
 * {@link JdbcTemplate}: the use cases would stamp every row "now" and send every transfer to Kafka.
 *
 * <p>It writes exactly the rows the real code writes (see "Row rules" in the plan): accounts start at RM 20.00
 * without a ledger row, like {@code Account.open}; a transfer gets a DEBIT and a CREDIT ledger row, a deposit or
 * remittance one CREDIT row; {@code balance_after} is the running balance; {@code current_balance} ends at the last
 * {@code balance_after}. It writes no {@code outbox_events}, so the history never reaches Kafka.
 *
 * <p>All in one database transaction: either the whole demo exists or nothing does. The story is already
 * affordable ({@link DemoStoryGenerator} leaves out what a sender can't pay), so a balance below 0 here is a bug,
 * and the writer stops and rolls back instead of writing a ledger that doesn't add up.
 *
 * <p>References ("TXN-" / "CDM-" / "RMT-" + 12 upper-case hex, like {@code RandomTransactionReferenceAdapter}) and
 * idempotency keys come from a random generator seeded from {@code app.demo.random-seed}, so they repeat after every
 * reset. It doesn't check whether demo data already exists: the seeder does that (step 5), and the unique
 * constraints on usernames and account numbers would reject a second run anyway.
 */
public class DemoDataWriter {

    /** Mixed into the seed so references don't come from the same random sequence as the story. */
    private static final long REFERENCE_SEED_SALT = 0x5EED_4EF5L;
    private static final BigDecimal OPENING_BALANCE = new BigDecimal("20.00");

    private final JdbcTemplate jdbc;
    private final TransactionTemplate transaction;
    private final PasswordEncoder passwordEncoder;
    private final DemoProperties properties;
    private final ZoneId zone;

    public DemoDataWriter(JdbcTemplate jdbc, PlatformTransactionManager transactionManager,
                          PasswordEncoder passwordEncoder, DemoProperties properties, ZoneId zone) {
        this.jdbc = Objects.requireNonNull(jdbc, "jdbc");
        this.transaction = new TransactionTemplate(Objects.requireNonNull(transactionManager, "transactionManager"));
        this.passwordEncoder = Objects.requireNonNull(passwordEncoder, "passwordEncoder");
        this.properties = Objects.requireNonNull(properties, "properties");
        this.zone = Objects.requireNonNull(zone, "zone");
    }

    /**
     * @param now the same instant the story was generated with: opening days are counted back from its date
     */
    public DemoWriteResult write(DemoCast cast, DemoStory story, Instant now) {
        return transaction.execute(status -> insert(cast, story, LocalDate.ofInstant(now, zone)));
    }

    private DemoWriteResult insert(DemoCast cast, DemoStory story, LocalDate today) {
        Random random = new Random(properties.randomSeed() ^ REFERENCE_SEED_SALT);

        insertUsers(cast, today);
        Map<String, Long> accountIds = insertAccounts(cast, today);

        Map<String, BigDecimal> balances = new LinkedHashMap<>();
        cast.accounts().forEach(a -> balances.put(a.accountNumber(), OPENING_BALANCE));
        Set<String> references = new HashSet<>();
        Instant previous = null;
        int ledgerRows = 0;

        for (DemoEvent event : story.events()) {
            if (previous != null && !event.at().isAfter(previous)) {
                throw new IllegalStateException("Story events must be in strictly increasing time order: " + event);
            }
            previous = event.at();
            Long toId = idOf(accountIds, event.toAccount());
            Long fromId = event.fromAccount() == null ? null : idOf(accountIds, event.fromAccount());

            long transactionId = insertTransaction(event, fromId, toId, nextReference(event, random, references),
                    "demo-" + hex(random.nextLong(), 16));

            if (fromId != null) {
                BigDecimal after = balances.get(event.fromAccount()).subtract(event.amount());
                if (after.signum() < 0) {
                    throw new IllegalStateException("Story overdraws " + event.fromAccount() + ": " + event);
                }
                balances.put(event.fromAccount(), after);
                insertLedger(fromId, transactionId, "DEBIT", event, after);
                ledgerRows++;
            }
            BigDecimal after = balances.get(event.toAccount()).add(event.amount());
            balances.put(event.toAccount(), after);
            insertLedger(toId, transactionId, "CREDIT", event, after);
            ledgerRows++;
        }

        for (Map.Entry<String, BigDecimal> entry : balances.entrySet()) {
            BigDecimal expected = story.finalBalance(entry.getKey());
            if (entry.getValue().compareTo(expected) != 0) {
                throw new IllegalStateException("Balance of " + entry.getKey() + " is " + entry.getValue()
                        + " after the story, but the story says " + expected);
            }
            jdbc.update("UPDATE accounts SET current_balance = ? WHERE id = ?",
                    entry.getValue(), accountIds.get(entry.getKey()));
        }
        return new DemoWriteResult(cast.users().size(), cast.accounts().size(), story.events().size(), ledgerRows);
    }

    /**
     * One BCrypt hash per password (customers share one), because BCrypt is slow on purpose. A user is created half
     * an hour before their first account opens; a user without accounts (demoadmin) a day before the first account.
     */
    private void insertUsers(DemoCast cast, LocalDate today) {
        String customerHash = passwordEncoder.encode(properties.customerPassword());
        String adminHash = passwordEncoder.encode(properties.adminPassword());
        Instant firstOpening = cast.accounts().stream().map(a -> a.openedAt(today, zone))
                .min(Instant::compareTo).orElse(today.atStartOfDay(zone).toInstant());

        for (DemoUser user : cast.users()) {
            Instant createdAt = cast.accountsOf(user.username()).stream().map(a -> a.openedAt(today, zone))
                    .min(Instant::compareTo)
                    .map(opened -> opened.minus(Duration.ofMinutes(30)))
                    .orElse(firstOpening.minus(Duration.ofDays(1)));
            jdbc.update("""
                    INSERT INTO users (id, username, role, full_name, email, password, created_at, updated_at)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?)""",
                    user.id(), user.username(), user.role().name(), user.fullName(), user.email(),
                    user.isAdmin() ? adminHash : customerHash, utc(createdAt), utc(createdAt));
        }
    }

    private Map<String, Long> insertAccounts(DemoCast cast, LocalDate today) {
        Map<String, Long> ids = new HashMap<>();
        for (DemoAccount account : cast.accounts()) {
            Long id = jdbc.queryForObject("""
                    INSERT INTO accounts (user_id, account_number, account_type, account_status, account_currency,
                                          created_at, current_balance)
                    VALUES (?, ?, ?, ?, 'MYR', ?, ?)
                    RETURNING id""",
                    Long.class,
                    cast.user(account.ownerUsername()).id(), account.accountNumber(), account.type().name(),
                    account.status().name(), utc(account.openedAt(today, zone)), OPENING_BALANCE);
            ids.put(account.accountNumber(), id);
        }
        return ids;
    }

    private long insertTransaction(DemoEvent event, Long fromId, Long toId, String reference, String idempotencyKey) {
        DemoEvent.Fx fx = event.fx();
        Long id = jdbc.queryForObject("""
                INSERT INTO transactions (reference_number, from_account_id, to_account_id, amount, status,
                                          description, created_at, success_at, idempotency_key, transaction_type,
                                          original_currency, original_amount, exchange_rate)
                VALUES (?, ?, ?, ?, 'SUCCESS', ?, ?, ?, ?, ?, ?, ?, ?)
                RETURNING id""",
                Long.class,
                reference,
                new SqlParameterValue(Types.BIGINT, fromId),
                toId,
                event.amount(),
                event.description(),
                utc(event.at()),
                utc(event.at()),
                idempotencyKey,
                event.type().name(),
                new SqlParameterValue(Types.VARCHAR, fx == null ? null : fx.currency()),
                new SqlParameterValue(Types.NUMERIC, fx == null ? null : fx.originalAmount()),
                new SqlParameterValue(Types.NUMERIC, fx == null ? null : fx.rate()));
        return Objects.requireNonNull(id, "transaction id");
    }

    private void insertLedger(long accountId, long transactionId, String type, DemoEvent event,
                              BigDecimal balanceAfter) {
        jdbc.update("""
                INSERT INTO ledgers (account_id, transaction_id, ledger_type, amount, balance_after, created_at)
                VALUES (?, ?, ?, ?, ?, ?)""",
                accountId, transactionId, type, event.amount(), balanceAfter, utc(event.at()));
    }

    /** Same formats as RandomTransactionReferenceAdapter; drawn again in the (very unlikely) case of a repeat. */
    private static String nextReference(DemoEvent event, Random random, Set<String> used) {
        String prefix = switch (event.type()) {
            case TRANSFER -> "TXN-";
            case CASH_DEPOSIT -> "CDM-";
            case FOREIGN_REMITTANCE -> "RMT-";
        };
        String reference;
        do {
            reference = prefix + hex(random.nextLong(), 12);
        } while (!used.add(reference));
        return reference;
    }

    /** The lowest {@code digits} hex digits of {@code value}, upper case, zero-padded. */
    private static String hex(long value, int digits) {
        String all = String.format(Locale.ROOT, "%016X", value);
        return all.substring(16 - digits);
    }

    private static Long idOf(Map<String, Long> accountIds, String accountNumber) {
        Long id = accountIds.get(accountNumber);
        if (id == null) {
            throw new IllegalStateException("Story uses an account that isn't in the cast: " + accountNumber);
        }
        return id;
    }

    /** TIMESTAMPTZ parameter: the PostgreSQL driver takes OffsetDateTime (not Instant). */
    private static OffsetDateTime utc(Instant instant) {
        return instant.atOffset(ZoneOffset.UTC);
    }
}
