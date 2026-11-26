package com.Banking_app.demo;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The people and accounts of the live demo (plan step 2). Hand-written, not generated: the login page, the README
 * and the visitor tour name them ("log in as weiming, send RM 50 to 5550 123 456"), so they must never change.
 * Only the opening days follow the configured history length.
 *
 * <p>Plain Java, no Spring and no database: the writer (step 4) inserts it, the story generator (step 3) uses it.
 * The constructor checks the rules the database would otherwise reject later: unique usernames, emails and account
 * numbers, and every account owned by a customer of the cast.
 */
public record DemoCast(List<DemoUser> users, List<DemoAccount> accounts) {

    /** The customer the demo tour and the login page start with. */
    public static final String TOUR_CUSTOMER = "weiming";

    public static final String MAIN_SAVINGS = "1234567890";      // weiming, the account the tour starts from
    public static final String MAIN_CURRENT = "9876543210";      // weiming
    public static final String MEI_LING_SAVINGS = "7781204459";
    public static final String AHMAD_CURRENT = "4402917736";     // sends the LARGE_TRANSFER in the warm-up
    public static final String AHMAD_SAVINGS_LOCKED = "4402917735";
    public static final String PRIYA_SAVINGS = "5550123456";
    public static final String JASON_SAVINGS_NEW = "3326518840"; // opened a few days ago, never used

    /** How many days ago jason.k opened his account: new, so the list and chart show an account without history. */
    static final int NEW_ACCOUNT_DAYS_AGO = 5;

    public DemoCast {
        users = List.copyOf(users);
        accounts = List.copyOf(accounts);

        Set<String> usernames = new HashSet<>();
        Set<String> emails = new HashSet<>();
        for (DemoUser user : users) {
            if (!usernames.add(user.username())) {
                throw new IllegalArgumentException("Duplicate username: " + user.username());
            }
            if (!emails.add(user.email())) {
                throw new IllegalArgumentException("Duplicate email: " + user.email());
            }
        }
        Set<String> numbers = new HashSet<>();
        for (DemoAccount account : accounts) {
            if (!numbers.add(account.accountNumber())) {
                throw new IllegalArgumentException("Duplicate account number: " + account.accountNumber());
            }
            DemoUser owner = users.stream().filter(u -> u.username().equals(account.ownerUsername())).findFirst()
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Account " + account.accountNumber() + " belongs to an unknown user: "
                                    + account.ownerUsername()));
            if (owner.isAdmin()) {
                throw new IllegalArgumentException("Admins have no accounts: " + account.accountNumber());
            }
        }
    }

    /**
     * The standard cast. Every account except jason.k's opens in the first days of the history window, so its
     * opening deposit is inside the window and the balance chart starts from the first day.
     *
     * @param historyDays days of history (app.demo.history-days, 7-366)
     */
    public static DemoCast standard(int historyDays) {
        if (historyDays < 7) {
            throw new IllegalArgumentException("historyDays must be at least 7: " + historyDays);
        }
        // Day 0 of the window is "historyDays - 1" days ago: a 90-day chart covers today and the 89 days before.
        int first = historyDays - 1;

        List<DemoUser> users = List.of(
                customer("weiming", "Lim Wei Ming"),
                customer("mei.ling", "Tan Mei Ling"),
                customer("ahmad.f", "Ahmad Faizal"),
                customer("priya.n", "Priya Nair"),
                customer("jason.k", "Jason Khoo"),
                new DemoUser("demoadmin", "Demo Admin", "demoadmin@banking.demo", DemoUser.Role.ADMIN));

        List<DemoAccount> accounts = List.of(
                DemoAccount.active(MAIN_SAVINGS, "weiming", DemoAccount.Type.SAVINGS, first),
                DemoAccount.active(MAIN_CURRENT, "weiming", DemoAccount.Type.CURRENT, first - 2),
                DemoAccount.active(MEI_LING_SAVINGS, "mei.ling", DemoAccount.Type.SAVINGS, first - 1),
                DemoAccount.active(AHMAD_CURRENT, "ahmad.f", DemoAccount.Type.CURRENT, first - 3),
                // Locked a third of the way back: two thirds of the window still show its earlier activity.
                // At least a day after opening, even with the shortest (7-day) history.
                DemoAccount.locked(AHMAD_SAVINGS_LOCKED, "ahmad.f", DemoAccount.Type.SAVINGS, first - 4,
                        Math.max(1, Math.min(historyDays / 3, first - 5))),
                DemoAccount.active(PRIYA_SAVINGS, "priya.n", DemoAccount.Type.SAVINGS, first - 2),
                DemoAccount.active(JASON_SAVINGS_NEW, "jason.k", DemoAccount.Type.SAVINGS, NEW_ACCOUNT_DAYS_AGO));

        return new DemoCast(users, accounts);
    }

    public DemoUser user(String username) {
        return users.stream().filter(u -> u.username().equals(username)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No demo user " + username));
    }

    public DemoAccount account(String accountNumber) {
        return accounts.stream().filter(a -> a.accountNumber().equals(accountNumber)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No demo account " + accountNumber));
    }

    public List<DemoAccount> accountsOf(String username) {
        return accounts.stream().filter(a -> a.ownerUsername().equals(username)).toList();
    }

    private static DemoUser customer(String username, String fullName) {
        return new DemoUser(username, fullName, username + "@banking.demo", DemoUser.Role.CUSTOMER);
    }
}
