package com.Banking_app.transaction.application.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class AccountHistoryServiceTest {

    private final FakeTransactionStore store = new FakeTransactionStore();
    private final AccountHistoryService service = new AccountHistoryService(store);

    @Test
    @DisplayName("true for a sender, a recipient and a cash-deposit recipient; false for an untouched account")
    void hasTransactions() {
        Instant t = Instant.parse("2026-11-06T08:00:00Z");
        store.add("TXN-TRANSFER0001", 1L, 2L, "30.00", "k1", t);
        store.add("TXN-DEPOSIT00001", null, 3L, "50.00", "k2", t);

        assertThat(service.hasTransactions(1L)).isTrue();
        assertThat(service.hasTransactions(2L)).isTrue();
        assertThat(service.hasTransactions(3L)).isTrue();
        assertThat(service.hasTransactions(4L)).isFalse();
    }
}
