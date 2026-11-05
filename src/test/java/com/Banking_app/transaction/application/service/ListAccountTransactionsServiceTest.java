package com.Banking_app.transaction.application.service;

import com.Banking_app.shared.PageResult;
import com.Banking_app.shared.Requester;
import com.Banking_app.transaction.application.exception.TransactionAccessDeniedException;
import com.Banking_app.transaction.application.exception.TransactionAccountNotFoundException;
import com.Banking_app.transaction.application.port.in.query.ListAccountTransactionsQuery;
import com.Banking_app.transaction.application.port.in.result.TransactionView;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ListAccountTransactionsServiceTest {

    private static final Instant T0 = Instant.parse("2026-11-06T08:00:00Z");

    private final FakeAccounts accounts = new FakeAccounts();
    private final FakeTransactionStore store = new FakeTransactionStore();
    private final ListAccountTransactionsService service = new ListAccountTransactionsService(store, accounts);

    ListAccountTransactionsServiceTest() {
        accounts.add(1L, "1000000001", "alice", false, "90.00");
        accounts.add(2L, "1000000002", "bob", false, "50.00");
        accounts.add(3L, "1000000003", "carol", false, "20.00");
        store.add("TXN-OLDEST000001", null, 2L, "50.00", "k1", T0);                 // cash → bob
        store.add("TXN-MIDDLE000001", 1L, 2L, "30.00", "k2", T0.plusSeconds(60));   // alice → bob
        store.add("TXN-NEWEST000001", 2L, 3L, "10.00", "k3", T0.plusSeconds(120));  // bob → carol
        store.add("TXN-NOTBOBS00001", 1L, 3L, "5.00", "k4", T0.plusSeconds(180));   // alice → carol
    }

    private static ListAccountTransactionsQuery bobs(int page, int size) {
        return new ListAccountTransactionsQuery("1000000002", page, size);
    }

    @Test
    @DisplayName("the owner gets their account's transactions, newest first, with account numbers")
    void ownerSeesHistory() {
        PageResult<TransactionView> page = service.list(bobs(0, 10), new Requester("bob", false));

        assertThat(page.content().stream().map(TransactionView::referenceNumber).toList())
                .containsExactly("TXN-NEWEST000001", "TXN-MIDDLE000001", "TXN-OLDEST000001");
        assertThat(page.content().get(0).fromAccountNumber()).isEqualTo("1000000002");
        assertThat(page.content().get(0).toAccountNumber()).isEqualTo("1000000003");
        assertThat(page.content().get(2).fromAccountNumber()).isNull();   // cash deposit
        assertThat(page.totalElements()).isEqualTo(3L);
    }

    @Test
    @DisplayName("one account lookup for the whole page, not one per row")
    void oneLookupPerPage() {
        service.list(bobs(0, 10), new Requester("bob", false));

        assertThat(accounts.findByIdsCalls).isEqualTo(1);
    }

    @Test
    @DisplayName("paging passes through: page 1 of size 2 holds the oldest transaction")
    void paging() {
        PageResult<TransactionView> page = service.list(bobs(1, 2), new Requester("bob", false));

        assertThat(page.content().stream().map(TransactionView::referenceNumber).toList())
                .containsExactly("TXN-OLDEST000001");
        assertThat(page.page()).isEqualTo(1);
        assertThat(page.size()).isEqualTo(2);
        assertThat(page.totalElements()).isEqualTo(3L);
    }

    @Test
    @DisplayName("admins may read any account's history")
    void admin() {
        assertThat(service.list(bobs(0, 10), new Requester("admin-id", true)).totalElements()).isEqualTo(3L);
    }

    @Test
    @DisplayName("anyone else → 403")
    void otherUser() {
        assertThatThrownBy(() -> service.list(bobs(0, 10), new Requester("alice", false)))
                .isInstanceOf(TransactionAccessDeniedException.class)
                .hasMessage("You can only view the history of your own accounts");
    }

    @Test
    @DisplayName("unknown account → 404 with the account number in the message")
    void unknownAccount() {
        assertThatThrownBy(() -> service.list(new ListAccountTransactionsQuery("1000000009", 0, 10),
                new Requester("alice", false)))
                .isInstanceOf(TransactionAccountNotFoundException.class)
                .hasMessage("Account not found: 1000000009");
    }
}
