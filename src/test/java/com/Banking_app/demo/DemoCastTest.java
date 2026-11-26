package com.Banking_app.demo;

import com.Banking_app.account.domain.enums.AccountStatus;
import com.Banking_app.account.domain.enums.AccountType;
import com.Banking_app.userProfile.domain.enums.UserRole;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** The demo cast: the fixed names and numbers the demo tour relies on, and the rules the database would enforce. */
class DemoCastTest {

    private static final ZoneId KL = ZoneId.of("Asia/Kuala_Lumpur");

    private final DemoCast cast = DemoCast.standard(90);

    @Test
    void standardCast_hasTheUsersAndAccountsFromThePlan() {
        assertThat(cast.users()).extracting(DemoUser::username)
                .containsExactly("weiming", "mei.ling", "ahmad.f", "priya.n", "jason.k", "demoadmin");
        assertThat(cast.accounts()).extracting(DemoAccount::accountNumber)
                .containsExactlyInAnyOrder("1234567890", "9876543210", "7781204459", "4402917736", "4402917735",
                        "5550123456", "3326518840");
        assertThat(cast.accountsOf("weiming")).extracting(DemoAccount::type)
                .containsExactlyInAnyOrder(DemoAccount.Type.SAVINGS, DemoAccount.Type.CURRENT);
    }

    @Test
    void demoadminIsTheOnlyAdmin_andHasNoAccounts() {
        assertThat(cast.users()).filteredOn(DemoUser::isAdmin).extracting(DemoUser::username)
                .containsExactly("demoadmin");
        assertThat(cast.accountsOf("demoadmin")).isEmpty();
    }

    @Test
    void onlyAhmadsSavingsIsLocked() {
        assertThat(cast.accounts()).filteredOn(a -> a.status() == DemoAccount.Status.LOCKED)
                .extracting(DemoAccount::accountNumber).containsExactly(DemoCast.AHMAD_SAVINGS_LOCKED);
    }

    @Test
    void userIds_areUuidsThatStayTheSameAfterAReset() {
        DemoCast again = DemoCast.standard(90);
        for (DemoUser user : cast.users()) {
            assertThat(user.id()).hasSize(36).isEqualTo(again.user(user.username()).id());   // users.id VARCHAR(36)
        }
        assertThat(cast.users()).extracting(DemoUser::id).doesNotHaveDuplicates();
    }

    @Test
    void emails_areUnique() {
        assertThat(cast.users()).extracting(DemoUser::email).doesNotHaveDuplicates();
    }

    @ParameterizedTest
    @ValueSource(ints = {7, 8, 30, 90, 366})
    void everyAllowedHistoryLength_givesAValidCast(int historyDays) {
        DemoCast c = DemoCast.standard(historyDays);   // the constructor checks would throw otherwise

        for (DemoAccount account : c.accounts()) {
            // Inside the history window, so the opening deposit is part of the story and the chart.
            assertThat(account.openedDaysAgo()).isBetween(1, historyDays - 1);
        }
        DemoAccount locked = c.account(DemoCast.AHMAD_SAVINGS_LOCKED);
        assertThat(locked.lockedDaysAgo()).isPositive().isLessThan(locked.openedDaysAgo());
        assertThat(c.account(DemoCast.JASON_SAVINGS_NEW).openedDaysAgo()).isEqualTo(DemoCast.NEW_ACCOUNT_DAYS_AGO);
    }

    @Test
    void historyShorterThanSevenDays_isRejected() {
        assertThatThrownBy(() -> DemoCast.standard(6)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void openedAt_isTheOpeningTimeInTheBankTimeZone() {
        LocalDate today = LocalDate.of(2026, 11, 26);
        DemoAccount main = cast.account(DemoCast.MAIN_SAVINGS);

        Instant openedAt = main.openedAt(today, KL);

        ZonedDateTime local = openedAt.atZone(KL);
        assertThat(local.toLocalDate()).isEqualTo(today.minusDays(89));
        assertThat(local.toLocalTime()).isEqualTo(DemoAccount.OPENING_TIME);
    }

    @Test
    void demoEnums_haveTheSameNamesAsTheModuleEnums() {
        // The demo writes these names as text into users.role, accounts.account_type and accounts.account_status.
        assertThat(names(DemoUser.Role.values())).isEqualTo(names(UserRole.values()));
        assertThat(names(DemoAccount.Type.values())).isEqualTo(names(AccountType.values()));
        assertThat(names(DemoAccount.Status.values())).isEqualTo(names(AccountStatus.values()));
    }

    @Test
    void duplicateAccountNumber_isRejected() {
        DemoUser owner = customer("a");
        assertThatThrownBy(() -> new DemoCast(List.of(owner), List.of(
                DemoAccount.active("1111111111", "a", DemoAccount.Type.SAVINGS, 10),
                DemoAccount.active("1111111111", "a", DemoAccount.Type.CURRENT, 10))))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Duplicate account number");
    }

    @Test
    void accountOfAnUnknownUser_isRejected() {
        assertThatThrownBy(() -> new DemoCast(List.of(customer("a")),
                List.of(DemoAccount.active("1111111111", "nobody", DemoAccount.Type.SAVINGS, 10))))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("unknown user");
    }

    @Test
    void accountOfAnAdmin_isRejected() {
        DemoUser admin = new DemoUser("boss", "Boss", "boss@banking.demo", DemoUser.Role.ADMIN);
        assertThatThrownBy(() -> new DemoCast(List.of(admin),
                List.of(DemoAccount.active("1111111111", "boss", DemoAccount.Type.SAVINGS, 10))))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Admins have no accounts");
    }

    @Test
    void duplicateUsernameOrEmail_isRejected() {
        assertThatThrownBy(() -> new DemoCast(List.of(customer("a"), customer("a")), List.of()))
                .hasMessageContaining("Duplicate username");
        DemoUser b = new DemoUser("b", "B", "same@banking.demo", DemoUser.Role.CUSTOMER);
        DemoUser c = new DemoUser("c", "C", "same@banking.demo", DemoUser.Role.CUSTOMER);
        assertThatThrownBy(() -> new DemoCast(List.of(b, c), List.of())).hasMessageContaining("Duplicate email");
    }

    @Test
    void badAccounts_areRejected() {
        assertThatThrownBy(() -> DemoAccount.active("0123456789", "a", DemoAccount.Type.SAVINGS, 10))
                .hasMessageContaining("10 digits");                    // below 1 000 000 000
        assertThatThrownBy(() -> DemoAccount.active("12345", "a", DemoAccount.Type.SAVINGS, 10))
                .hasMessageContaining("10 digits");
        assertThatThrownBy(() -> DemoAccount.locked("1111111111", "a", DemoAccount.Type.SAVINGS, 10, 10))
                .hasMessageContaining("locked after it was opened");   // locked on the opening day itself
        assertThatThrownBy(() -> new DemoAccount("1111111111", "a", DemoAccount.Type.SAVINGS,
                DemoAccount.Status.ACTIVE, 10, 3))
                .hasMessageContaining("no lock day");
    }

    private static DemoUser customer(String username) {
        return new DemoUser(username, username, username + "@banking.demo", DemoUser.Role.CUSTOMER);
    }

    private static List<String> names(Enum<?>[] values) {
        return Arrays.stream(values).map(Enum::name).toList();
    }
}
