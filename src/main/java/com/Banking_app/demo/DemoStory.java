package com.Banking_app.demo;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * What {@link DemoStoryGenerator} produces: the events, oldest first with strictly increasing times, and each
 * account's balance after all of them. Every event in it is affordable: replaying it from RM 20.00 per account never
 * goes below 0, so the writer (step 4) can insert it as it is.
 *
 * @param events        oldest first; no two at the same instant
 * @param finalBalances account number → balance after the last event (RM 20.00 for an account without events);
 *                      the writer's {@code accounts.current_balance}
 */
public record DemoStory(List<DemoEvent> events, Map<String, BigDecimal> finalBalances) {

    public DemoStory {
        events = List.copyOf(events);
        finalBalances = Collections.unmodifiableMap(new LinkedHashMap<>(finalBalances));
    }

    public BigDecimal finalBalance(String accountNumber) {
        BigDecimal balance = finalBalances.get(accountNumber);
        if (balance == null) {
            throw new IllegalArgumentException("No demo account " + accountNumber);
        }
        return balance;
    }

    public List<DemoEvent> eventsOf(String accountNumber) {
        return events.stream().filter(e -> e.involves(accountNumber)).toList();
    }
}
