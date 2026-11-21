package com.Banking_app.alert;

import com.Banking_app.support.AbstractKafkaIntegrationTest;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * End to end with the real broker: a large transfer through the API → outbox → relay → topic → read by BOTH consumer
 * groups: the alert module raises an alert for admins, the notification module tells the recipient.
 */
class AlertKafkaTest extends AbstractKafkaIntegrationTest {

    private List<JsonNode> contentOf(String path, String token) {
        ResponseEntity<String> response = get(path, token);
        assertStatus(response, 200);
        List<JsonNode> list = new ArrayList<>();
        json(response).get("content").forEach(list::add);
        return list;
    }

    @Test
    @DisplayName("a RM 12,000 transfer → a LARGE_TRANSFER alert for admins, and the recipient is still notified")
    void largeTransferRaisesAlertAndNotifies() {
        Customer alice = newCustomer("kalert_alice");
        Customer bob = newCustomer("kalert_bob");
        topUp(alice.token(), alice.account(), "15000.00");

        ResponseEntity<String> response = transfer(alice.token(), alice.account(), bob.account(), "12000.00",
                UUID.randomUUID().toString());
        assertStatus(response, 201);
        String reference = text(response, "transactionReference");

        await().atMost(Duration.ofSeconds(30)).untilAsserted(() ->
                assertThat(contentOf("/api/admin/alerts?status=OPEN&size=100", adminToken()))
                        .anySatisfy(a -> {
                            assertThat(a.get("reference").asText()).isEqualTo(reference);
                            assertThat(a.get("rule").asText()).isEqualTo("LARGE_TRANSFER");
                            assertThat(a.get("accountNumber").asText()).isEqualTo(alice.account().number());
                        }));
        await().atMost(Duration.ofSeconds(30)).untilAsserted(() ->
                assertThat(contentOf("/api/notifications?size=100", bob.token()))
                        .anySatisfy(n -> assertThat(n.get("reference").asText()).isEqualTo(reference)));
    }
}
