package com.Banking_app.transaction.adapter.in.web;

import com.Banking_app.support.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Idempotency keys over real HTTP, including requests sent at the same moment (plan, Phase 4 item 15).
 * TransferEndpointTest / TransferSecurityTest already cover the sequential duplicate and the key of another account.
 */
class TransferIdempotencyTest extends AbstractIntegrationTest {

    private static final String DUPLICATE = "Duplicate request detected. Returning existing transaction.";
    private static final String SUCCESS = "Transfer successful";
    private static final String OTHER_ACCOUNTS_KEY = "idempotencyKey has already been used for a different transfer";

    private static String key() {
        return "idem-" + UUID.randomUUID();
    }

    /** Starts both calls at the same moment on two threads and waits for both answers. */
    private static List<ResponseEntity<String>> atTheSameTime(Callable<ResponseEntity<String>> first,
                                                             Callable<ResponseEntity<String>> second) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            CountDownLatch go = new CountDownLatch(1);
            Future<ResponseEntity<String>> a = pool.submit(() -> { go.await(); return first.call(); });
            Future<ResponseEntity<String>> b = pool.submit(() -> { go.await(); return second.call(); });
            go.countDown();
            return List.of(a.get(30, TimeUnit.SECONDS), b.get(30, TimeUnit.SECONDS));
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    @DisplayName("same key, same account, different amount or recipient → 409; nothing moves a second time")
    void sameKeyDifferentTransfer() {
        Customer alice = newCustomer("id_alice");
        Customer bob = newCustomer("id_bob");
        Customer carol = newCustomer("id_carol");
        topUp(alice.token(), alice.account(), "100.00");
        String key = key();
        assertStatus(transfer(alice.token(), alice.account(), bob.account(), "30.00", key), 201);

        ResponseEntity<String> otherAmount = transfer(alice.token(), alice.account(), bob.account(), "31.00", key);
        ResponseEntity<String> otherRecipient = transfer(alice.token(), alice.account(), carol.account(), "30.00", key);

        for (ResponseEntity<String> response : List.of(otherAmount, otherRecipient)) {
            assertStatus(response, 409);
            assertThat(text(response, "message"))
                    .isEqualTo("idempotencyKey has already been used for a transfer with a different amount or recipient");
        }
        assertThat(balanceOf(alice.account())).isEqualByComparingTo("90.00");
        assertThat(balanceOf(bob.account())).isEqualByComparingTo("50.00");
        assertThat(balanceOf(carol.account())).isEqualByComparingTo("20.00");
    }

    @Test
    @DisplayName("two identical requests at the same moment → one transfer, one duplicate reply, debited once")
    void identicalRequestsAtTheSameMoment() throws Exception {
        Customer alice = newCustomer("id_alice");
        Customer bob = newCustomer("id_bob");
        topUp(alice.token(), alice.account(), "100.00");
        String key = key();

        List<ResponseEntity<String>> responses = atTheSameTime(
                () -> transfer(alice.token(), alice.account(), bob.account(), "30.00", key),
                () -> transfer(alice.token(), alice.account(), bob.account(), "30.00", key));

        for (ResponseEntity<String> response : responses) {
            assertStatus(response, 201);
        }
        assertThat(text(responses.get(0), "transactionReference"))
                .isEqualTo(text(responses.get(1), "transactionReference"));
        Set<String> messages = responses.stream().map(r -> text(r, "message")).collect(Collectors.toSet());
        assertThat(messages).containsExactlyInAnyOrder(SUCCESS, DUPLICATE);
        assertThat(balanceOf(alice.account())).isEqualByComparingTo("90.00");
        assertThat(balanceOf(bob.account())).isEqualByComparingTo("50.00");
    }

    @Test
    @DisplayName("same key from two accounts at the same moment → one 201 and one 400, never a 500; money moves once")
    void sameKeyFromTwoAccountsAtTheSameMoment() throws Exception {
        // Repeated a few times: whether the two requests really overlap depends on timing. Either way the answers
        // must be 201 + 400 (the overlapping case used to give a 500 from the unique constraint).
        for (int round = 0; round < 3; round++) {
            Customer alice = newCustomer("id_alice");
            Customer bob = newCustomer("id_bob");
            Customer carol = newCustomer("id_carol");
            Customer dave = newCustomer("id_dave");   // separate recipients: no shared row lock between the two
            topUp(alice.token(), alice.account(), "100.00");
            topUp(carol.token(), carol.account(), "100.00");
            String key = key();

            List<ResponseEntity<String>> responses = atTheSameTime(
                    () -> transfer(alice.token(), alice.account(), bob.account(), "10.00", key),
                    () -> transfer(carol.token(), carol.account(), dave.account(), "10.00", key));

            List<Integer> statuses = responses.stream().map(r -> r.getStatusCode().value()).sorted().toList();
            assertThat(statuses).as("round %d: %s", round, responses).containsExactly(201, 400);
            ResponseEntity<String> refused = responses.stream()
                    .filter(r -> r.getStatusCode().value() == 400).findFirst().orElseThrow();
            assertThat(text(refused, "message")).isEqualTo(OTHER_ACCOUNTS_KEY);

            BigDecimal senders = balanceOf(alice.account()).add(balanceOf(carol.account()));
            BigDecimal recipients = balanceOf(bob.account()).add(balanceOf(dave.account()));
            assertThat(senders).isEqualByComparingTo("230.00");     // 120 + 120 - 10
            assertThat(recipients).isEqualByComparingTo("50.00");   //  20 +  20 + 10
        }
    }
}
