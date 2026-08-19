package com.Banking_app.integration;

import com.Banking_app.enums.AccountType;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Role and ownership boundaries. These assert the rules a reviewer would want to see
 * enforced, independently of how the code is organised internally.
 */
@DisplayName("Account authorization")
class AccountAuthorizationIntegrationTest extends AbstractIntegrationTest {

    private static final String PASSWORD = "password123";

    private String adminToken;
    private String aliceToken;
    private String aliceId;

    @BeforeEach
    void setUpUsers() throws Exception {
        adminToken = bootstrapAdmin("admin", "admin@example.com", PASSWORD);
        aliceToken = registerCustomer("alice", "alice@example.com", PASSWORD);
        aliceId = currentUserId(aliceToken);
    }

    @Test
    @DisplayName("an admin can open an account, and it starts at the minimum balance")
    void adminCanOpenAccount() throws Exception {
        String response = mockMvc.perform(post("/api/accounts/create")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("userId", aliceId, "accountType", "SAVINGS"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accountNumber").isNotEmpty())
                .andExpect(jsonPath("$.accountStatus").value("ACTIVE"))
                .andExpect(jsonPath("$.accountCurrency").value("MYR"))
                .andExpect(jsonPath("$.accountType").value("SAVINGS"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Asserted against the database rather than the JSON body: comparing money through
        // jsonPath depends on how the JSON number is parsed, which makes for brittle tests.
        assertThat(balanceOf(readTree(response).get("accountNumber").asText()))
                .isEqualByComparingTo("20.00");
    }

    @Test
    @DisplayName("a customer cannot open an account")
    void customerCannotOpenAccount() throws Exception {
        mockMvc.perform(post("/api/accounts/create")
                        .header("Authorization", bearer(aliceToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("userId", aliceId, "accountType", "SAVINGS"))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("a customer cannot lock an account")
    void customerCannotChangeAccountStatus() throws Exception {
        JsonNode account = createAccount(adminToken, aliceId, AccountType.SAVINGS);

        mockMvc.perform(patch("/api/accounts/{id}/status", account.get("accountId").asLong())
                        .header("Authorization", bearer(aliceToken))
                        .param("status", "LOCKED"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("a customer cannot delete an account")
    void customerCannotDeleteAccount() throws Exception {
        JsonNode account = createAccount(adminToken, aliceId, AccountType.SAVINGS);

        mockMvc.perform(delete("/api/accounts/{id}", account.get("accountId").asLong())
                        .header("Authorization", bearer(aliceToken)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("a customer cannot top up an account belonging to someone else")
    void customerCannotTopUpAnotherUsersAccount() throws Exception {
        JsonNode aliceAccount = createAccount(adminToken, aliceId, AccountType.SAVINGS);
        String malloryToken = registerCustomer("mallory", "mallory@example.com", PASSWORD);

        mockMvc.perform(patch("/api/accounts/{id}/topup", aliceAccount.get("accountId").asLong())
                        .header("Authorization", bearer(malloryToken))
                        .param("amount", "1000.00"))
                .andExpect(status().isBadRequest());

        assertThat(balanceOf(aliceAccount.get("accountNumber").asText()))
                .as("a third party must not be able to change Alice's balance")
                .isEqualByComparingTo("20.00");
    }

    @Test
    @DisplayName("topping up your own account credits it and writes a ledger entry")
    void ownerCanTopUpOwnAccount() throws Exception {
        JsonNode account = createAccount(adminToken, aliceId, AccountType.SAVINGS);
        long accountId = account.get("accountId").asLong();
        String accountNumber = account.get("accountNumber").asText();

        mockMvc.perform(patch("/api/accounts/{id}/topup", accountId)
                        .header("Authorization", bearer(aliceToken))
                        .param("amount", "250.00"))
                .andExpect(status().isOk());

        assertThat(balanceOf(accountNumber)).isEqualByComparingTo("270.00");
        assertThat(countLedgerEntries())
                .as("every balance change must leave a ledger trail")
                .isEqualTo(1);
    }

    @Test
    @DisplayName("a negative top-up is rejected")
    void negativeTopUpIsRejected() throws Exception {
        JsonNode account = createAccount(adminToken, aliceId, AccountType.SAVINGS);

        mockMvc.perform(patch("/api/accounts/{id}/topup", account.get("accountId").asLong())
                        .header("Authorization", bearer(aliceToken))
                        .param("amount", "-100.00"))
                .andExpect(status().isBadRequest());

        assertThat(balanceOf(account.get("accountNumber").asText())).isEqualByComparingTo("20.00");
    }
}
