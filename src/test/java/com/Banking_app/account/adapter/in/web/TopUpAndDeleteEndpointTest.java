package com.Banking_app.account.adapter.in.web;

import com.Banking_app.support.AbstractIntegrationTest;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The account endpoints that write to or read from the transaction module: top-up (records a cash deposit) and
 * delete (refused when the account has history). Expected values come from the old LegacyCashDepositAdapter /
 * LegacyAccountHistoryAdapter.
 */
class TopUpAndDeleteEndpointTest extends AbstractIntegrationTest {

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    @DisplayName("a top-up is recorded like before: CDM- reference, old description, one CREDIT row, same timestamps")
    void topUpIsRecorded() {
        Customer alice = newCustomer("tu_alice");

        topUp(alice.token(), alice.account(), "50.00");

        assertThat(balanceOf(alice.account())).isEqualByComparingTo("70.00");
        JsonNode deposit = json(get("/api/transfers/account/" + alice.account().number(), alice.token()))
                .get("content").get(0);
        String reference = deposit.get("reference").asText();
        assertThat(reference).matches("CDM-[0-9A-F]{12}");
        assertThat(deposit.get("fromAccountNumber").asText()).isEqualTo("Cash Deposit Machine");
        assertThat(deposit.get("toAccountNumber").asText()).isEqualTo(alice.account().number());
        assertThat(deposit.get("amount").decimalValue()).isEqualByComparingTo("50.00");
        assertThat(deposit.get("description").asText()).isEqualTo("Cash Deposit Machine top-up");
        assertThat(deposit.get("status").asText()).isEqualTo("SUCCESS");
        assertThat(deposit.get("postedAt").asText()).isEqualTo(deposit.get("createdAt").asText());

        Map<String, Object> row = jdbc.queryForMap("""
                SELECT t.idempotency_key, l.ledger_type, l.amount, l.balance_after,
                       l.created_at = t.created_at AS same_time
                FROM transactions t JOIN ledgers l ON l.transaction_id = t.id
                WHERE t.reference_number = ?""", reference);
        assertThat((String) row.get("idempotency_key")).startsWith("CDM-");
        assertThat(row.get("ledger_type")).isEqualTo("CREDIT");
        assertThat((BigDecimal) row.get("balance_after")).isEqualByComparingTo("70.00");
        assertThat(row.get("same_time")).isEqualTo(true);
    }

    @Test
    @DisplayName("each top-up gets its own transaction (no idempotency clash between top-ups)")
    void topUpsAreSeparate() {
        Customer alice = newCustomer("tu_alice");

        topUp(alice.token(), alice.account(), "10.00");
        topUp(alice.token(), alice.account(), "10.00");

        assertThat(balanceOf(alice.account())).isEqualByComparingTo("40.00");
        assertThat(json(get("/api/transfers/account/" + alice.account().number(), alice.token()))
                .get("totalElements").asLong()).isEqualTo(2);
    }

    @Test
    @DisplayName("delete: a fresh account → 204; with a top-up → 409; one that only received a transfer → 409")
    void deleteChecksHistory() {
        Customer fresh = newCustomer("tu_fresh");
        Customer toppedUp = newCustomer("tu_topped");
        Customer recipient = newCustomer("tu_recipient");
        topUp(toppedUp.token(), toppedUp.account(), "100.00");
        assertStatus(transfer(toppedUp.token(), toppedUp.account(), recipient.account(), "5.00",
                "delete-" + UUID.randomUUID()), 201);
        String admin = adminToken();

        assertStatus(delete("/api/accounts/" + fresh.account().id(), admin), 204);
        assertStatus(delete("/api/accounts/" + toppedUp.account().id(), admin), 409);
        assertStatus(delete("/api/accounts/" + recipient.account().id(), admin), 409);
    }
}
