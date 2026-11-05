package com.Banking_app.transaction.application.service;

import com.Banking_app.transaction.application.port.in.command.RecordCashDepositCommand;
import com.Banking_app.transaction.domain.enums.LedgerType;
import com.Banking_app.transaction.domain.enums.TransactionStatus;
import com.Banking_app.transaction.domain.models.transaction.Transaction;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class RecordCashDepositServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-06T08:00:00Z");

    private final FakeTransactionStore store = new FakeTransactionStore();
    private final RecordCashDepositService service = new RecordCashDepositService(store, new SequentialReferences(),
            Clock.fixed(NOW, ZoneOffset.UTC));

    @Test
    @DisplayName("stores a SUCCESS cash deposit with one CREDIT entry and returns its reference")
    void recordsDeposit() {
        String reference = service.record(new RecordCashDepositCommand(2L, new BigDecimal("50.00"),
                new BigDecimal("70.00"), "Cash deposit", "topup-key"));

        assertThat(reference).isEqualTo("TXN-000000000001");
        assertThat(store.saved).hasSize(1);
        Transaction tx = store.saved.get(0);
        assertThat(tx.isCashDeposit()).isTrue();
        assertThat(tx.getToAccountId()).isEqualTo(2L);
        assertThat(tx.getStatus()).isEqualTo(TransactionStatus.SUCCESS);
        assertThat(tx.getCreatedAt()).isEqualTo(NOW);
        assertThat(tx.getSuccessAt()).isEqualTo(NOW);
        assertThat(tx.getIdempotencyKey()).isEqualTo("topup-key");
        assertThat(tx.getLedgerEntries()).singleElement().satisfies(credit -> {
            assertThat(credit.type()).isEqualTo(LedgerType.CREDIT);
            assertThat(credit.amount()).isEqualByComparingTo("50.00");
            assertThat(credit.balanceAfter()).isEqualByComparingTo("70.00");
        });
    }
}
