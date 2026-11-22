package com.Banking_app.outbox;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionOperations;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** The relay's rules with a fake table and a fake broker: no Spring, no database, no Kafka. */
class OutboxRelayTest {

    private static final Instant NOW = Instant.parse("2026-11-21T10:00:00Z");

    private final FakeOutboxStore store = new FakeOutboxStore();
    private final List<OutboxMessage> sent = new ArrayList<>();
    private final Set<UUID> brokerRefuses = new HashSet<>();

    private final OutboxSender broker = message -> {
        if (brokerRefuses.contains(message.eventId())) {
            throw new IllegalStateException("broker unavailable");
        }
        sent.add(message);
    };

    private OutboxRelay relay(int batchSize) {
        return new OutboxRelay(store, broker, TransactionOperations.withoutTransaction(),
                Clock.fixed(NOW, ZoneOffset.UTC),
                new OutboxProperties(batchSize, Duration.ofSeconds(15), Duration.ofMinutes(1), Duration.ofDays(7)));
    }

    @Test
    @DisplayName("sends every waiting event in id order and stamps each with the time it was sent")
    void sendsInOrder() {
        OutboxMessage a = store.add("1000000001");
        OutboxMessage b = store.add("1000000002");
        OutboxMessage c = store.add("1000000001");

        relay(100).relayPending();

        assertThat(sent).containsExactly(a, b, c);
        assertThat(store.publishedAt).containsEntry(a.id(), NOW).containsEntry(b.id(), NOW).containsEntry(c.id(), NOW);
    }

    @Test
    @DisplayName("a failed send stops the round: earlier events stay sent, the failed one gets attempts + 1, later ones wait")
    void stopsAtFirstFailure() {
        OutboxMessage a = store.add("1000000001");
        OutboxMessage b = store.add("1000000001");
        OutboxMessage c = store.add("1000000001");
        brokerRefuses.add(b.eventId());

        relay(100).relayPending();

        assertThat(sent).containsExactly(a);
        assertThat(store.isPublished(a)).isTrue();
        assertThat(store.isPublished(b)).isFalse();
        assertThat(store.attempts).containsEntry(b.id(), 1);
        assertThat(store.isPublished(c)).isFalse();   // c may not overtake b
        assertThat(store.attempts).doesNotContainKey(c.id());
    }

    @Test
    @DisplayName("the next round starts again at the failed event; once the broker is back, everything goes out in order")
    void retriesNextRound() {
        OutboxMessage a = store.add("1000000001");
        OutboxMessage b = store.add("1000000001");
        brokerRefuses.add(a.eventId());
        OutboxRelay relay = relay(100);

        relay.relayPending();
        relay.relayPending();
        assertThat(store.attempts).containsEntry(a.id(), 2);
        assertThat(sent).isEmpty();

        brokerRefuses.clear();   // Kafka is back
        relay.relayPending();

        assertThat(sent).containsExactly(a, b);
        assertThat(store.backlog().waiting()).isZero();
    }

    @Test
    @DisplayName("a backlog bigger than one batch is drained in one round, batch after batch")
    void drainsSeveralBatches() {
        for (int i = 0; i < 5; i++) {
            store.add("100000000" + i);
        }

        relay(2).relayPending();

        assertThat(sent).hasSize(5);
        assertThat(store.batchesRead).isEqualTo(3);   // 2 + 2 + 1: the short batch ends the round
    }

    @Test
    @DisplayName("while another app instance holds the relay lock, this one sends nothing")
    void anotherInstanceIsRelaying() {
        store.add("1000000001");
        store.relayLockFree = false;

        relay(100).relayPending();

        assertThat(sent).isEmpty();
        assertThat(store.batchesRead).isZero();
    }

    @Test
    @DisplayName("an empty outbox: one look, nothing sent")
    void emptyOutbox() {
        relay(100).relayPending();

        assertThat(sent).isEmpty();
        assertThat(store.batchesRead).isEqualTo(1);
    }
}
