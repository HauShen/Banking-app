package com.Banking_app.account.application.service;

import com.Banking_app.account.application.port.in.result.AccountSnapshot;
import com.Banking_app.account.domain.enums.AccountStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class LookupAccountsServiceTest {

    private final InMemoryAccountStore store = new InMemoryAccountStore();
    private final LookupAccountsService service = new LookupAccountsService(store);

    @Test
    @DisplayName("findByAccountNumber returns a snapshot, or empty for an unknown number")
    void findByAccountNumber() {
        store.add(1L, "alice", "1000000001", AccountStatus.ACTIVE, "120.00");

        Optional<AccountSnapshot> found = service.findByAccountNumber("1000000001");

        assertThat(found.isPresent()).isTrue();
        assertThat(found.get().ownerId()).isEqualTo("alice");
        assertThat(found.get().balance()).isEqualByComparingTo("120.00");
        assertThat(service.findByAccountNumber("1000000009").isEmpty()).isTrue();
    }

    @Test
    @DisplayName("findByIds returns the known accounts and skips unknown ids")
    void findByIds() {
        store.add(1L, "alice", "1000000001", AccountStatus.ACTIVE, "120.00");
        store.add(2L, "bob", "1000000002", AccountStatus.ACTIVE, "20.00");

        List<AccountSnapshot> found = service.findByIds(List.of(1L, 2L, 99L));

        assertThat(found.stream().map(AccountSnapshot::accountNumber).sorted().toList())
                .containsExactly("1000000001", "1000000002");
    }

    @Test
    @DisplayName("findByIds with no ids returns an empty list")
    void findByNoIds() {
        assertThat(service.findByIds(List.of())).isEmpty();
    }
}
