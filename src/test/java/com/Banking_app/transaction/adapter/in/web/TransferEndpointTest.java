package com.Banking_app.transaction.adapter.in.web;

import com.Banking_app.support.AbstractIntegrationTest;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * /api/transfers over real HTTP: the "behaviour that must stay identical" checklist of the transaction plan
 * (status codes, messages, JSON shapes). TransferSecurityTest covers who may do what; this class covers the rest.
 * Every expected value was taken from the old controllers.TransferController / TransactionServiceImpl.
 */
class TransferEndpointTest extends AbstractIntegrationTest {

    private static String key() {
        return "endpoint-" + UUID.randomUUID();
    }

    private static String message(ResponseEntity<String> response) {
        return json(response).get("message").asText();
    }

    /** For account numbers that don't exist (the helper's Account record can't be created outside its package). */
    private ResponseEntity<String> transferByNumbers(String token, String fromNumber, String toNumber) {
        return post("/api/transfers", token, Map.of(
                "fromAccountNumber", fromNumber,
                "toAccountNumber", toNumber,
                "amount", new BigDecimal("1.00"),
                "currency", "MYR",
                "description", "unknown account test",
                "idempotencyKey", key()));
    }

    private void lock(Account account) {
        assertStatus(patch("/api/accounts/" + account.id() + "/status?status=LOCKED", adminToken()), 200);
    }

    @Nested
    @DisplayName("POST /api/transfers")
    class Transfer {

        @Test
        @DisplayName("success → 201 with reference, SUCCESS, createdAt and 'Transfer successful'")
        void success() {
            Customer alice = newCustomer("ep_alice");
            Customer bob = newCustomer("ep_bob");
            topUp(alice.token(), alice.account(), "100.00");

            ResponseEntity<String> response = transfer(alice.token(), alice.account(), bob.account(), "30.00", key());

            assertStatus(response, 201);
            assertThat(text(response, "transactionReference")).matches("TXN-[0-9A-F]{12}");
            assertThat(text(response, "status")).isEqualTo("SUCCESS");
            assertThat(text(response, "message")).isEqualTo("Transfer successful");
            assertThat(json(response).hasNonNull("createdAt")).isTrue();
            assertThat(balanceOf(alice.account())).isEqualByComparingTo("90.00");
            assertThat(balanceOf(bob.account())).isEqualByComparingTo("50.00");
        }

        @Test
        @DisplayName("same key again → 201 with the original reference and the duplicate message")
        void duplicate() {
            Customer alice = newCustomer("ep_alice");
            Customer bob = newCustomer("ep_bob");
            topUp(alice.token(), alice.account(), "100.00");
            String key = key();
            ResponseEntity<String> first = transfer(alice.token(), alice.account(), bob.account(), "30.00", key);

            ResponseEntity<String> again = transfer(alice.token(), alice.account(), bob.account(), "30.00", key);

            assertStatus(again, 201);
            assertThat(text(again, "transactionReference")).isEqualTo(text(first, "transactionReference"));
            assertThat(text(again, "status")).isEqualTo("SUCCESS");
            assertThat(text(again, "message")).isEqualTo("Duplicate request detected. Returning existing transaction.");
            assertThat(balanceOf(alice.account())).isEqualByComparingTo("90.00");
        }

        @Test
        @DisplayName("a duplicate reply shows exactly the original createdAt, which is also the stored time")
        void duplicateReplyKeepsOriginalCreatedAt() {
            Customer alice = newCustomer("ep_alice");
            Customer bob = newCustomer("ep_bob");
            topUp(alice.token(), alice.account(), "100.00");
            String key = key();

            ResponseEntity<String> first = transfer(alice.token(), alice.account(), bob.account(), "30.00", key);
            ResponseEntity<String> again = transfer(alice.token(), alice.account(), bob.account(), "30.00", key);
            ResponseEntity<String> stored = get("/api/transfers/" + text(first, "transactionReference"), alice.token());

            Instant firstCreatedAt = Instant.parse(text(first, "createdAt"));
            assertThat(Instant.parse(text(again, "createdAt"))).isEqualTo(firstCreatedAt);    // was: a later now()
            assertThat(Instant.parse(text(stored, "createdAt"))).isEqualTo(firstCreatedAt);   // same as in the DB
        }

        @Test
        @DisplayName("same account on both sides → 400")
        void sameAccount() {
            Customer alice = newCustomer("ep_alice");

            ResponseEntity<String> response = transfer(alice.token(), alice.account(), alice.account(), "1.00", key());

            assertStatus(response, 400);
            assertThat(message(response)).isEqualTo("Cannot transfer to the same account");
        }

        @Test
        @DisplayName("unknown to-account → 404 'To account not found'; unknown from-account → 404 'From account not found'")
        void unknownAccounts() {
            Customer alice = newCustomer("ep_alice");
            String real = alice.account().number();

            ResponseEntity<String> unknownTo = transferByNumbers(alice.token(), real, "ZZZZZZZZZZ");
            ResponseEntity<String> unknownFrom = transferByNumbers(alice.token(), "0000000000", real);

            assertStatus(unknownTo, 404);
            assertThat(message(unknownTo)).isEqualTo("To account not found");
            assertStatus(unknownFrom, 404);
            assertThat(message(unknownFrom)).isEqualTo("From account not found");
        }

        @Test
        @DisplayName("below the minimum balance → 400, no money moves")
        void insufficientBalance() {
            Customer alice = newCustomer("ep_alice");
            Customer bob = newCustomer("ep_bob");

            ResponseEntity<String> response = transfer(alice.token(), alice.account(), bob.account(), "0.01", key());

            assertStatus(response, 400);
            assertThat(message(response))
                    .isEqualTo("Insufficient balance. Minimum balance of $20.00 must be maintained.");
            assertThat(balanceOf(alice.account())).isEqualByComparingTo("20.00");
            assertThat(balanceOf(bob.account())).isEqualByComparingTo("20.00");
        }

        @Test
        @DisplayName("locked from-account / locked to-account → 400 with the old messages")
        void lockedAccounts() {
            Customer alice = newCustomer("ep_alice");
            Customer bob = newCustomer("ep_bob");
            Customer carol = newCustomer("ep_carol");
            topUp(alice.token(), alice.account(), "100.00");
            topUp(carol.token(), carol.account(), "100.00");
            lock(bob.account());
            lock(carol.account());

            ResponseEntity<String> toLocked = transfer(alice.token(), alice.account(), bob.account(), "1.00", key());
            ResponseEntity<String> fromLocked = transfer(carol.token(), carol.account(), alice.account(), "1.00", key());

            assertStatus(toLocked, 400);
            assertThat(message(toLocked)).isEqualTo("Selected account has been locked");
            assertStatus(fromLocked, 400);
            assertThat(message(fromLocked)).isEqualTo("Cannot process transfer with an locked account.");
        }

        @Test
        @DisplayName("more than 2 decimals → 400 with a field error, no money moves (was: silently rounded)")
        void moreThanTwoDecimals() {
            Customer alice = newCustomer("ep_alice");
            Customer bob = newCustomer("ep_bob");
            topUp(alice.token(), alice.account(), "100.00");

            ResponseEntity<String> response = transfer(alice.token(), alice.account(), bob.account(), "10.005", key());

            assertStatus(response, 400);
            assertThat(json(response).get("fieldErrors").get("amount").asText())
                    .isEqualTo("amount can have at most 2 decimal places and 17 digits before the decimal point");
            assertThat(balanceOf(alice.account())).isEqualByComparingTo("120.00");
            assertThat(balanceOf(bob.account())).isEqualByComparingTo("20.00");
        }

        @Test
        @DisplayName("decimals count as written: 10.500 → 400, 10.5 → 201 and moves 10.50")
        void decimalsCountAsWritten() {
            Customer alice = newCustomer("ep_alice");
            Customer bob = newCustomer("ep_bob");
            topUp(alice.token(), alice.account(), "100.00");

            assertStatus(transfer(alice.token(), alice.account(), bob.account(), "10.500", key()), 400);
            assertStatus(transfer(alice.token(), alice.account(), bob.account(), "10.5", key()), 201);

            assertThat(balanceOf(alice.account())).isEqualByComparingTo("109.50");
            assertThat(balanceOf(bob.account())).isEqualByComparingTo("30.50");
        }

        @Test
        @DisplayName("more than 17 digits before the decimal point → 400 (was: database overflow → 500)")
        void tooLarge() {
            Customer alice = newCustomer("ep_alice");
            Customer bob = newCustomer("ep_bob");

            ResponseEntity<String> response = transfer(alice.token(), alice.account(), bob.account(),
                    "123456789012345678.00", key());

            assertStatus(response, 400);
            assertThat(json(response).get("fieldErrors").has("amount")).isTrue();
        }

        @Test
        @DisplayName("request validation: missing amount, zero amount, bad currency, blank key → 400")
        void validation() {
            Customer alice = newCustomer("ep_alice");
            Customer bob = newCustomer("ep_bob");
            Map<String, Object> valid = Map.of(
                    "fromAccountNumber", alice.account().number(),
                    "toAccountNumber", bob.account().number(),
                    "amount", new BigDecimal("1.00"),
                    "currency", "MYR",
                    "description", "validation test",
                    "idempotencyKey", key());

            for (Map.Entry<String, Object> broken : Map.<String, Object>of(
                    "amount", BigDecimal.ZERO,
                    "currency", "myr",
                    "idempotencyKey", " ").entrySet()) {
                Map<String, Object> body = new HashMap<>(valid);
                body.put(broken.getKey(), broken.getValue());
                assertStatus(post("/api/transfers", alice.token(), body), 400);
            }
            Map<String, Object> noAmount = new HashMap<>(valid);
            noAmount.remove("amount");
            assertStatus(post("/api/transfers", alice.token(), noAmount), 400);
        }
    }

    @Nested
    @DisplayName("GET /api/transfers/...")
    class Reads {

        @Test
        @DisplayName("history page: newest first, all fields, 'Cash Deposit Machine' for top-ups, Spring Page JSON")
        void historyPage() {
            Customer alice = newCustomer("ep_alice");
            Customer bob = newCustomer("ep_bob");
            topUp(alice.token(), alice.account(), "100.00");
            String reference = text(transfer(alice.token(), alice.account(), bob.account(), "30.00", key()),
                    "transactionReference");

            ResponseEntity<String> response = get("/api/transfers/account/" + alice.account().number(), alice.token());

            assertStatus(response, 200);
            JsonNode page = json(response);
            JsonNode transfer = page.get("content").get(0);
            assertThat(transfer.get("reference").asText()).isEqualTo(reference);
            assertThat(transfer.get("fromAccountNumber").asText()).isEqualTo(alice.account().number());
            assertThat(transfer.get("toAccountNumber").asText()).isEqualTo(bob.account().number());
            assertThat(transfer.get("amount").decimalValue()).isEqualByComparingTo("30.00");
            assertThat(transfer.get("currency").asText()).isEqualTo("MYR");
            assertThat(transfer.get("status").asText()).isEqualTo("SUCCESS");
            assertThat(transfer.get("description").asText()).isEqualTo("integration test transfer");
            assertThat(transfer.hasNonNull("createdAt")).isTrue();
            assertThat(transfer.hasNonNull("postedAt")).isTrue();
            assertThat(transfer.has("toAccountUserName")).isFalse();   // removed in known-limitations Phase A

            JsonNode topUp = page.get("content").get(1);
            assertThat(topUp.get("fromAccountNumber").asText()).isEqualTo("Cash Deposit Machine");
            assertThat(topUp.get("toAccountNumber").asText()).isEqualTo(alice.account().number());

            // the Spring Data Page fields the frontend may read
            assertThat(page.get("totalElements").asLong()).isEqualTo(2);
            assertThat(page.get("totalPages").asInt()).isEqualTo(1);
            assertThat(page.get("number").asInt()).isZero();
            assertThat(page.get("size").asInt()).isEqualTo(10);
            assertThat(page.get("numberOfElements").asInt()).isEqualTo(2);
            assertThat(page.get("first").asBoolean()).isTrue();
            assertThat(page.get("last").asBoolean()).isTrue();
            assertThat(page.get("empty").asBoolean()).isFalse();
            assertThat(page.get("sort").get("sorted").asBoolean()).isTrue();
            assertThat(page.get("pageable").get("pageSize").asInt()).isEqualTo(10);
        }

        @Test
        @DisplayName("history paging: ?page=1&size=1 returns the older row")
        void historyPaging() {
            Customer alice = newCustomer("ep_alice");
            Customer bob = newCustomer("ep_bob");
            topUp(alice.token(), alice.account(), "100.00");
            transfer(alice.token(), alice.account(), bob.account(), "30.00", key());

            JsonNode page = json(get("/api/transfers/account/" + alice.account().number() + "?page=1&size=1",
                    alice.token()));

            assertThat(page.get("content")).hasSize(1);
            assertThat(page.get("content").get(0).get("fromAccountNumber").asText()).isEqualTo("Cash Deposit Machine");
            assertThat(page.get("totalPages").asInt()).isEqualTo(2);
            assertThat(page.get("last").asBoolean()).isTrue();
        }

        @Test
        @DisplayName("invalid paging (size=0, page=-1) → 400")
        void invalidPaging() {
            Customer alice = newCustomer("ep_alice");
            String base = "/api/transfers/account/" + alice.account().number();

            assertStatus(get(base + "?size=0", alice.token()), 400);
            assertStatus(get(base + "?page=-1", alice.token()), 400);
        }

        @Test
        @DisplayName("single transaction by reference: same fields as in the history")
        void byReference() {
            Customer alice = newCustomer("ep_alice");
            Customer bob = newCustomer("ep_bob");
            topUp(alice.token(), alice.account(), "100.00");
            String reference = text(transfer(alice.token(), alice.account(), bob.account(), "30.00", key()),
                    "transactionReference");

            ResponseEntity<String> response = get("/api/transfers/" + reference, alice.token());

            assertStatus(response, 200);
            assertThat(text(response, "reference")).isEqualTo(reference);
            assertThat(text(response, "fromAccountNumber")).isEqualTo(alice.account().number());
            assertThat(text(response, "toAccountNumber")).isEqualTo(bob.account().number());
            assertThat(text(response, "currency")).isEqualTo("MYR");
        }

        @Test
        @DisplayName("unknown reference → 404 'Transaction not found'; unknown account → 404 'Account not found: n'")
        void notFound() {
            Customer alice = newCustomer("ep_alice");

            ResponseEntity<String> unknownReference = get("/api/transfers/TXN-DOESNOTEXIST", alice.token());
            ResponseEntity<String> unknownAccount = get("/api/transfers/account/0000000000", alice.token());

            assertStatus(unknownReference, 404);
            assertThat(message(unknownReference)).isEqualTo("Transaction not found");
            assertStatus(unknownAccount, 404);
            assertThat(message(unknownAccount)).isEqualTo("Account not found: 0000000000");
        }
    }
}
