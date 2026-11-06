package com.Banking_app.transaction.application.service;

import com.Banking_app.transaction.application.port.in.command.RecordCashDepositCommand;
import com.Banking_app.transaction.domain.enums.LedgerType;
import com.Banking_app.transaction.domain.enums.TransactionStatus;
import com.Banking_app.transaction.domain.models.transaction.Transaction;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class RecordCashDepositServiceTest {

    private static final Instant DEPOSITED_AT = Instant.parse("2026-11-06T08:00:00Z");

    private final FakeTransactionStore store = new FakeTransactionStore();
    private final RecordCashDepositService service = new RecordCashDepositService(store, new SequentialReferences());

    @Test
    @DisplayName("stores a SUCCESS cash deposit with a CDM- reference and one CREDIT entry, all at the deposit time")
    void recordsDeposit() {
        String reference = service.record(new RecordCashDepositCommand(2L, new BigDecimal("50.00"),
                new BigDecimal("70.00"), DEPOSITED_AT, "Cash Deposit Machine top-up", "CDM-topup-key"));

        assertThat(reference).isEqualTo("CDM-000000000001");
        assertThat(store.saved).hasSize(1);
        Transaction tx = store.saved.get(0);
        assertThat(tx.isCashDeposit()).isTrue();
        assertThat(tx.getToAccountId()).isEqualTo(2L);
        assertThat(tx.getDescription()).isEqualTo("Cash Deposit Machine top-up");
        assertThat(tx.getStatus()).isEqualTo(TransactionStatus.SUCCESS);
        assertThat(tx.getCreatedAt()).isEqualTo(DEPOSITED_AT);
        assertThat(tx.getSuccessAt()).isEqualTo(DEPOSITED_AT);
        assertThat(tx.getIdempotencyKey()).isEqualTo("CDM-topup-key");
        assertThat(tx.getLedgerEntries()).singleElement().satisfies(credit -> {
            assertThat(credit.type()).isEqualTo(LedgerType.CREDIT);
            assertThat(credit.accountId()).isEqualTo(2L);
            assertThat(credit.amount()).isEqualByComparingTo("50.00");
            assertThat(credit.balanceAfter()).isEqualByComparingTo("70.00");
            assertThat(credit.postedAt()).isEqualTo(DEPOSITED_AT);
        });
    }
}
