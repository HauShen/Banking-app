package com.Banking_app.transaction;

import com.Banking_app.support.AbstractIntegrationTest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End to end through HTTP: every new money movement leaves exactly one event in the outbox, written in the same
 * database transaction — a replay adds none, a failed request leaves none. Kafka is off here (the normal suite):
 * the rows just wait, unpublished, which is exactly what happens while the broker is down.
 */
class TransactionEventsOutboxTest extends AbstractIntegrationTest {

    @Autowired
    private JdbcTemplate jdbc;

    /** The outbox rows of one transaction, found by the reference in the jsonb payload. */
    private List<Map<String, Object>> eventsOf(String reference) {
        return jdbc.queryForList("SELECT message_key, event_type, payload::text AS payload, published_at "
                + "FROM outbox_events WHERE payload->>'reference' = ?", reference);
    }

    private int eventCountFor(Account account) {
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM outbox_events WHERE message_key = ?",
                Integer.class, account.number());
        return count == null ? 0 : count;
    }

    private static JsonNode payload(Map<String, Object> row) {
        try {
            return JSON.readTree((String) row.get("payload"));
        } catch (JsonProcessingException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Test
    @DisplayName("a transfer → one unpublished TRANSFER_COMPLETED, filed under the sender, with both accounts and owners")
    void transfer() {
        Customer alice = newCustomer("evt_alice");
        Customer bob = newCustomer("evt_bob");
        topUp(alice.token(), alice.account(), "100.00");

        ResponseEntity<String> response = transfer(alice.token(), alice.account(), bob.account(), "30.00",
                UUID.randomUUID().toString());
        assertStatus(response, 201);
        String reference = text(response, "transactionReference");

        List<Map<String, Object>> rows = eventsOf(reference);
        assertThat(rows).hasSize(1);
        Map<String, Object> row = rows.get(0);
        assertThat(row.get("event_type")).isEqualTo("TRANSFER_COMPLETED");
        assertThat(row.get("message_key")).isEqualTo(alice.account().number());
        assertThat(row.get("published_at")).isNull();

        JsonNode event = payload(row);
        assertThat(event.get("fromAccountNumber").asText()).isEqualTo(alice.account().number());
        assertThat(event.get("fromOwnerId").asText()).isEqualTo(userIdOf(alice.token()));
        assertThat(event.get("toAccountNumber").asText()).isEqualTo(bob.account().number());
        assertThat(event.get("toOwnerId").asText()).isEqualTo(userIdOf(bob.token()));
        assertThat(event.get("amount").decimalValue()).isEqualByComparingTo("30.00");
        assertThat(event.get("original").isNull()).isTrue();
    }

    @Test
    @DisplayName("the same idempotency key again → the original transfer and still only one event")
    void replayAddsNoEvent() {
        Customer alice = newCustomer("evt_alice");
        Customer bob = newCustomer("evt_bob");
        topUp(alice.token(), alice.account(), "50.00");   // a new account holds only the RM 20.00 minimum
        String key = UUID.randomUUID().toString();

        ResponseEntity<String> first = transfer(alice.token(), alice.account(), bob.account(), "5.00", key);
        assertStatus(first, 201);
        String reference = text(first, "transactionReference");
        ResponseEntity<String> retry = transfer(alice.token(), alice.account(), bob.account(), "5.00", key);

        assertStatus(retry, 201);
        assertThat(text(retry, "transactionReference")).isEqualTo(reference);
        assertThat(eventsOf(reference)).hasSize(1);
    }

    @Test
    @DisplayName("a failed transfer (below the minimum balance) is rolled back completely: no event")
    void failedTransferLeavesNoEvent() {
        Customer alice = newCustomer("evt_alice");   // RM 20.00, the minimum
        Customer bob = newCustomer("evt_bob");

        assertStatus(transfer(alice.token(), alice.account(), bob.account(), "1.00", UUID.randomUUID().toString()),
                400);

        assertThat(eventCountFor(alice.account())).isZero();
    }

    @Test
    @DisplayName("a top-up → one CASH_DEPOSITED with the account, its owner and the amount")
    void topUpEvent() {
        Customer carol = newCustomer("evt_carol");

        topUp(carol.token(), carol.account(), "50.00");

        List<Map<String, Object>> rows = jdbc.queryForList("SELECT event_type, payload::text AS payload "
                + "FROM outbox_events WHERE message_key = ?", carol.account().number());
        assertThat(rows).singleElement().satisfies(row -> {
            assertThat(row.get("event_type")).isEqualTo("CASH_DEPOSITED");
            JsonNode event = payload(row);
            assertThat(event.get("reference").asText()).startsWith("CDM-");
            assertThat(event.get("accountNumber").asText()).isEqualTo(carol.account().number());
            assertThat(event.get("ownerId").asText()).isEqualTo(userIdOf(carol.token()));
            assertThat(event.get("amount").decimalValue()).isEqualByComparingTo("50.00");
        });
    }

    @Test
    @DisplayName("a foreign remittance → one FOREIGN_REMITTANCE_RECEIVED with the MYR amount and the original USD")
    void remittanceEvent() {
        Customer dave = newCustomer("evt_dave");

        ResponseEntity<String> response = post("/api/admin/remittances", adminToken(), Map.of(
                "toAccountNumber", dave.account().number(),
                "currency", "USD",
                "amount", new BigDecimal("100.00"),
                "senderName", "John Smith",
                "idempotencyKey", "gateway-" + UUID.randomUUID()));
        assertStatus(response, 201);

        List<Map<String, Object>> rows = eventsOf(text(response, "transactionReference"));
        assertThat(rows).singleElement().satisfies(row -> {
            assertThat(row.get("event_type")).isEqualTo("FOREIGN_REMITTANCE_RECEIVED");
            assertThat(row.get("message_key")).isEqualTo(dave.account().number());
            JsonNode event = payload(row);
            assertThat(event.get("ownerId").asText()).isEqualTo(userIdOf(dave.token()));
            assertThat(event.get("amount").decimalValue())
                    .isEqualByComparingTo(json(response).get("amount").decimalValue());   // whatever the test rate gives
            assertThat(event.get("original").get("currency").asText()).isEqualTo("USD");
            assertThat(event.get("original").get("amount").decimalValue()).isEqualByComparingTo("100.00");
            assertThat(event.has("senderName")).isFalse();   // no names in events
        });
    }
}
