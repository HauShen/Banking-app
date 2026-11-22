package com.Banking_app.transaction.adapter.out.persistence;

import com.Banking_app.support.AbstractIntegrationTest;
import com.Banking_app.transaction.application.port.out.PublishTransactionEventPort;
import com.Banking_app.transaction.application.port.out.event.CashDeposited;
import com.Banking_app.transaction.application.port.out.event.ForeignAmount;
import com.Banking_app.transaction.application.port.out.event.TransactionEvent;
import com.Banking_app.transaction.application.port.out.event.TransferCompleted;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The outbox adapter against the real PostgreSQL schema (V12, jsonb payload). Events are built by hand here; the
 * services that create them in real transfers are covered by TransactionEventsOutboxTest.
 *
 * <p>The hand-built events name users that don't exist, so they are deleted after each test: otherwise the relay of
 * a later Kafka test class would send them to the topic as if they were real money movements.
 */
class OutboxPersistenceAdapterTest extends AbstractIntegrationTest {

    private static final Instant AT = Instant.parse("2026-11-21T01:02:03.456789Z");

    @Autowired
    private PublishTransactionEventPort outbox;

    @Autowired
    private JdbcTemplate jdbc;

    private TransactionTemplate tx;

    @Autowired
    void transactionTemplate(PlatformTransactionManager transactionManager) {
        tx = new TransactionTemplate(transactionManager);
    }

    @AfterEach
    void deleteHandBuiltEvents() {
        jdbc.update("DELETE FROM outbox_events WHERE occurred_at = ?", AT.atOffset(ZoneOffset.UTC));
    }

    private static CashDeposited deposit() {
        return new CashDeposited(UUID.randomUUID(), AT, "CDM-" + UUID.randomUUID().toString().substring(0, 12),
                "1000009999", "owner-1", new BigDecimal("50.00"));
    }

    private Map<String, Object> rowOf(TransactionEvent event) {
        return jdbc.queryForMap("SELECT topic, message_key, event_type, event_version, payload::text AS payload, "
                + "published_at, attempts FROM outbox_events WHERE event_id = ?", event.eventId());
    }

    private int countOf(TransactionEvent event) {
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM outbox_events WHERE event_id = ?", Integer.class,
                event.eventId());
        return count == null ? 0 : count;
    }

    @Test
    @DisplayName("stores one unpublished row in the caller's transaction: topic, key, type, version and the JSON payload")
    void storesRow() throws Exception {
        CashDeposited event = deposit();

        tx.executeWithoutResult(status -> outbox.publish(event));

        Map<String, Object> row = rowOf(event);
        assertThat(row.get("topic")).isEqualTo("banking.transactions.v1");
        assertThat(row.get("message_key")).isEqualTo("1000009999");
        assertThat(row.get("event_type")).isEqualTo("CASH_DEPOSITED");
        assertThat(row.get("event_version")).isEqualTo(1);
        assertThat(row.get("published_at")).isNull();
        assertThat(row.get("attempts")).isEqualTo(0);

        JsonNode payload = JSON.readTree((String) row.get("payload"));
        assertThat(payload.get("eventId").asText()).isEqualTo(event.eventId().toString());
        assertThat(payload.get("occurredAt").asText()).isEqualTo("2026-11-21T01:02:03.456789Z");
        assertThat(payload.get("reference").asText()).isEqualTo(event.reference());
        assertThat(payload.get("accountNumber").asText()).isEqualTo("1000009999");
        assertThat(payload.get("ownerId").asText()).isEqualTo("owner-1");
        assertThat(payload.get("amount").decimalValue()).isEqualByComparingTo("50.00");
        // type, version and key live in their own columns, not in the payload
        assertThat(payload.has("eventType")).isFalse();
        assertThat(payload.has("version")).isFalse();
        assertThat(payload.has("orderingKey")).isFalse();
    }

    @Test
    @DisplayName("a transfer is filed under the from-account; a foreign amount is nested, a MYR transfer has original = null")
    void transferPayload() throws Exception {
        TransferCompleted usd = new TransferCompleted(UUID.randomUUID(), AT, "TXN-USD", "1000000001", "alice",
                "1000000002", "bob", new BigDecimal("95.00"),
                new ForeignAmount("USD", new BigDecimal("20.00"), new BigDecimal("4.75")));
        TransferCompleted myr = new TransferCompleted(UUID.randomUUID(), AT, "TXN-MYR", "1000000001", "alice",
                "1000000002", "bob", new BigDecimal("30.00"), null);

        tx.executeWithoutResult(status -> {
            outbox.publish(usd);
            outbox.publish(myr);
        });

        Map<String, Object> usdRow = rowOf(usd);
        assertThat(usdRow.get("message_key")).isEqualTo("1000000001");
        assertThat(usdRow.get("event_type")).isEqualTo("TRANSFER_COMPLETED");
        JsonNode original = JSON.readTree((String) usdRow.get("payload")).get("original");
        assertThat(original.get("currency").asText()).isEqualTo("USD");
        assertThat(original.get("amount").decimalValue()).isEqualByComparingTo("20.00");
        assertThat(original.get("exchangeRate").decimalValue()).isEqualByComparingTo("4.75");

        assertThat(JSON.readTree((String) rowOf(myr).get("payload")).get("original").isNull()).isTrue();
    }

    @Test
    @DisplayName("rolled back together with the caller's transaction: no row")
    void rolledBack() {
        CashDeposited event = deposit();

        tx.executeWithoutResult(status -> {
            outbox.publish(event);
            status.setRollbackOnly();   // e.g. the transfer failed after the event was written
        });

        assertThat(countOf(event)).isZero();
    }

    @Test
    @DisplayName("outside a transaction it refuses to run (the insert would commit on its own) and stores nothing")
    void needsTransaction() {
        CashDeposited event = deposit();

        assertThatThrownBy(() -> outbox.publish(event)).isInstanceOf(IllegalTransactionStateException.class);
        assertThat(countOf(event)).isZero();
    }
}
