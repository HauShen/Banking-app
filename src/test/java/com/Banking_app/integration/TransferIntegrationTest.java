package com.Banking_app.integration;

import com.Banking_app.enums.AccountType;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The money-movement tests. These are the ones that matter: they assert that a transfer
 * changes both balances by exactly the right amount and writes a balanced pair of ledger
 * entries, which is the core invariant of the whole application.
 */
@DisplayName("Fund transfers")
class TransferIntegrationTest extends AbstractIntegrationTest {

    private static final String PASSWORD = "password123";

    private String aliceToken;
    private long aliceAccountId;
    private String aliceAccountNumber;
    private String bobAccountNumber;

    @BeforeEach
    void openAccounts() throws Exception {
        String adminToken = bootstrapAdmin("admin", "admin@example.com", PASSWORD);
        aliceToken = registerCustomer("alice", "alice@example.com", PASSWORD);
        String bobToken = registerCustomer("bob", "bob@example.com", PASSWORD);

        JsonNode aliceAccount = createAccount(adminToken, currentUserId(aliceToken), AccountType.SAVINGS);
        JsonNode bobAccount = createAccount(adminToken, currentUserId(bobToken), AccountType.SAVINGS);

        aliceAccountId = aliceAccount.get("accountId").asLong();
        aliceAccountNumber = aliceAccount.get("accountNumber").asText();
        bobAccountNumber = bobAccount.get("accountNumber").asText();

        // Accounts open at the 20.00 floor; give Alice something to send.
        topUp(aliceToken, aliceAccountId, "100.00");
    }

    @Test
    @DisplayName("moves money and records a balanced pair of ledger entries")
    void transferMovesMoneyAndWritesBalancedLedger() throws Exception {
        assertThat(balanceOf(aliceAccountNumber)).isEqualByComparingTo("120.00");
        assertThat(balanceOf(bobAccountNumber)).isEqualByComparingTo("20.00");

        String response = performTransfer(aliceToken, aliceAccountNumber, bobAccountNumber,
                "50.00", UUID.randomUUID().toString())
                // Asserted as "any 2xx" on purpose: TransferController carries both
                // @ResponseStatus(CREATED) and ResponseEntity.ok(), which contradict each
                // other. Pin this to an exact code once that is resolved.
                .andExpect(status().is2xxSuccessful())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.transactionReference").isNotEmpty())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String reference = readTree(response).get("transactionReference").asText();

        assertThat(balanceOf(aliceAccountNumber))
                .as("sender is debited by exactly the transfer amount")
                .isEqualByComparingTo("70.00");
        assertThat(balanceOf(bobAccountNumber))
                .as("recipient is credited by exactly the transfer amount")
                .isEqualByComparingTo("70.00");

        List<Map<String, Object>> ledger = ledgerEntriesFor(reference);
        assertThat(ledger).hasSize(2);

        Map<String, Object> debit = entryOfType(ledger, "DEBIT");
        Map<String, Object> credit = entryOfType(ledger, "CREDIT");

        assertThat(debit.get("account_number")).isEqualTo(aliceAccountNumber);
        assertThat(credit.get("account_number")).isEqualTo(bobAccountNumber);

        assertThat((BigDecimal) debit.get("amount")).isEqualByComparingTo("50.00");
        assertThat((BigDecimal) credit.get("amount")).isEqualByComparingTo("50.00");

        assertThat((BigDecimal) debit.get("balance_after"))
                .as("ledger must record the balance the account actually ended up with")
                .isEqualByComparingTo("70.00");
        assertThat((BigDecimal) credit.get("balance_after")).isEqualByComparingTo("70.00");
    }

    @Test
    @DisplayName("replaying the same idempotency key does not move money twice")
    void replayedIdempotencyKeyDoesNotDoubleSpend() throws Exception {
        String key = UUID.randomUUID().toString();

        String first = performTransfer(aliceToken, aliceAccountNumber, bobAccountNumber, "50.00", key)
                .andReturn().getResponse().getContentAsString();
        String firstReference = readTree(first).get("transactionReference").asText();

        int transactionsAfterFirst = countTransactions();
        int ledgerAfterFirst = countLedgerEntries();

        String second = performTransfer(aliceToken, aliceAccountNumber, bobAccountNumber, "50.00", key)
                .andReturn().getResponse().getContentAsString();

        assertThat(readTree(second).get("transactionReference").asText())
                .as("a replay must return the original transaction, not a new one")
                .isEqualTo(firstReference);

        assertThat(balanceOf(aliceAccountNumber))
                .as("balances must be untouched by the replay")
                .isEqualByComparingTo("70.00");
        assertThat(balanceOf(bobAccountNumber)).isEqualByComparingTo("70.00");

        assertThat(countTransactions()).isEqualTo(transactionsAfterFirst);
        assertThat(countLedgerEntries()).isEqualTo(ledgerAfterFirst);
    }

    @Test
    @DisplayName("is rejected when it would breach the minimum balance, leaving balances untouched")
    void transferBreachingMinimumBalanceIsRejected() throws Exception {
        performTransfer(aliceToken, aliceAccountNumber, bobAccountNumber,
                "105.00", UUID.randomUUID().toString())
                .andExpect(status().isBadRequest());

        assertThat(balanceOf(aliceAccountNumber))
                .as("a rejected transfer must not debit the sender")
                .isEqualByComparingTo("120.00");
        assertThat(balanceOf(bobAccountNumber))
                .as("a rejected transfer must not credit the recipient")
                .isEqualByComparingTo("20.00");
    }

    @Test
    @DisplayName("is rejected when source and destination are the same account")
    void transferToSameAccountIsRejected() throws Exception {
        performTransfer(aliceToken, aliceAccountNumber, aliceAccountNumber,
                "10.00", UUID.randomUUID().toString())
                .andExpect(status().isBadRequest());

        assertThat(balanceOf(aliceAccountNumber)).isEqualByComparingTo("120.00");
    }

    @Test
    @DisplayName("is rejected when the sending account is locked")
    void transferFromLockedAccountIsRejected() throws Exception {
        lockAccount(aliceAccountNumber);

        performTransfer(aliceToken, aliceAccountNumber, bobAccountNumber,
                "10.00", UUID.randomUUID().toString())
                .andExpect(status().isBadRequest());

        assertThat(balanceOf(aliceAccountNumber)).isEqualByComparingTo("120.00");
        assertThat(balanceOf(bobAccountNumber)).isEqualByComparingTo("20.00");
    }

    @Test
    @DisplayName("is rejected when the destination account does not exist")
    void transferToUnknownAccountIsRejected() throws Exception {
        performTransfer(aliceToken, aliceAccountNumber, "0000000000",
                "10.00", UUID.randomUUID().toString())
                .andExpect(status().isNotFound());

        assertThat(balanceOf(aliceAccountNumber)).isEqualByComparingTo("120.00");
    }

    @Test
    @DisplayName("rejects a zero or negative amount")
    void transferWithNonPositiveAmountIsRejected() throws Exception {
        performTransfer(aliceToken, aliceAccountNumber, bobAccountNumber,
                "0.00", UUID.randomUUID().toString())
                .andExpect(status().isBadRequest());

        assertThat(balanceOf(aliceAccountNumber)).isEqualByComparingTo("120.00");
    }

    @Test
    @Disabled("""
            KNOWN GAP — POST /api/transfers does not check that the caller owns the source \
            account, so any authenticated user can drain any account whose number they know. \
            Enable this test once the ownership check is in place (the hexagonal refactor is \
            the natural moment: the check belongs in the domain service, not the controller).""")
    @DisplayName("a user cannot transfer out of an account they do not own")
    void userCannotTransferFromSomeoneElsesAccount() throws Exception {
        String mallory = registerCustomer("mallory", "mallory@example.com", PASSWORD);

        performTransfer(mallory, aliceAccountNumber, bobAccountNumber,
                "50.00", UUID.randomUUID().toString())
                .andExpect(status().isForbidden());

        assertThat(balanceOf(aliceAccountNumber))
                .as("a third party must not be able to move Alice's money")
                .isEqualByComparingTo("120.00");
    }

    // ---------------------------------------------------------------------

    private ResultActions performTransfer(
            String token, String from, String to, String amount, String idempotencyKey) throws Exception {

        // Built as raw JSON so the amount goes over the wire as a number, exactly as a
        // real client would send it.
        String body = """
                {
                  "fromAccountNumber": "%s",
                  "toAccountNumber": "%s",
                  "amount": %s,
                  "currency": "MYR",
                  "description": "integration test transfer",
                  "idempotencyKey": "%s"
                }
                """.formatted(from, to, amount, idempotencyKey);

        return mockMvc.perform(post("/api/transfers")
                .header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private Map<String, Object> entryOfType(List<Map<String, Object>> ledger, String type) {
        return ledger.stream()
                .filter(row -> type.equals(row.get("ledger_type")))
                .findFirst()
                .orElseThrow(() -> new AssertionError("no " + type + " ledger entry was written"));
    }
}
