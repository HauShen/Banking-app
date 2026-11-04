package com.Banking_app.security;

import com.Banking_app.support.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Money movement and transfer history. Every test starts with:
 * alice RM120 (20 opening + 100 top-up), bob RM20, carol RM20 (an outsider).
 */
class TransferSecurityTest extends AbstractIntegrationTest {

    private Customer alice;
    private Customer bob;
    private Customer carol;

    @BeforeEach
    void setUpCustomers() {
        alice = newCustomer("alice");
        bob = newCustomer("bob");
        carol = newCustomer("carol");
        topUp(alice.token(), alice.account(), "100");
    }

    private static String newKey() {
        return UUID.randomUUID().toString();
    }

    // ------------------------------------------------------------------ sending money

    @Test
    @DisplayName("The owner can transfer from their own account -> 201, both balances updated")
    void owner_canTransfer() {
        ResponseEntity<String> response = transfer(alice.token(), alice.account(), bob.account(), "30", newKey());

        assertStatus(response, 201);
        assertThat(balanceOf(alice.account())).isEqualByComparingTo("90");
        assertThat(balanceOf(bob.account())).isEqualByComparingTo("50");
    }

    @Test
    @DisplayName("Another customer cannot transfer from someone else's account -> 403, no money moves")
    void otherCustomer_cannotTransferFromSomeoneElsesAccount() {
        ResponseEntity<String> response = transfer(bob.token(), alice.account(), bob.account(), "30", newKey());

        assertStatus(response, 403);
        assertThat(text(response, "message")).isEqualTo("You can only transfer from your own accounts");
        assertThat(balanceOf(alice.account())).isEqualByComparingTo("120");
        assertThat(balanceOf(bob.account())).isEqualByComparingTo("20");
    }

    @Test
    @DisplayName("An admin cannot transfer from a customer's account either -> 403")
    void admin_cannotTransferFromCustomersAccount() {
        ResponseEntity<String> response = transfer(adminToken(), alice.account(), bob.account(), "30", newKey());

        assertStatus(response, 403);
        assertThat(balanceOf(alice.account())).isEqualByComparingTo("120");
    }

    @Test
    @DisplayName("No token -> 401")
    void noToken_cannotTransfer() {
        assertStatus(transfer(null, alice.account(), bob.account(), "30", newKey()), 401);
    }

    // ------------------------------------------------------------------ idempotency

    @Test
    @DisplayName("Same key from the same account returns the original transfer and debits only once")
    void sameKeySameAccount_isDuplicate() {
        String key = newKey();

        ResponseEntity<String> first = transfer(alice.token(), alice.account(), bob.account(), "30", key);
        ResponseEntity<String> second = transfer(alice.token(), alice.account(), bob.account(), "30", key);

        assertStatus(first, 201);
        assertStatus(second, 201);
        assertThat(text(second, "transactionReference")).isEqualTo(text(first, "transactionReference"));
        assertThat(text(second, "message")).startsWith("Duplicate request detected");
        assertThat(balanceOf(alice.account())).isEqualByComparingTo("90");
    }

    @Test
    @DisplayName("Reusing another account's key is rejected -> 400, and their transaction is not revealed")
    void reusingAnotherAccountsKey_isRejected() {
        String key = newKey();
        ResponseEntity<String> alicesTransfer = transfer(alice.token(), alice.account(), bob.account(), "30", key);
        assertStatus(alicesTransfer, 201);

        ResponseEntity<String> response = transfer(bob.token(), bob.account(), carol.account(), "1", key);

        assertStatus(response, 400);
        assertThat(text(response, "message")).isEqualTo("idempotencyKey has already been used for a different transfer");
        assertThat(response.getBody()).doesNotContain(text(alicesTransfer, "transactionReference"));
    }

    // ------------------------------------------------------------------ reading history

    @Test
    @DisplayName("Account history: owner and admin only")
    void accountHistory_ownerAndAdminOnly() {
        assertStatus(transfer(alice.token(), alice.account(), bob.account(), "30", newKey()), 201);
        String path = "/api/transfers/account/" + alice.account().number();

        assertStatus(get(path, alice.token()), 200);
        assertStatus(get(path, adminToken()), 200);
        assertStatus(get(path, bob.token()), 403);     // received money from alice, but it's still her account
        assertStatus(get(path, carol.token()), 403);
    }

    @Test
    @DisplayName("Single transaction: sender, recipient and admin only")
    void singleTransaction_senderRecipientAndAdminOnly() {
        ResponseEntity<String> sent = transfer(alice.token(), alice.account(), bob.account(), "30", newKey());
        assertStatus(sent, 201);
        String path = "/api/transfers/" + text(sent, "transactionReference");

        assertStatus(get(path, alice.token()), 200);
        ResponseEntity<String> seenByRecipient = get(path, bob.token());
        assertStatus(seenByRecipient, 200);
        assertThat(json(seenByRecipient).has("toAccountUserName"))
                .as("recipient's internal user id must not be exposed").isFalse();
        assertStatus(get(path, adminToken()), 200);
        assertStatus(get(path, carol.token()), 403);
    }
}
