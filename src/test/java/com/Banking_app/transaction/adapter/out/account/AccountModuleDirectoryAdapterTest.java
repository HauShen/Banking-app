package com.Banking_app.transaction.adapter.out.account;

import com.Banking_app.account.application.port.in.result.AccountSnapshot;
import com.Banking_app.account.application.port.in.usecase.LookupAccountsUseCase;
import com.Banking_app.account.domain.enums.AccountCurrency;
import com.Banking_app.account.domain.enums.AccountStatus;
import com.Banking_app.transaction.application.port.out.AccountRef;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class AccountModuleDirectoryAdapterTest {

    private static AccountSnapshot snapshot(Long id, String number, String owner) {
        return new AccountSnapshot(id, number, owner, AccountStatus.ACTIVE, AccountCurrency.MYR, BigDecimal.TEN);
    }

    private final LookupAccountsUseCase fakeAccountModule = new LookupAccountsUseCase() {
        @Override
        public Optional<AccountSnapshot> findByAccountNumber(String accountNumber) {
            return "1000000001".equals(accountNumber) ? Optional.of(snapshot(1L, accountNumber, "alice"))
                    : Optional.empty();
        }

        @Override
        public List<AccountSnapshot> findByIds(Collection<Long> accountIds) {
            return List.of(snapshot(1L, "1000000001", "alice"), snapshot(2L, "1000000002", "bob"));
        }
    };

    private final AccountModuleDirectoryAdapter adapter = new AccountModuleDirectoryAdapter(fakeAccountModule);

    @Test
    @DisplayName("findByAccountNumber: copies id, number, owner and the currency as text")
    void findByAccountNumber() {
        AccountRef ref = adapter.findByAccountNumber("1000000001").orElseThrow();

        assertThat(ref.accountId()).isEqualTo(1L);
        assertThat(ref.accountNumber()).isEqualTo("1000000001");
        assertThat(ref.ownerId()).isEqualTo("alice");
        assertThat(ref.currency()).isEqualTo("MYR");
        assertThat(adapter.findByAccountNumber("1000000009")).isEmpty();
    }

    @Test
    @DisplayName("findByIds: a map from account id to account")
    void findByIds() {
        Map<Long, AccountRef> refs = adapter.findByIds(List.of(1L, 2L));

        assertThat(refs).hasSize(2);
        assertThat(refs.get(2L).accountNumber()).isEqualTo("1000000002");
        assertThat(refs.get(2L).ownerId()).isEqualTo("bob");
    }
}
