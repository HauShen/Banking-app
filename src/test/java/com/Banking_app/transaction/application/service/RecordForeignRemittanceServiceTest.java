package com.Banking_app.transaction.application.service;

import com.Banking_app.exception.ConflictException;
import com.Banking_app.transaction.application.exception.IdempotencyKeyMismatchException;
import com.Banking_app.transaction.application.exception.TransferAccountLockedException;
import com.Banking_app.transaction.application.port.in.command.RecordForeignRemittanceCommand;
import com.Banking_app.transaction.application.port.in.result.RemittanceResult;
import com.Banking_app.transaction.domain.enums.LedgerType;
import com.Banking_app.transaction.domain.enums.TransactionStatus;
import com.Banking_app.transaction.domain.enums.TransactionType;
import com.Banking_app.transaction.domain.models.transaction.Transaction;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Use-case tests with fake ports: no Spring, no database. Buy rates: USD 4.60, SGD 3.40, JPY 0.0290. */
class RecordForeignRemittanceServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-10T08:00:00Z");
    private static final Long BOB_ACC = 2L;
    private static final Long ALICE_ACC = 1L;

    private final FakeReceivingAccounts accounts = new FakeReceivingAccounts();
    private final FakeTransactionStore store = new FakeTransactionStore();
    private final FakeCurrencyConversions conversions = new FakeCurrencyConversions();
    private final RecordForeignRemittanceService service = new RecordForeignRemittanceService(accounts, store, store,
            new SequentialReferences(), conversions, Clock.fixed(NOW, ZoneOffset.UTC));

    RecordForeignRemittanceServiceTest() {
        accounts.add(ALICE_ACC, "1000000001", false, "120.00");
        accounts.add(BOB_ACC, "1000000002", false, "20.00");
    }

    private static RecordForeignRemittanceCommand toBob(String amount, String currency, String key) {
        return new RecordForeignRemittanceCommand("1000000002", currency, new BigDecimal(amount), "John Smith", key);
    }

    private void assertNothingHappened() {
        assertThat(accounts.receiveCalls).isZero();
        assertThat(store.saved).isEmpty();
        assertThat(accounts.balanceOf(BOB_ACC)).isEqualByComparingTo("20.00");
    }

    @Test
    @DisplayName("100.00 USD at the buy rate 4.60 credits RM 460.00 and stores a FOREIGN_REMITTANCE with one CREDIT")
    void receivesRemittance() {
        RemittanceResult result = service.receive(toBob("100.00", "USD", "gw-1"));

        assertThat(result.transactionReference()).isEqualTo("RMT-000000000001");
        assertThat(result.status()).isEqualTo(TransactionStatus.SUCCESS);
        assertThat(result.createdAt()).isEqualTo(NOW);
        assertThat(result.toAccountNumber()).isEqualTo("1000000002");
        assertThat(result.amount()).isEqualByComparingTo("460.00");
        assertThat(result.original().currency()).isEqualTo("USD");
        assertThat(result.original().amount()).isEqualByComparingTo("100.00");
        assertThat(result.original().exchangeRate()).isEqualByComparingTo("4.60");
        assertThat(result.duplicate()).isFalse();
        assertThat(accounts.balanceOf(BOB_ACC)).isEqualByComparingTo("480.00");

        Transaction tx = store.saved.get(0);
        assertThat(tx.getType()).isEqualTo(TransactionType.FOREIGN_REMITTANCE);
        assertThat(tx.getFromAccountId()).isNull();
        assertThat(tx.getToAccountId()).isEqualTo(BOB_ACC);
        assertThat(tx.getDescription()).isEqualTo("Foreign remittance from John Smith");
        assertThat(tx.getIdempotencyKey()).isEqualTo("gw-1");
        assertThat(tx.getLedgerEntries()).hasSize(1);
        assertThat(tx.getLedgerEntries().get(0).type()).isEqualTo(LedgerType.CREDIT);
        assertThat(tx.getLedgerEntries().get(0).amount()).isEqualByComparingTo("460.00");
        assertThat(tx.getLedgerEntries().get(0).balanceAfter()).isEqualByComparingTo("480.00");
    }

    @Test
    @DisplayName("JPY: 10000 at 0.0290 → RM 290.00")
    void jpy() {
        assertThat(service.receive(toBob("10000", "JPY", "gw-jpy")).amount()).isEqualByComparingTo("290.00");
    }

    @Nested
    @DisplayName("refused")
    class Refused {

        @Test
        @DisplayName("locked account → 400 'Selected account has been locked', nothing converted or credited")
        void locked() {
            accounts.add(3L, "1000000003", true, "20.00");

            assertThatThrownBy(() -> service.receive(new RecordForeignRemittanceCommand("1000000003", "USD",
                    BigDecimal.TEN, "John Smith", "gw-locked")))
                    .isInstanceOf(TransferAccountLockedException.class)
                    .hasMessage("Selected account has been locked");
            assertThat(conversions.calls).isZero();
            assertThat(accounts.receiveCalls).isZero();
        }

        @Test
        @DisplayName("unknown account → the account module's 404 passes through before anything else")
        void unknownAccount() {
            assertThatThrownBy(() -> service.receive(new RecordForeignRemittanceCommand("1000000009", "XYZ",
                    BigDecimal.TEN, "John Smith", "gw-unknown")))
                    .hasMessage("To account not found");
            assertThat(conversions.calls).isZero();
        }

        @Test
        @DisplayName("the fx module's refusal (e.g. currency not offered) passes through; nothing is credited")
        void refusedByFx() {
            assertThatThrownBy(() -> service.receive(toBob("10.00", "XYZ", "gw-xyz")))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Currency XYZ is not supported");
            assertNothingHappened();
        }

        @Test
        @DisplayName("losing the race to another request's insert with the same key → 409, the credit is rolled back")
        void lostRace() {
            store.nextSaveLosesTheRace = true;

            assertThatThrownBy(() -> service.receive(toBob("100.00", "USD", "gw-race")))
                    .isInstanceOf(IdempotencyKeyMismatchException.class);
            assertThat(store.saved).isEmpty();   // (the real transaction also rolls back the credit)
        }
    }

    @Nested
    @DisplayName("idempotency")
    class Idempotency {

        @Test
        @DisplayName("same key, same remittance → the original; not converted or credited again, even at a new rate")
        void duplicate() {
            RemittanceResult first = service.receive(toBob("100.00", "USD", "gw-1"));
            conversions.setBuyRate("USD", "4.65");

            RemittanceResult retry = service.receive(toBob("100", "USD", "gw-1"));   // 100 = 100.00

            assertThat(retry.duplicate()).isTrue();
            assertThat(retry.transactionReference()).isEqualTo(first.transactionReference());
            assertThat(retry.amount()).isEqualByComparingTo("460.00");
            assertThat(retry.original().exchangeRate()).isEqualByComparingTo("4.60");
            assertThat(conversions.calls).isEqualTo(1);
            assertThat(accounts.receiveCalls).isEqualTo(1);
            assertThat(accounts.balanceOf(BOB_ACC)).isEqualByComparingTo("480.00");
        }

        @Test
        @DisplayName("a retry finds the original even if the account was locked since")
        void duplicateBeforeLockedCheck() {
            accounts.add(3L, "1000000003", true, "20.00");
            store.save(Transaction.startForeignRemittance("RMT-ORIGINAL0001", 3L,
                            conversions.convertIncoming("USD", new BigDecimal("10.00")), "earlier", "gw-old", NOW)
                    .completeForeignRemittance(new BigDecimal("66.00"), NOW));

            RemittanceResult retry = service.receive(new RecordForeignRemittanceCommand("1000000003", "USD",
                    new BigDecimal("10.00"), "John Smith", "gw-old"));

            assertThat(retry.duplicate()).isTrue();
            assertThat(retry.transactionReference()).isEqualTo("RMT-ORIGINAL0001");
        }

        @Test
        @DisplayName("same key, another amount, currency or account → 409, nothing happens")
        void mismatch() {
            service.receive(toBob("100.00", "USD", "gw-1"));

            for (RecordForeignRemittanceCommand other : new RecordForeignRemittanceCommand[]{
                    toBob("101.00", "USD", "gw-1"),
                    toBob("100.00", "SGD", "gw-1"),
                    new RecordForeignRemittanceCommand("1000000001", "USD", new BigDecimal("100.00"), "John", "gw-1")}) {
                assertThatThrownBy(() -> service.receive(other))
                        .isInstanceOf(IdempotencyKeyMismatchException.class)
                        .isInstanceOf(ConflictException.class)   // → 409
                        .hasMessage("idempotencyKey has already been used for a different transaction");
            }
            assertThat(accounts.receiveCalls).isEqualTo(1);
        }

        @Test
        @DisplayName("the key of a transfer or a top-up → 409 (admins see every transaction, nothing is revealed)")
        void keyOfAnotherKind() {
            store.add("TXN-TRANSFER0001", ALICE_ACC, BOB_ACC, "460.00", "used-by-transfer", NOW.minusSeconds(60));
            store.add("CDM-DEPOSIT00001", null, BOB_ACC, "460.00", "used-by-top-up", NOW.minusSeconds(60));

            assertThatThrownBy(() -> service.receive(toBob("100.00", "USD", "used-by-transfer")))
                    .isInstanceOf(IdempotencyKeyMismatchException.class);
            assertThatThrownBy(() -> service.receive(toBob("100.00", "USD", "used-by-top-up")))
                    .isInstanceOf(IdempotencyKeyMismatchException.class);
            assertNothingHappened();
        }
    }
}
