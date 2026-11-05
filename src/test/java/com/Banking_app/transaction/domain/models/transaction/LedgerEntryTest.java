package com.Banking_app.transaction.domain.models.transaction;

import com.Banking_app.transaction.domain.enums.LedgerType;
import com.Banking_app.transaction.domain.exception.InvalidTransactionAmountException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LedgerEntryTest {

    private static final Instant NOW = Instant.parse("2026-11-05T10:00:00Z");

    @Test
    @DisplayName("debit and credit set the type; the amount stays positive for both")
    void debitAndCredit() {
        LedgerEntry debit = LedgerEntry.debit(1L, new BigDecimal("30.00"), new BigDecimal("90.00"), NOW);
        LedgerEntry credit = LedgerEntry.credit(2L, new BigDecimal("30.00"), new BigDecimal("50.00"), NOW);

        assertThat(debit.type()).isEqualTo(LedgerType.DEBIT);
        assertThat(debit.amount()).isPositive();
        assertThat(debit.id()).isNull();
        assertThat(credit.type()).isEqualTo(LedgerType.CREDIT);
        assertThat(credit.amount()).isPositive();
    }

    @Test
    @DisplayName("rejects zero, negative and missing amounts (the database has CHECK amount <> 0)")
    void rejectsNonPositiveAmount() {
        for (BigDecimal amount : new BigDecimal[]{BigDecimal.ZERO, new BigDecimal("-5"), null}) {
            assertThatThrownBy(() -> LedgerEntry.credit(1L, amount, BigDecimal.TEN, NOW))
                    .as("amount %s", amount)
                    .isInstanceOf(InvalidTransactionAmountException.class);
        }
    }

    @Test
    @DisplayName("requires account, type, balance after and posting time")
    void requiresFields() {
        assertThatThrownBy(() -> new LedgerEntry(null, null, LedgerType.CREDIT, BigDecimal.TEN, BigDecimal.TEN, NOW))
                .isInstanceOf(NullPointerException.class).hasMessage("accountId");
        assertThatThrownBy(() -> new LedgerEntry(null, 1L, null, BigDecimal.TEN, BigDecimal.TEN, NOW))
                .isInstanceOf(NullPointerException.class).hasMessage("type");
        assertThatThrownBy(() -> new LedgerEntry(null, 1L, LedgerType.CREDIT, BigDecimal.TEN, null, NOW))
                .isInstanceOf(NullPointerException.class).hasMessage("balanceAfter");
        assertThatThrownBy(() -> new LedgerEntry(null, 1L, LedgerType.CREDIT, BigDecimal.TEN, BigDecimal.TEN, null))
                .isInstanceOf(NullPointerException.class).hasMessage("postedAt");
    }
}
