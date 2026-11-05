package com.Banking_app.transaction.application.service;

import com.Banking_app.shared.Requester;
import com.Banking_app.transaction.application.exception.TransactionAccessDeniedException;
import com.Banking_app.transaction.application.exception.TransactionNotFoundException;
import com.Banking_app.transaction.application.port.in.result.TransactionView;
import com.Banking_app.transaction.domain.enums.TransactionStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GetTransactionServiceTest {

    private static final Instant CREATED = Instant.parse("2026-11-06T08:00:00Z");

    private final FakeAccounts accounts = new FakeAccounts();
    private final FakeTransactionStore store = new FakeTransactionStore();
    private final GetTransactionService service = new GetTransactionService(store, accounts);

    GetTransactionServiceTest() {
        accounts.add(1L, "1000000001", "alice", false, "90.00");
        accounts.add(2L, "1000000002", "bob", false, "50.00");
        accounts.add(3L, "1000000003", "carol", false, "20.00");
        store.add("TXN-TRANSFER0001", 1L, 2L, "30.00", "k1", CREATED);     // alice → bob
        store.add("TXN-DEPOSIT00001", null, 2L, "50.00", "k2", CREATED);   // cash → bob
    }

    @Test
    @DisplayName("the sender sees the transfer, with account numbers instead of ids")
    void senderSeesIt() {
        TransactionView view = service.getByReference("TXN-TRANSFER0001", new Requester("alice", false));

        assertThat(view.referenceNumber()).isEqualTo("TXN-TRANSFER0001");
        assertThat(view.fromAccountNumber()).isEqualTo("1000000001");
        assertThat(view.toAccountNumber()).isEqualTo("1000000002");
        assertThat(view.amount()).isEqualByComparingTo("30.00");
        assertThat(view.currency()).isEqualTo("MYR");
        assertThat(view.status()).isEqualTo(TransactionStatus.SUCCESS);
        assertThat(view.createdAt()).isEqualTo(CREATED);
    }

    @Test
    @DisplayName("the recipient and admins see it too")
    void recipientAndAdmin() {
        assertThat(service.getByReference("TXN-TRANSFER0001", new Requester("bob", false)).referenceNumber())
                .isEqualTo("TXN-TRANSFER0001");
        assertThat(service.getByReference("TXN-TRANSFER0001", new Requester("admin-id", true)).referenceNumber())
                .isEqualTo("TXN-TRANSFER0001");
    }

    @Test
    @DisplayName("anyone else → 403")
    void otherUser() {
        assertThatThrownBy(() -> service.getByReference("TXN-TRANSFER0001", new Requester("carol", false)))
                .isInstanceOf(TransactionAccessDeniedException.class)
                .hasMessage("You can only view your own transactions");
    }

    @Test
    @DisplayName("unknown reference → 404")
    void unknownReference() {
        assertThatThrownBy(() -> service.getByReference("TXN-NOPE", new Requester("alice", false)))
                .isInstanceOf(TransactionNotFoundException.class)
                .hasMessage("Transaction not found");
    }

    @Test
    @DisplayName("a cash deposit has no from-account: only the recipient (and admins) may see it")
    void cashDeposit() {
        TransactionView view = service.getByReference("TXN-DEPOSIT00001", new Requester("bob", false));

        assertThat(view.fromAccountNumber()).isNull();
        assertThat(view.toAccountNumber()).isEqualTo("1000000002");
        assertThat(view.currency()).isEqualTo("MYR");
        assertThatThrownBy(() -> service.getByReference("TXN-DEPOSIT00001", new Requester("alice", false)))
                .isInstanceOf(TransactionAccessDeniedException.class);
    }
}
