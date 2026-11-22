package com.Banking_app.alert.adapter.in.web;

import com.Banking_app.alert.application.port.in.command.CheckTransferCommand;
import com.Banking_app.alert.application.port.in.usecase.CheckTransferUseCase;
import com.Banking_app.support.AbstractIntegrationTest;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The admin API and the SQL behind the rules, against the real database. Kafka is off in this suite, so transfers are
 * checked by calling the use case directly, the way the Kafka listener does (end to end: AlertKafkaTest).
 * Each test uses an account number of its own; alerts are found by account or reference.
 */
class AdminAlertEndpointTest extends AbstractIntegrationTest {

    @Autowired
    private CheckTransferUseCase checkTransfer;

    @Autowired
    private JdbcTemplate jdbc;

    private static String newAccountNumber() {
        return "9" + String.format("%09d", Math.abs(UUID.randomUUID().getMostSignificantBits()) % 1_000_000_000L);
    }

    private static Instant now() {
        return Instant.now().truncatedTo(ChronoUnit.MICROS);
    }

    private CheckTransferCommand transfer(UUID eventId, String fromAccount, String amount, Instant at) {
        return new CheckTransferCommand(eventId, at, "TXN-" + eventId.toString().substring(0, 12), fromAccount,
                "owner-" + fromAccount, "1000000002", new BigDecimal(amount));
    }

    /** The newest 100 alerts, optionally filtered by status, as an admin. */
    private List<JsonNode> alerts(String query) {
        ResponseEntity<String> response = get("/api/admin/alerts?size=100" + query, adminToken());
        assertStatus(response, 200);
        List<JsonNode> list = new ArrayList<>();
        json(response).get("content").forEach(list::add);
        return list;
    }

    private List<JsonNode> alertsOf(String account, String query) {
        return alerts(query).stream().filter(a -> account.equals(a.get("accountNumber").asText())).toList();
    }

    private long openCount() {
        ResponseEntity<String> response = get("/api/admin/alerts/open-count", adminToken());
        assertStatus(response, 200);
        return json(response).get("open").asLong();
    }

    @Test
    @DisplayName("a large transfer → an OPEN LARGE_TRANSFER alert in the admin list, with every field")
    void largeTransferAlert() {
        String account = newAccountNumber();
        UUID eventId = UUID.randomUUID();
        Instant at = now();

        checkTransfer.check(transfer(eventId, account, "12000.00", at));

        assertThat(alertsOf(account, "&status=OPEN")).singleElement().satisfies(a -> {
            assertThat(a.get("id").asLong()).isPositive();
            assertThat(a.get("rule").asText()).isEqualTo("LARGE_TRANSFER");
            assertThat(a.get("status").asText()).isEqualTo("OPEN");
            assertThat(a.get("ownerId").asText()).isEqualTo("owner-" + account);
            assertThat(a.get("reference").asText()).isEqualTo("TXN-" + eventId.toString().substring(0, 12));
            assertThat(a.get("amount").decimalValue()).isEqualByComparingTo("12000.00");
            assertThat(a.get("detail").asText()).isEqualTo("Transfer of RM 12000.00 is at or above the RM 10000.00 limit");
            assertThat(a.get("createdAt").asText()).isEqualTo(at.toString());
            assertThat(a.get("reviewedAt").isNull()).isTrue();
        });
    }

    @Test
    @DisplayName("6 transfers in 6 minutes → one RAPID_TRANSFERS alert (counted in the database); the 7th adds none")
    void rapidTransfersThroughTheDatabase() {
        String account = newAccountNumber();
        Instant start = now().minus(Duration.ofMinutes(20));
        for (int i = 0; i < 7; i++) {
            checkTransfer.check(transfer(UUID.randomUUID(), account, "10.00", start.plus(Duration.ofMinutes(i))));
        }

        assertThat(alertsOf(account, "")).singleElement().satisfies(a -> {
            assertThat(a.get("rule").asText()).isEqualTo("RAPID_TRANSFERS");
            assertThat(a.get("detail").asText()).isEqualTo("6 transfers from this account within 10 minutes (limit 5)");
        });
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM alert_transfer_log WHERE account_number = ?",
                Integer.class, account)).isEqualTo(7);   // all inside the last 10 minutes of the burst
    }

    @Test
    @DisplayName("the log forgets an account's transfers older than the window")
    void logForgetsOldTransfers() {
        String account = newAccountNumber();
        Instant start = now().minus(Duration.ofHours(2));

        checkTransfer.check(transfer(UUID.randomUUID(), account, "10.00", start));
        checkTransfer.check(transfer(UUID.randomUUID(), account, "10.00", start.plus(Duration.ofHours(1))));

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM alert_transfer_log WHERE account_number = ?",
                Integer.class, account)).isEqualTo(1);
    }

    @Test
    @DisplayName("the same event checked twice (a redelivery) → one alert, one log entry")
    void redelivery() {
        String account = newAccountNumber();
        CheckTransferCommand large = transfer(UUID.randomUUID(), account, "50000.00", now());

        checkTransfer.check(large);
        checkTransfer.check(large);

        assertThat(alertsOf(account, "")).hasSize(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM alert_transfer_log WHERE account_number = ?",
                Integer.class, account)).isEqualTo(1);
    }

    @Test
    @DisplayName("PATCH /{id}/review → REVIEWED by this admin; again → unchanged; the open count goes down by one")
    void review() {
        String account = newAccountNumber();
        checkTransfer.check(transfer(UUID.randomUUID(), account, "12000.00", now()));
        long id = alertsOf(account, "").get(0).get("id").asLong();
        long openBefore = openCount();
        String adminId = userIdOf(adminToken());

        ResponseEntity<String> reviewed = patch("/api/admin/alerts/" + id + "/review", adminToken());
        ResponseEntity<String> again = patch("/api/admin/alerts/" + id + "/review", adminToken());

        assertStatus(reviewed, 200);
        assertThat(text(reviewed, "status")).isEqualTo("REVIEWED");
        assertThat(text(reviewed, "reviewedBy")).isEqualTo(adminId);
        assertThat(json(reviewed).get("reviewedAt").isNull()).isFalse();
        assertStatus(again, 200);
        assertThat(text(again, "reviewedAt")).isEqualTo(text(reviewed, "reviewedAt"));
        assertThat(openCount()).isEqualTo(openBefore - 1);
        assertThat(alertsOf(account, "&status=OPEN")).isEmpty();
        assertThat(alertsOf(account, "&status=REVIEWED")).hasSize(1);
    }

    @Test
    @DisplayName("unknown alert → 404; an unknown status or a bad page size → 400")
    void errors() {
        assertStatus(patch("/api/admin/alerts/999999999/review", adminToken()), 404);
        assertStatus(get("/api/admin/alerts?status=MAYBE", adminToken()), 400);
        assertStatus(get("/api/admin/alerts?size=0", adminToken()), 400);
    }

    @Test
    @DisplayName("customers get 403, no token 401: alerts are for admins only")
    void adminsOnly() {
        String customer = newCustomer("alert_cust").token();

        assertStatus(get("/api/admin/alerts", customer), 403);
        assertStatus(get("/api/admin/alerts/open-count", customer), 403);
        assertStatus(patch("/api/admin/alerts/1/review", customer), 403);
        assertStatus(get("/api/admin/alerts", null), 401);
    }
}
