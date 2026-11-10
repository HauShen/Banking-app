package com.Banking_app.transaction.application.service;

import com.Banking_app.transaction.application.exception.IdempotencyKeyConflictException;
import com.Banking_app.transaction.application.exception.IdempotencyKeyMismatchException;
import com.Banking_app.transaction.application.exception.QuoteRequiresForeignCurrencyException;
import com.Banking_app.transaction.application.exception.TransactionAccessDeniedException;
import com.Banking_app.transaction.application.exception.TransferAccountLockedException;
import com.Banking_app.transaction.application.port.in.command.TransferMoneyCommand;
import com.Banking_app.transaction.application.port.in.result.TransferResult;
import com.Banking_app.transaction.domain.enums.LedgerType;
import com.Banking_app.transaction.domain.enums.TransactionStatus;
import com.Banking_app.transaction.domain.exception.SameAccountTransferException;
import com.Banking_app.transaction.domain.models.transaction.Transaction;
import com.Banking_app.exception.ConflictException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Use-case tests with fake ports: no Spring, no database. Mirrors the HTTP tests in TransferSecurityTest at the
 * application level, plus the cases those don't reach (locked accounts, order of the checks).
 */
class TransferMoneyServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-06T08:00:00Z");
    private static final Long ALICE_ACC = 1L;
    private static final Long BOB_ACC = 2L;

    private final FakeAccounts accounts = new FakeAccounts();
    private final FakeTransactionStore store = new FakeTransactionStore();
    private final FakeCurrencyConversions conversions = new FakeCurrencyConversions();
    private final TransferMoneyService service = new TransferMoneyService(accounts, store, store,
            new SequentialReferences(), conversions, Clock.fixed(NOW, ZoneOffset.UTC));

    TransferMoneyServiceTest() {
        accounts.add(ALICE_ACC, "1000000001", "alice", false, "120.00");
        accounts.add(BOB_ACC, "1000000002", "bob", false, "20.00");
    }

    /** No currency given, as before foreign-currency transfers: MYR. */
    private static TransferMoneyCommand command(String user, String from, String to, String amount, String key) {
        return new TransferMoneyCommand(user, from, to, new BigDecimal(amount), null, "Dinner", key, null);
    }

    /** Alice → Bob, amount in {@code currency}. */
    private static TransferMoneyCommand aliceToBob(String amount, String currency, String key) {
        return new TransferMoneyCommand("alice", "1000000001", "1000000002", new BigDecimal(amount), currency,
                "Dinner", key, null);
    }

    /** Alice → Bob at a locked quote. */
    private static TransferMoneyCommand aliceToBobWithQuote(String amount, String currency, String key, String quoteId) {
        return new TransferMoneyCommand("alice", "1000000001", "1000000002", new BigDecimal(amount), currency,
                "Dinner", key, quoteId);
    }

    private void assertNothingHappened() {
        assertThat(accounts.moveCalls).isZero();
        assertThat(store.saved).isEmpty();
        assertThat(accounts.balanceOf(ALICE_ACC)).isEqualByComparingTo("120.00");
        assertThat(accounts.balanceOf(BOB_ACC)).isEqualByComparingTo("20.00");
    }

    @Test
    @DisplayName("the owner's transfer moves the money and stores a SUCCESS transaction with DEBIT + CREDIT")
    void completesTransfer() {
        TransferResult result = service.transfer(command("alice", "1000000001", "1000000002", "30.00", "k1"));

        assertThat(result.transactionReference()).isEqualTo("TXN-000000000001");
        assertThat(result.status()).isEqualTo(TransactionStatus.SUCCESS);
        assertThat(result.createdAt()).isEqualTo(NOW);
        assertThat(result.duplicate()).isFalse();

        assertThat(accounts.balanceOf(ALICE_ACC)).isEqualByComparingTo("90.00");
        assertThat(accounts.balanceOf(BOB_ACC)).isEqualByComparingTo("50.00");

        assertThat(store.saved).hasSize(1);
        Transaction tx = store.saved.get(0);
        assertThat(tx.getFromAccountId()).isEqualTo(ALICE_ACC);
        assertThat(tx.getToAccountId()).isEqualTo(BOB_ACC);
        assertThat(tx.getIdempotencyKey()).isEqualTo("k1");
        assertThat(tx.getDescription()).isEqualTo("Dinner");
        assertThat(tx.getSuccessAt()).isEqualTo(NOW);
        assertThat(tx.getLedgerEntries()).hasSize(2);
        assertThat(tx.getLedgerEntries().get(0).type()).isEqualTo(LedgerType.DEBIT);
        assertThat(tx.getLedgerEntries().get(0).balanceAfter()).isEqualByComparingTo("90.00");
        assertThat(tx.getLedgerEntries().get(1).type()).isEqualTo(LedgerType.CREDIT);
        assertThat(tx.getConversion()).isEmpty();
        assertThat(conversions.calls).isZero();
        assertThat(result.amount()).isEqualByComparingTo("30.00");
        assertThat(result.original()).isNull();
        assertThat(tx.getLedgerEntries().get(1).balanceAfter()).isEqualByComparingTo("50.00");
    }

    @Test
    @DisplayName("same account on both sides → 400 before anything is locked")
    void sameAccount() {
        assertThatThrownBy(() -> service.transfer(command("alice", "1000000001", "1000000001", "10", "k1")))
                .isInstanceOf(SameAccountTransferException.class)
                .hasMessage("Cannot transfer to the same account");
        assertThat(accounts.lockCalls).isZero();
        assertNothingHappened();
    }

    @Nested
    @DisplayName("ownership")
    class Ownership {

        @Test
        @DisplayName("someone else's account → 403 after locking, nothing moves")
        void notOwner() {
            assertThatThrownBy(() -> service.transfer(command("bob", "1000000001", "1000000002", "10", "k1")))
                    .isInstanceOf(TransactionAccessDeniedException.class)
                    .hasMessage("You can only transfer from your own accounts");
            assertThat(accounts.lockCalls).isEqualTo(1);
            assertNothingHappened();
        }

        @Test
        @DisplayName("admins are not the owner either → 403")
        void adminIsNotOwner() {
            assertThatThrownBy(() -> service.transfer(command("admin-id", "1000000001", "1000000002", "10", "k1")))
                    .isInstanceOf(TransactionAccessDeniedException.class);
            assertNothingHappened();
        }
    }

    @Nested
    @DisplayName("idempotency")
    class Idempotency {

        @Test
        @DisplayName("same key from the same account → the original transfer, nothing moves again")
        void duplicateReturnsOriginal() {
            Instant earlier = NOW.minusSeconds(60);
            store.add("TXN-ORIGINAL0001", ALICE_ACC, BOB_ACC, "30.00", "k1", earlier);

            TransferResult result = service.transfer(command("alice", "1000000001", "1000000002", "30.00", "k1"));

            assertThat(result.duplicate()).isTrue();
            assertThat(result.transactionReference()).isEqualTo("TXN-ORIGINAL0001");
            assertThat(result.status()).isEqualTo(TransactionStatus.SUCCESS);
            assertThat(result.createdAt()).isEqualTo(earlier);
            assertNothingHappened();
        }

        @Test
        @DisplayName("the key of another account's transfer → 400, that transfer is not revealed")
        void keyOfAnotherAccount() {
            store.add("TXN-BOBS00000001", BOB_ACC, ALICE_ACC, "5.00", "bobs-key", NOW.minusSeconds(60));

            assertThatThrownBy(() -> service.transfer(command("alice", "1000000001", "1000000002", "10", "bobs-key")))
                    .isInstanceOf(IdempotencyKeyConflictException.class)
                    .hasMessage("idempotencyKey has already been used for a different transfer");
            assertNothingHappened();
        }

        @Test
        @DisplayName("the key of a cash deposit → 400 (a deposit has no from-account)")
        void keyOfCashDeposit() {
            store.add("TXN-DEPOSIT00001", null, ALICE_ACC, "50.00", "deposit-key", NOW.minusSeconds(60));

            assertThatThrownBy(() -> service.transfer(command("alice", "1000000001", "1000000002", "10", "deposit-key")))
                    .isInstanceOf(IdempotencyKeyConflictException.class);
            assertNothingHappened();
        }

        @Test
        @DisplayName("same key from the same account but a different amount → 409, nothing moves")
        void sameKeyDifferentAmount() {
            store.add("TXN-ORIGINAL0001", ALICE_ACC, BOB_ACC, "30.00", "k1", NOW.minusSeconds(60));

            assertThatThrownBy(() -> service.transfer(command("alice", "1000000001", "1000000002", "31.00", "k1")))
                    .isInstanceOf(IdempotencyKeyMismatchException.class)
                    .isInstanceOf(ConflictException.class)   // → 409
                    .hasMessage("idempotencyKey has already been used for a transfer with a different amount or recipient");
            assertNothingHappened();
        }

        @Test
        @DisplayName("same key from the same account but a different recipient → 409, nothing moves")
        void sameKeyDifferentRecipient() {
            accounts.add(3L, "1000000003", "carol", false, "20.00");
            store.add("TXN-ORIGINAL0001", ALICE_ACC, BOB_ACC, "30.00", "k1", NOW.minusSeconds(60));

            assertThatThrownBy(() -> service.transfer(command("alice", "1000000001", "1000000003", "30.00", "k1")))
                    .isInstanceOf(IdempotencyKeyMismatchException.class);
            assertNothingHappened();
        }

        @Test
        @DisplayName("same amount written differently (30 vs 30.00) is the same transfer → duplicate")
        void sameAmountDifferentScale() {
            store.add("TXN-ORIGINAL0001", ALICE_ACC, BOB_ACC, "30.00", "k1", NOW.minusSeconds(60));

            TransferResult result = service.transfer(command("alice", "1000000001", "1000000002", "30", "k1"));

            assertThat(result.duplicate()).isTrue();
        }

        @Test
        @DisplayName("losing the race to another account's insert with the same key → the same 400 as a later request")
        void lostRaceOnInsert() {
            store.nextSaveLosesTheRace = true;

            assertThatThrownBy(() -> service.transfer(command("alice", "1000000001", "1000000002", "30.00", "k1")))
                    .isInstanceOf(IdempotencyKeyConflictException.class)
                    .hasMessage("idempotencyKey has already been used for a different transfer");
            assertThat(store.saved).isEmpty();   // (the real transaction also rolls back the moved money)
        }

        @Test
        @DisplayName("ownership is checked first: a stranger with the owner's key gets 403, not the transfer")
        void ownershipBeforeIdempotency() {
            store.add("TXN-ORIGINAL0001", ALICE_ACC, BOB_ACC, "30.00", "k1", NOW.minusSeconds(60));

            assertThatThrownBy(() -> service.transfer(command("bob", "1000000001", "1000000002", "30.00", "k1")))
                    .isInstanceOf(TransactionAccessDeniedException.class);
        }
    }

    @Nested
    @DisplayName("locked accounts")
    class Locked {

        @Test
        @DisplayName("locked from-account → 400 with the old message")
        void lockedFrom() {
            accounts.add(3L, "1000000003", "carol", true, "500.00");

            assertThatThrownBy(() -> service.transfer(command("carol", "1000000003", "1000000002", "10", "k1")))
                    .isInstanceOf(TransferAccountLockedException.class)
                    .hasMessage("Cannot process transfer with an locked account.");
            assertNothingHappened();
        }

        @Test
        @DisplayName("locked to-account → 400 with the old message")
        void lockedTo() {
            accounts.add(3L, "1000000003", "carol", true, "500.00");

            assertThatThrownBy(() -> service.transfer(command("alice", "1000000001", "1000000003", "10", "k1")))
                    .isInstanceOf(TransferAccountLockedException.class)
                    .hasMessage("Selected account has been locked");
            assertNothingHappened();
        }

        @Test
        @DisplayName("a retry of an earlier transfer still gets the original, even if the account is locked now")
        void duplicateBeforeLockedCheck() {
            accounts.add(3L, "1000000003", "carol", true, "500.00");
            store.add("TXN-ORIGINAL0003", 3L, BOB_ACC, "10.00", "k3", NOW.minusSeconds(60));

            TransferResult result = service.transfer(command("carol", "1000000003", "1000000002", "10", "k3"));

            assertThat(result.duplicate()).isTrue();
            assertThat(result.transactionReference()).isEqualTo("TXN-ORIGINAL0003");
        }
    }

    @Nested
    @DisplayName("paying in a foreign currency")
    class ForeignCurrency {

        @Test
        @DisplayName("20.00 USD at the sell rate 4.75 moves RM 95.00; the transaction keeps currency, amount and rate")
        void usd() {
            TransferResult result = service.transfer(aliceToBob("20.00", "USD", "k-usd"));

            assertThat(accounts.balanceOf(ALICE_ACC)).isEqualByComparingTo("25.00");
            assertThat(accounts.balanceOf(BOB_ACC)).isEqualByComparingTo("115.00");
            assertThat(result.amount()).isEqualByComparingTo("95.00");
            assertThat(result.original().currency()).isEqualTo("USD");
            assertThat(result.original().amount()).isEqualByComparingTo("20.00");
            assertThat(result.original().exchangeRate()).isEqualByComparingTo("4.75");

            Transaction tx = store.saved.get(0);
            assertThat(tx.getAmount()).isEqualByComparingTo("95.00");
            assertThat(tx.getLedgerEntries().get(0).amount()).isEqualByComparingTo("95.00");   // ledger is MYR
            assertThat(tx.getConversion().orElseThrow().originalAmount()).isEqualByComparingTo("20.00");
        }

        @Test
        @DisplayName("currency MYR is a plain MYR transfer: nothing is converted")
        void myr() {
            TransferResult result = service.transfer(aliceToBob("30.00", "MYR", "k-myr"));

            assertThat(result.amount()).isEqualByComparingTo("30.00");
            assertThat(result.original()).isNull();
            assertThat(conversions.calls).isZero();
            assertThat(store.saved.get(0).getConversion()).isEmpty();
        }

        @Test
        @DisplayName("the fx module's refusal (e.g. currency not offered) passes through; nothing moves")
        void refusedByFx() {
            assertThatThrownBy(() -> service.transfer(aliceToBob("10.00", "XYZ", "k-xyz")))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Currency XYZ is not supported");
            assertNothingHappened();
        }

        @Test
        @DisplayName("the minimum balance applies to the converted amount: 22.00 USD = RM 104.50 leaves 15.50 → 400")
        void minimumBalanceOnConvertedAmount() {
            assertThatThrownBy(() -> service.transfer(aliceToBob("22.00", "USD", "k-big")))
                    .hasMessage("Insufficient balance. Minimum balance of $20.00 must be maintained.");
            assertThat(store.saved).isEmpty();
            assertThat(accounts.balanceOf(ALICE_ACC)).isEqualByComparingTo("120.00");
        }

        @Test
        @DisplayName("converted only after the checks: not the owner → 403, locked → 400, no rate asked for")
        void convertsAfterTheChecks() {
            accounts.add(3L, "1000000003", "carol", true, "500.00");

            assertThatThrownBy(() -> service.transfer(new TransferMoneyCommand("bob", "1000000001", "1000000002",
                    BigDecimal.TEN, "XYZ", "Dinner", "k1", null)))
                    .isInstanceOf(TransactionAccessDeniedException.class);
            assertThatThrownBy(() -> service.transfer(new TransferMoneyCommand("carol", "1000000003", "1000000002",
                    BigDecimal.TEN, "XYZ", "Dinner", "k2", null)))
                    .isInstanceOf(TransferAccountLockedException.class);
            assertThat(conversions.calls).isZero();
        }

        @Nested
        @DisplayName("idempotency")
        class ForeignIdempotency {

            @Test
            @DisplayName("a retry after the rate changed is still the same request → the original, not converted again")
            void retryAfterRateChange() {
                TransferResult first = service.transfer(aliceToBob("20.00", "USD", "k-fx"));
                conversions.setSellRate("USD", "4.80");

                TransferResult retry = service.transfer(aliceToBob("20.00", "USD", "k-fx"));

                assertThat(retry.duplicate()).isTrue();
                assertThat(retry.transactionReference()).isEqualTo(first.transactionReference());
                assertThat(retry.amount()).isEqualByComparingTo("95.00");                 // the original MYR amount
                assertThat(retry.original().exchangeRate()).isEqualByComparingTo("4.75"); // and the original rate
                assertThat(conversions.calls).isEqualTo(1);
                assertThat(accounts.moveCalls).isEqualTo(1);
            }

            @Test
            @DisplayName("same key, a different original amount or currency → 409")
            void differentAmountOrCurrency() {
                service.transfer(aliceToBob("20.00", "USD", "k-fx"));

                assertThatThrownBy(() -> service.transfer(aliceToBob("21.00", "USD", "k-fx")))
                        .isInstanceOf(IdempotencyKeyMismatchException.class);
                assertThatThrownBy(() -> service.transfer(aliceToBob("20.00", "SGD", "k-fx")))
                        .isInstanceOf(IdempotencyKeyMismatchException.class);
                assertThat(accounts.moveCalls).isEqualTo(1);
            }

            @Test
            @DisplayName("MYR and foreign never match: 47.50 MYR is not a retry of 10.00 USD, 10.00 USD not of 10.00 MYR")
            void myrAndForeignNeverMatch() {
                service.transfer(aliceToBob("10.00", "USD", "k-fx"));    // RM 47.50
                service.transfer(aliceToBob("10.00", "MYR", "k-myr"));

                assertThatThrownBy(() -> service.transfer(aliceToBob("47.50", "MYR", "k-fx")))
                        .isInstanceOf(IdempotencyKeyMismatchException.class);
                assertThatThrownBy(() -> service.transfer(aliceToBob("47.50", null, "k-fx")))
                        .isInstanceOf(IdempotencyKeyMismatchException.class);
                assertThatThrownBy(() -> service.transfer(aliceToBob("10.00", "USD", "k-myr")))
                        .isInstanceOf(IdempotencyKeyMismatchException.class);
            }

            @Test
            @DisplayName("a MYR retry without the currency field matches a MYR transfer that had it (absent = MYR)")
            void absentCurrencyIsMyr() {
                service.transfer(aliceToBob("30.00", "MYR", "k-myr"));

                assertThat(service.transfer(aliceToBob("30.00", null, "k-myr")).duplicate()).isTrue();
            }
        }
    }

    @Nested
    @DisplayName("paying at a locked quote")
    class LockedQuote {

        private static final String QUOTE = "3f9a0c12-b7d4-4e5f-8a6b-1c2d3e4f5a6b";

        @Test
        @DisplayName("20.00 USD at the locked 4.70 (today 4.75) moves RM 94.00; today's rate isn't asked for")
        void usesLockedRate() {
            conversions.lockQuote(QUOTE, "alice", "USD", "20.00", "4.70");

            TransferResult result = service.transfer(aliceToBobWithQuote("20.00", "USD", "k-q", QUOTE));

            assertThat(result.amount()).isEqualByComparingTo("94.00");
            assertThat(result.original().exchangeRate()).isEqualByComparingTo("4.70");
            assertThat(accounts.balanceOf(ALICE_ACC)).isEqualByComparingTo("26.00");
            assertThat(conversions.quoteCalls).isEqualTo(1);
            assertThat(conversions.calls).isZero();
        }

        @Test
        @DisplayName("a quote with a MYR transfer (or no currency) → 400 before anything moves")
        void quoteWithMyr() {
            assertThatThrownBy(() -> service.transfer(aliceToBobWithQuote("20.00", "MYR", "k-myr", QUOTE)))
                    .isInstanceOf(QuoteRequiresForeignCurrencyException.class)
                    .isInstanceOf(IllegalArgumentException.class)   // → 400
                    .hasMessage("quoteId can only be used with a foreign currency");
            assertThatThrownBy(() -> service.transfer(aliceToBobWithQuote("20.00", null, "k-none", QUOTE)))
                    .isInstanceOf(QuoteRequiresForeignCurrencyException.class);
            assertNothingHappened();
            assertThat(conversions.quoteCalls).isZero();
        }

        @Test
        @DisplayName("fx's refusal (e.g. the quote was used) passes through; nothing moves")
        void refusedQuote() {
            conversions.lockQuote(QUOTE, "alice", "USD", "20.00", "4.70");
            service.transfer(aliceToBobWithQuote("20.00", "USD", "k-1", QUOTE));

            assertThatThrownBy(() -> service.transfer(aliceToBobWithQuote("20.00", "USD", "k-2", QUOTE)))
                    .hasMessage("The quote has already been used");
            assertThat(accounts.moveCalls).isEqualTo(1);
        }

        @Test
        @DisplayName("a retry with the same key returns the original before the (used) quote is looked at")
        void retryAfterQuoteUsed() {
            conversions.lockQuote(QUOTE, "alice", "USD", "20.00", "4.70");
            TransferResult first = service.transfer(aliceToBobWithQuote("20.00", "USD", "k-q", QUOTE));

            TransferResult retry = service.transfer(aliceToBobWithQuote("20.00", "USD", "k-q", QUOTE));

            assertThat(retry.duplicate()).isTrue();
            assertThat(retry.transactionReference()).isEqualTo(first.transactionReference());
            assertThat(retry.amount()).isEqualByComparingTo("94.00");
            assertThat(conversions.quoteCalls).isEqualTo(1);
        }
    }

    @Test
    @DisplayName("below the minimum balance → the account module's 400 passes through, nothing is stored")
    void insufficientBalance() {
        assertThatThrownBy(() -> service.transfer(command("alice", "1000000001", "1000000002", "100.01", "k1")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Insufficient balance. Minimum balance of $20.00 must be maintained.");
        assertThat(store.saved).isEmpty();
        assertThat(accounts.balanceOf(ALICE_ACC)).isEqualByComparingTo("120.00");
    }
}
