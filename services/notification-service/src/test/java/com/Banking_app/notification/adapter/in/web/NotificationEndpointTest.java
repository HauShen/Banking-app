package com.Banking_app.notification.adapter.in.web;

import com.Banking_app.notification.application.port.in.command.CashDepositedNotice;
import com.Banking_app.notification.application.port.in.usecase.RecordNotificationsUseCase;
import com.Banking_app.notification.support.AbstractIntegrationTest;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The REST API against the real database. Kafka is off in this suite, so notifications are created by calling the
 * use case directly, the way the Kafka listener does (end to end through Kafka: NotificationKafkaTest). The token
 * checks themselves: AccessTokenTest.
 */
class NotificationEndpointTest extends AbstractIntegrationTest {

    private static final Instant T0 = Instant.parse("2026-11-21T08:00:00Z");

    @Autowired
    private RecordNotificationsUseCase recordNotifications;

    @Autowired
    private JdbcTemplate jdbc;

    /** A CASH_DEPOSITED notification for {@code user}, into {@code accountNumber}; returns its reference. */
    private String deposit(User user, String accountNumber, String amount, Instant at) {
        String reference = "CDM-" + UUID.randomUUID().toString().substring(0, 12);
        recordNotifications.cashDeposited(new CashDepositedNotice(UUID.randomUUID(), at, reference,
                accountNumber, user.id(), new BigDecimal(amount)));
        return reference;
    }

    private String deposit(User user, String amount, Instant at) {
        return deposit(user, accountNumber(), amount, at);
    }

    private JsonNode list(User customer, String query) {
        ResponseEntity<String> response = get("/api/notifications" + query, customer.token());
        assertStatus(response, 200);
        return json(response);
    }

    private static List<String> references(JsonNode page) {
        List<String> references = new ArrayList<>();
        page.get("content").forEach(n -> references.add(n.get("reference").asText()));
        return references;
    }

    private long unreadCount(User customer) {
        ResponseEntity<String> response = get("/api/notifications/unread-count", customer.token());
        assertStatus(response, 200);
        return json(response).get("unread").asLong();
    }

    @Test
    @DisplayName("GET: the user's own notifications, newest first, with every field; nobody else sees them")
    void listOwn() {
        User bob = newUser();
        User alice = newUser();
        String bobsAccount = accountNumber();
        String older = deposit(bob, bobsAccount, "10.00", T0);
        String newer = deposit(bob, bobsAccount, "25.50", T0.plusSeconds(60));

        JsonNode page = list(bob, "");

        assertThat(references(page)).containsExactly(newer, older);
        assertThat(page.get("totalElements").asLong()).isEqualTo(2);
        JsonNode first = page.get("content").get(0);
        assertThat(first.get("id").asLong()).isPositive();
        assertThat(first.get("type").asText()).isEqualTo("CASH_DEPOSITED");
        assertThat(first.get("message").asText()).isEqualTo("RM 25.50 was deposited into your account ****"
                + bobsAccount.substring(bobsAccount.length() - 4) + ".");
        assertThat(first.get("amount").decimalValue()).isEqualByComparingTo("25.50");
        assertThat(first.get("createdAt").asText()).isEqualTo("2026-11-21T08:01:00Z");
        assertThat(first.get("read").asBoolean()).isFalse();

        assertThat(list(alice, "").get("totalElements").asLong()).isZero();
    }

    @Test
    @DisplayName("PATCH /{id}/read → 204: it is read, the unread count drops, unreadOnly leaves it out")
    void markRead() {
        User bob = newUser();
        deposit(bob, "10.00", T0);
        String newer = deposit(bob, "20.00", T0.plusSeconds(60));
        long newerId = list(bob, "").get("content").get(0).get("id").asLong();
        assertThat(unreadCount(bob)).isEqualTo(2);

        assertStatus(patch("/api/notifications/" + newerId + "/read", bob.token()), 204);
        assertStatus(patch("/api/notifications/" + newerId + "/read", bob.token()), 204);   // again: still fine

        assertThat(unreadCount(bob)).isEqualTo(1);
        assertThat(list(bob, "").get("content").get(0).get("read").asBoolean()).isTrue();
        assertThat(references(list(bob, "?unreadOnly=true"))).doesNotContain(newer).hasSize(1);
    }

    @Test
    @DisplayName("someone else's notification or an unknown id → the same 404, and it stays unread; a non-number → 400")
    void markReadNotFound() {
        User bob = newUser();
        User alice = newUser();
        deposit(bob, "10.00", T0);
        long bobsId = list(bob, "").get("content").get(0).get("id").asLong();

        ResponseEntity<String> stranger = patch("/api/notifications/" + bobsId + "/read", alice.token());
        assertStatus(stranger, 404);
        assertThat(text(stranger, "message")).isEqualTo("Notification not found");
        assertStatus(patch("/api/notifications/999999999/read", bob.token()), 404);
        assertStatus(patch("/api/notifications/abc/read", bob.token()), 400);
        assertThat(unreadCount(bob)).isEqualTo(1);
    }

    @Test
    @DisplayName("POST /read-all: marks all of the user's unread notifications and says how many")
    void readAll() {
        User bob = newUser();
        User alice = newUser();
        deposit(bob, "10.00", T0);
        deposit(bob, "20.00", T0);
        deposit(alice, "30.00", T0);

        ResponseEntity<String> response = post("/api/notifications/read-all", bob.token(), Map.of());

        assertStatus(response, 200);
        assertThat(json(response).get("marked").asInt()).isEqualTo(2);
        assertThat(unreadCount(bob)).isZero();
        assertThat(unreadCount(alice)).isEqualTo(1);
    }

    @Test
    @DisplayName("paging: ?page=1&size=1 → the older one; size 0 or 101, page -1 → 400")
    void paging() {
        User bob = newUser();
        String older = deposit(bob, "10.00", T0);
        deposit(bob, "20.00", T0.plusSeconds(60));

        assertThat(references(list(bob, "?page=1&size=1"))).containsExactly(older);
        assertStatus(get("/api/notifications?size=0", bob.token()), 400);
        assertStatus(get("/api/notifications?size=101", bob.token()), 400);
        assertStatus(get("/api/notifications?page=-1", bob.token()), 400);
    }

    @Test
    @DisplayName("without a token → 401 on every endpoint")
    void noToken() {
        assertStatus(get("/api/notifications", null), 401);
        assertStatus(get("/api/notifications/unread-count", null), 401);
        assertStatus(patch("/api/notifications/1/read", null), 401);
        assertStatus(post("/api/notifications/read-all", null, Map.of()), 401);
    }

    @Test
    @DisplayName("the same event handled twice (a redelivery) → one notification, one processed_events row")
    void sameEventTwice() {
        User bob = newUser();
        UUID eventId = UUID.randomUUID();
        CashDepositedNotice notice = new CashDepositedNotice(eventId, T0, "CDM-TWICE-" + eventId.toString().substring(0, 8),
                accountNumber(), bob.id(), new BigDecimal("10.00"));

        recordNotifications.cashDeposited(notice);
        recordNotifications.cashDeposited(notice);

        assertThat(list(bob, "").get("totalElements").asLong()).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM processed_events WHERE event_id = ?", Integer.class,
                eventId)).isEqualTo(1);
    }
}
