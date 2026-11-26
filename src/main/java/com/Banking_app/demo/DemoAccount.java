package com.Banking_app.demo;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Objects;
import java.util.OptionalInt;

/**
 * One seeded account. Type and status are the demo package's own enums; their names match the account module's
 * {@code AccountType} and {@code AccountStatus} (DemoCastTest checks that), because they are written as text.
 *
 * <p>Dates are kept as "days before today", not as fixed dates, so the demo never gets older: the writer turns them
 * into instants on the day it runs.
 *
 * @param accountNumber  10 digits, from 1 000 000 000 like the numbers {@code OpenAccountService} makes
 * @param ownerUsername  the {@link DemoUser} who owns it
 * @param type           SAVINGS or CURRENT
 * @param status         ACTIVE or LOCKED
 * @param openedDaysAgo  opening day, in days before today (0 = today)
 * @param lockedDaysAgo  for a LOCKED account: the day it was locked (the story has no transactions after it);
 *                       -1 for an ACTIVE account. There is no column for it, only the story uses it.
 */
public record DemoAccount(String accountNumber, String ownerUsername, Type type, Status status,
                          int openedDaysAgo, int lockedDaysAgo) {

    public enum Type { SAVINGS, CURRENT }

    public enum Status { ACTIVE, LOCKED }

    /** Accounts are opened at this time (bank time zone) on their opening day, before any story event of that day. */
    public static final LocalTime OPENING_TIME = LocalTime.of(7, 0);

    private static final long MIN_NUMBER = 1_000_000_000L;
    private static final long MAX_NUMBER = 9_999_999_999L;

    public DemoAccount {
        Objects.requireNonNull(accountNumber, "accountNumber");
        Objects.requireNonNull(ownerUsername, "ownerUsername");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(status, "status");
        if (!accountNumber.matches("\\d{10}") || Long.parseLong(accountNumber) < MIN_NUMBER
                || Long.parseLong(accountNumber) > MAX_NUMBER) {
            throw new IllegalArgumentException("Account number must be 10 digits from 1000000000: " + accountNumber);
        }
        if (openedDaysAgo < 0) {
            throw new IllegalArgumentException("openedDaysAgo can't be negative: " + accountNumber);
        }
        if (status == Status.ACTIVE && lockedDaysAgo != -1) {
            throw new IllegalArgumentException("An ACTIVE account has no lock day: " + accountNumber);
        }
        if (status == Status.LOCKED && (lockedDaysAgo < 0 || lockedDaysAgo >= openedDaysAgo)) {
            throw new IllegalArgumentException(
                    "A LOCKED account is locked after it was opened and not in the future: " + accountNumber);
        }
    }

    public static DemoAccount active(String accountNumber, String ownerUsername, Type type, int openedDaysAgo) {
        return new DemoAccount(accountNumber, ownerUsername, type, Status.ACTIVE, openedDaysAgo, -1);
    }

    public static DemoAccount locked(String accountNumber, String ownerUsername, Type type,
                                     int openedDaysAgo, int lockedDaysAgo) {
        return new DemoAccount(accountNumber, ownerUsername, type, Status.LOCKED, openedDaysAgo, lockedDaysAgo);
    }

    /** {@code accounts.created_at}: {@link #OPENING_TIME} on the opening day, in the bank's time zone. */
    public Instant openedAt(LocalDate today, ZoneId zone) {
        return today.minusDays(openedDaysAgo).atTime(OPENING_TIME).atZone(zone).toInstant();
    }

    /** The lock day, for the story: no transactions on or after it. Empty for an ACTIVE account. */
    public OptionalInt lockedDaysAgoIfLocked() {
        return status == Status.LOCKED ? OptionalInt.of(lockedDaysAgo) : OptionalInt.empty();
    }
}
