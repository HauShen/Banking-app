package com.Banking_app.account.domain.models.account;

import com.Banking_app.account.domain.enums.AccountCurrency;
import com.Banking_app.account.domain.enums.AccountStatus;
import com.Banking_app.account.domain.enums.AccountType;
import com.Banking_app.account.domain.exception.AccountNotActiveException;
import com.Banking_app.account.domain.exception.InsufficientBalanceException;
import com.Banking_app.account.domain.exception.InvalidAmountException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Plain unit tests for the Account aggregate: no Spring, no database. */
class AccountTest {

    private static final Instant CREATED = Instant.parse("2026-11-05T10:00:00Z");

    private static Account account(AccountStatus status, String balance) {
        return Account.rehydrate(1L, "user-1", "1000000001", AccountType.values()[0], status,
                AccountCurrency.MYR, new BigDecimal(balance), CREATED);
    }

    @Nested
    @DisplayName("withdraw")
    class Withdraw {

        @Test
        @DisplayName("takes the amount off the balance")
        void reducesBalance() {
            Account account = account(AccountStatus.ACTIVE, "120.00");

            account.withdraw(new BigDecimal("30.00"));

            assertThat(account.getBalance()).isEqualByComparingTo("90.00");
        }

        @Test
        @DisplayName("may leave exactly the minimum balance of 20.00")
        void mayLeaveExactlyTheMinimum() {
            Account account = account(AccountStatus.ACTIVE, "120.00");

            account.withdraw(new BigDecimal("100.00"));

            assertThat(account.getBalance()).isEqualByComparingTo("20.00");
        }

        @Test
        @DisplayName("may not go below 20.00: same message as the old transfer code, balance unchanged")
        void rejectsGoingBelowTheMinimum() {
            Account account = account(AccountStatus.ACTIVE, "120.00");

            assertThatThrownBy(() -> account.withdraw(new BigDecimal("100.01")))
                    .isInstanceOf(InsufficientBalanceException.class)
                    .isInstanceOf(IllegalArgumentException.class)   // → 400
                    .hasMessage("Insufficient balance. Minimum balance of $20.00 must be maintained.");
            assertThat(account.getBalance()).isEqualByComparingTo("120.00");
        }

        @Test
        @DisplayName("rejects zero, negative and missing amounts")
        void rejectsNonPositiveAmount() {
            Account account = account(AccountStatus.ACTIVE, "120.00");

            for (BigDecimal amount : new BigDecimal[]{BigDecimal.ZERO, new BigDecimal("-1"), null}) {
                assertThatThrownBy(() -> account.withdraw(amount))
                        .as("amount %s", amount)
                        .isInstanceOf(InvalidAmountException.class)
                        .hasMessage("Withdrawal amount must be greater than zero");
            }
            assertThat(account.getBalance()).isEqualByComparingTo("120.00");
        }

        @Test
        @DisplayName("rejects more than 2 decimals as written (10.005, 10.500), balance unchanged; 10.5 is fine")
        void rejectsMoreThanTwoDecimals() {
            Account account = account(AccountStatus.ACTIVE, "120.00");

            for (String amount : new String[]{"10.005", "10.500"}) {
                assertThatThrownBy(() -> account.withdraw(new BigDecimal(amount)))
                        .as("amount %s", amount)
                        .isInstanceOf(InvalidAmountException.class)
                        .hasMessage("Withdrawal amount can have at most 2 decimal places");
            }
            assertThat(account.getBalance()).isEqualByComparingTo("120.00");

            account.withdraw(new BigDecimal("10.5"));
            assertThat(account.getBalance()).isEqualByComparingTo("109.50");
        }

        @Test
        @DisplayName("is not allowed on a locked account")
        void rejectsLockedAccount() {
            Account account = account(AccountStatus.LOCKED, "120.00");

            assertThatThrownBy(() -> account.withdraw(new BigDecimal("10.00")))
                    .isInstanceOf(AccountNotActiveException.class)
                    .hasMessage("Cannot withdraw from a non-active account");
            assertThat(account.getBalance()).isEqualByComparingTo("120.00");
        }
    }

    @Nested
    @DisplayName("deposit")
    class Deposit {

        @Test
        @DisplayName("adds the amount to the balance")
        void addsAmount() {
            Account account = account(AccountStatus.ACTIVE, "20.00");

            account.deposit(new BigDecimal("50.00"));

            assertThat(account.getBalance()).isEqualByComparingTo("70.00");
        }

        @Test
        @DisplayName("rejects more than 2 decimals as written (0.001, 5.100), balance unchanged; 5.10 is fine")
        void rejectsMoreThanTwoDecimals() {
            Account account = account(AccountStatus.ACTIVE, "20.00");

            for (String amount : new String[]{"0.001", "5.100"}) {
                assertThatThrownBy(() -> account.deposit(new BigDecimal(amount)))
                        .as("amount %s", amount)
                        .isInstanceOf(InvalidAmountException.class)
                        .hasMessage("Top-up amount can have at most 2 decimal places");
            }
            assertThat(account.getBalance()).isEqualByComparingTo("20.00");

            account.deposit(new BigDecimal("5.10"));
            assertThat(account.getBalance()).isEqualByComparingTo("25.10");
        }

        @Test
        @DisplayName("is not allowed on a locked account")
        void rejectsLockedAccount() {
            Account account = account(AccountStatus.LOCKED, "20.00");

            assertThatThrownBy(() -> account.deposit(new BigDecimal("50.00")))
                    .isInstanceOf(AccountNotActiveException.class);
            assertThat(account.getBalance()).isEqualByComparingTo("20.00");
        }
    }
}
