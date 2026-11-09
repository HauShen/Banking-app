package com.Banking_app.transaction.adapter.in.web;

import com.Banking_app.support.AbstractIntegrationTest;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * POST /api/admin/remittances over real HTTP, with the round test rates from src/test/resources/application.properties
 * (buy: USD 4.60, SGD 3.40, EUR 4.90, JPY 0.0290). Money from abroad is converted at the buy rate and credited to a
 * MYR account; the history shows it as "Foreign Remittance".
 */
class RemittanceEndpointTest extends AbstractIntegrationTest {

    private static final String URL = "/api/admin/remittances";

    private static String key() {
        return "gateway-" + UUID.randomUUID();
    }

    private static String message(ResponseEntity<String> response) {
        return json(response).get("message").asText();
    }

    private static Map<String, Object> body(String toAccountNumber, String currency, String amount, String key) {
        return new HashMap<>(Map.of(
                "toAccountNumber", toAccountNumber,
                "currency", currency,
                "amount", new BigDecimal(amount),
                "senderName", "John Smith",
                "idempotencyKey", key));
    }

    private ResponseEntity<String> remit(Customer to, String currency, String amount, String key) {
        return post(URL, adminToken(), body(to.account().number(), currency, amount, key));
    }

    @Nested
    @DisplayName("receiving money from abroad")
    class Receiving {

        @Test
        @DisplayName("admin: 100.00 USD → 201, RM 460.00 credited at the buy rate 4.60; reference RMT-…")
        void usd() {
            Customer bob = newCustomer("rmt_bob");

            ResponseEntity<String> response = remit(bob, "USD", "100.00", key());

            assertStatus(response, 201);
            JsonNode body = json(response);
            assertThat(body.get("transactionReference").asText()).matches("RMT-[0-9A-F]{12}");
            assertThat(body.get("status").asText()).isEqualTo("SUCCESS");
            assertThat(body.get("message").asText()).isEqualTo("Remittance received");
            assertThat(body.get("toAccountNumber").asText()).isEqualTo(bob.account().number());
            assertThat(body.get("amount").decimalValue()).isEqualByComparingTo("460.00");
            assertThat(body.get("originalCurrency").asText()).isEqualTo("USD");
            assertThat(body.get("originalAmount").decimalValue()).isEqualByComparingTo("100.00");
            assertThat(body.get("exchangeRate").decimalValue()).isEqualByComparingTo("4.60");
            assertThat(body.hasNonNull("createdAt")).isTrue();
            assertThat(balanceOf(bob.account())).isEqualByComparingTo("480.00");
        }

        @Test
        @DisplayName("the recipient sees it: type FOREIGN_REMITTANCE, from 'Foreign Remittance', sender in the description")
        void history() {
            Customer bob = newCustomer("rmt_bob");
            String reference = text(remit(bob, "JPY", "10000", key()), "transactionReference");

            JsonNode tx = json(get("/api/transfers/" + reference, bob.token()));
            assertThat(tx.get("type").asText()).isEqualTo("FOREIGN_REMITTANCE");
            assertThat(tx.get("fromAccountNumber").asText()).isEqualTo("Foreign Remittance");
            assertThat(tx.get("toAccountNumber").asText()).isEqualTo(bob.account().number());
            assertThat(tx.get("amount").decimalValue()).isEqualByComparingTo("290.00");
            assertThat(tx.get("currency").asText()).isEqualTo("MYR");
            assertThat(tx.get("originalCurrency").asText()).isEqualTo("JPY");
            assertThat(tx.get("originalAmount").asText()).isEqualTo("10000");
            assertThat(tx.get("exchangeRate").decimalValue()).isEqualByComparingTo("0.0290");
            assertThat(tx.get("description").asText()).isEqualTo("Foreign remittance from John Smith");

            JsonNode row = null;
            for (JsonNode candidate : json(get("/api/transfers/account/" + bob.account().number(), bob.token()))
                    .get("content")) {
                if (candidate.get("reference").asText().equals(reference)) {
                    row = candidate;
                }
            }
            assertThat(row).as("the remittance in Bob's history").isNotNull();
            assertThat(row.get("fromAccountNumber").asText()).isEqualTo("Foreign Remittance");
        }

        @Test
        @DisplayName("an account that received a remittance has history, so it can't be deleted → 409")
        void deleteAfterRemittance() {
            Customer bob = newCustomer("rmt_bob");
            assertStatus(remit(bob, "USD", "1.00", key()), 201);

            assertStatus(delete("/api/accounts/" + bob.account().id(), adminToken()), 409);
        }
    }

    @Nested
    @DisplayName("who may call it")
    class Access {

        @Test
        @DisplayName("a customer → 403, even for their own account; nothing is credited")
        void customer() {
            Customer bob = newCustomer("rmt_bob");

            assertStatus(post(URL, bob.token(), body(bob.account().number(), "USD", "100.00", key())), 403);
            assertThat(balanceOf(bob.account())).isEqualByComparingTo("20.00");
        }

        @Test
        @DisplayName("no token → 401")
        void anonymous() {
            Customer bob = newCustomer("rmt_bob");

            assertStatus(post(URL, null, body(bob.account().number(), "USD", "100.00", key())), 401);
        }
    }

    @Nested
    @DisplayName("refused")
    class Refused {

        @Test
        @DisplayName("MYR → 400 'MYR needs no conversion' (that's a top-up); unsupported → 400; JPY decimals → 400")
        void badCurrencyOrAmount() {
            Customer bob = newCustomer("rmt_bob");

            ResponseEntity<String> myr = remit(bob, "MYR", "100.00", key());
            ResponseEntity<String> unsupported = remit(bob, "XYZ", "100.00", key());
            ResponseEntity<String> jpyDecimals = remit(bob, "JPY", "1000.50", key());

            assertStatus(myr, 400);
            assertThat(message(myr)).isEqualTo("MYR needs no conversion");
            assertStatus(unsupported, 400);
            assertThat(message(unsupported)).isEqualTo("Currency XYZ is not supported");
            assertStatus(jpyDecimals, 400);
            assertThat(message(jpyDecimals)).isEqualTo("JPY amounts cannot have decimal places");
            assertThat(balanceOf(bob.account())).isEqualByComparingTo("20.00");
        }

        @Test
        @DisplayName("locked recipient → 400 'Selected account has been locked'")
        void locked() {
            Customer bob = newCustomer("rmt_bob");
            assertStatus(patch("/api/accounts/" + bob.account().id() + "/status?status=LOCKED", adminToken()), 200);

            ResponseEntity<String> response = remit(bob, "USD", "100.00", key());

            assertStatus(response, 400);
            assertThat(message(response)).isEqualTo("Selected account has been locked");
            assertThat(balanceOf(bob.account())).isEqualByComparingTo("20.00");
        }

        @Test
        @DisplayName("unknown account → 404 'To account not found'")
        void unknownAccount() {
            ResponseEntity<String> response = post(URL, adminToken(), body("0000000000", "USD", "100.00", key()));

            assertStatus(response, 404);
            assertThat(message(response)).isEqualTo("To account not found");
        }

        @Test
        @DisplayName("request validation: missing sender, bad currency format, zero amount, blank key → 400")
        void validation() {
            Customer bob = newCustomer("rmt_bob");
            String number = bob.account().number();

            Map<String, Object> noSender = body(number, "USD", "100.00", key());
            noSender.remove("senderName");
            ResponseEntity<String> missingSender = post(URL, adminToken(), noSender);
            assertStatus(missingSender, 400);
            assertThat(json(missingSender).get("fieldErrors").get("senderName").asText())
                    .isEqualTo("senderName is required");

            assertStatus(post(URL, adminToken(), body(number, "usd", "100.00", key())), 400);
            assertStatus(post(URL, adminToken(), body(number, "USD", "0", key())), 400);
            assertStatus(post(URL, adminToken(), body(number, "USD", "100.00", " ")), 400);
            assertThat(balanceOf(bob.account())).isEqualByComparingTo("20.00");
        }
    }

    @Nested
    @DisplayName("idempotency")
    class Idempotency {

        @Test
        @DisplayName("same key again → 201 with the original and the duplicate message; credited once")
        void duplicate() {
            Customer bob = newCustomer("rmt_bob");
            String key = key();
            String reference = text(remit(bob, "USD", "100.00", key), "transactionReference");

            ResponseEntity<String> retry = remit(bob, "USD", "100.00", key);

            assertStatus(retry, 201);
            assertThat(text(retry, "transactionReference")).isEqualTo(reference);
            assertThat(text(retry, "message")).isEqualTo("Duplicate request detected. Returning existing transaction.");
            assertThat(json(retry).get("amount").decimalValue()).isEqualByComparingTo("460.00");
            assertThat(balanceOf(bob.account())).isEqualByComparingTo("480.00");
        }

        @Test
        @DisplayName("same key, another amount or currency, or the key of a transfer → 409; nothing is credited")
        void mismatch() {
            Customer alice = newCustomer("rmt_alice");
            Customer bob = newCustomer("rmt_bob");
            topUp(alice.token(), alice.account(), "100.00");
            String key = key();
            assertStatus(remit(bob, "USD", "100.00", key), 201);
            String transferKey = key();
            assertStatus(transfer(alice.token(), alice.account(), bob.account(), "10.00", transferKey), 201);

            ResponseEntity<String> otherAmount = remit(bob, "USD", "101.00", key);
            ResponseEntity<String> otherCurrency = remit(bob, "SGD", "100.00", key);
            ResponseEntity<String> keyOfTransfer = remit(bob, "USD", "100.00", transferKey);

            assertStatus(otherAmount, 409);
            assertThat(message(otherAmount)).isEqualTo("idempotencyKey has already been used for a different transaction");
            assertStatus(otherCurrency, 409);
            assertStatus(keyOfTransfer, 409);
            assertThat(balanceOf(bob.account())).isEqualByComparingTo("490.00");   // 20 + 460 + 10
        }
    }
}
