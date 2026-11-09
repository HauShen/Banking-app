package com.Banking_app.transaction.adapter.in.web;

import com.Banking_app.support.AbstractIntegrationTest;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * POST /api/transfers with an amount in a foreign currency, over real HTTP, with the round test rates from
 * src/test/resources/application.properties (sell: USD 4.75, SGD 3.50, JPY 0.0305). The MYR equivalent moves between
 * the two MYR accounts; the reply and the history show the original currency, amount and rate.
 */
class ForeignCurrencyTransferTest extends AbstractIntegrationTest {

    @Autowired
    private JdbcTemplate jdbc;

    private static String key() {
        return "fx-transfer-" + UUID.randomUUID();
    }

    private static String message(ResponseEntity<String> response) {
        return json(response).get("message").asText();
    }

    /** A transfer with the amount in {@code currency}; {@code currency == null} leaves the field out. */
    private ResponseEntity<String> transferIn(Customer from, Customer to, String amount, String currency, String key) {
        Map<String, Object> body = new HashMap<>(Map.of(
                "fromAccountNumber", from.account().number(),
                "toAccountNumber", to.account().number(),
                "amount", new BigDecimal(amount),
                "description", "foreign currency test",
                "idempotencyKey", key));
        if (currency != null) {
            body.put("currency", currency);
        }
        return post("/api/transfers", from.token(), body);
    }

    /** Alice with RM 120.00 (20.00 opening + 100.00 top-up) and Bob with RM 20.00. */
    private Customer[] aliceAndBob() {
        Customer alice = newCustomer("fx_alice");
        Customer bob = newCustomer("fx_bob");
        topUp(alice.token(), alice.account(), "100.00");
        return new Customer[]{alice, bob};
    }

    private static void assertNoOriginalAmount(JsonNode body) {
        assertThat(body.hasNonNull("originalCurrency")).isFalse();
        assertThat(body.hasNonNull("originalAmount")).isFalse();
        assertThat(body.hasNonNull("exchangeRate")).isFalse();
    }

    @Nested
    @DisplayName("paying in a foreign currency")
    class Paying {

        @Test
        @DisplayName("20.00 USD → 201, RM 95.00 moves at the sell rate 4.75; the reply shows amount, currency and rate")
        void usd() {
            Customer[] p = aliceAndBob();

            ResponseEntity<String> response = transferIn(p[0], p[1], "20.00", "USD", key());

            assertStatus(response, 201);
            JsonNode body = json(response);
            assertThat(body.get("message").asText()).isEqualTo("Transfer successful");
            assertThat(body.get("amount").decimalValue()).isEqualByComparingTo("95.00");
            assertThat(body.get("originalCurrency").asText()).isEqualTo("USD");
            assertThat(body.get("originalAmount").decimalValue()).isEqualByComparingTo("20.00");
            assertThat(body.get("exchangeRate").decimalValue()).isEqualByComparingTo("4.75");
            assertThat(balanceOf(p[0].account())).isEqualByComparingTo("25.00");
            assertThat(balanceOf(p[1].account())).isEqualByComparingTo("115.00");
        }

        @Test
        @DisplayName("the transaction and the recipient's history show the MYR amount plus the original amount and rate")
        void history() {
            Customer[] p = aliceAndBob();
            String reference = text(transferIn(p[0], p[1], "20.00", "USD", key()), "transactionReference");

            JsonNode tx = json(get("/api/transfers/" + reference, p[0].token()));
            assertThat(tx.get("type").asText()).isEqualTo("TRANSFER");
            assertThat(tx.get("amount").decimalValue()).isEqualByComparingTo("95.00");
            assertThat(tx.get("currency").asText()).isEqualTo("MYR");
            assertThat(tx.get("originalCurrency").asText()).isEqualTo("USD");
            assertThat(tx.get("originalAmount").decimalValue()).isEqualByComparingTo("20.00");
            assertThat(tx.get("exchangeRate").decimalValue()).isEqualByComparingTo("4.75");

            JsonNode bobsPage = json(get("/api/transfers/account/" + p[1].account().number(), p[1].token()));
            JsonNode row = null;
            for (JsonNode candidate : bobsPage.get("content")) {
                if (candidate.get("reference").asText().equals(reference)) {
                    row = candidate;
                }
            }
            assertThat(row).as("the transfer in Bob's history").isNotNull();
            assertThat(row.get("originalCurrency").asText()).isEqualTo("USD");
            assertThat(row.get("amount").decimalValue()).isEqualByComparingTo("95.00");
        }

        @Test
        @DisplayName("10000 JPY → RM 305.00; the history shows 10000, not 10000.00 (JPY has no decimals)")
        void jpy() {
            Customer alice = newCustomer("fx_alice");
            Customer bob = newCustomer("fx_bob");
            topUp(alice.token(), alice.account(), "400.00");

            ResponseEntity<String> response = transferIn(alice, bob, "10000", "JPY", key());

            assertStatus(response, 201);
            assertThat(json(response).get("amount").decimalValue()).isEqualByComparingTo("305.00");
            JsonNode tx = json(get("/api/transfers/" + text(response, "transactionReference"), alice.token()));
            assertThat(tx.get("originalAmount").asText()).isEqualTo("10000");
            assertThat(tx.get("exchangeRate").decimalValue()).isEqualByComparingTo("0.0305");
            assertThat(balanceOf(bob.account())).isEqualByComparingTo("325.00");
        }

        @Test
        @DisplayName("the minimum balance applies to the converted amount: 1.00 USD = RM 4.75 from RM 20.00 → 400")
        void minimumBalance() {
            Customer alice = newCustomer("fx_alice");
            Customer bob = newCustomer("fx_bob");

            ResponseEntity<String> response = transferIn(alice, bob, "1.00", "USD", key());

            assertStatus(response, 400);
            assertThat(message(response)).isEqualTo("Insufficient balance. Minimum balance of $20.00 must be maintained.");
            assertThat(balanceOf(alice.account())).isEqualByComparingTo("20.00");
        }
    }

    @Nested
    @DisplayName("MYR transfers are unchanged")
    class Myr {

        @Test
        @DisplayName("currency MYR → the amount moves as is; type TRANSFER, no original amount in reply or history")
        void myr() {
            Customer[] p = aliceAndBob();

            ResponseEntity<String> response = transferIn(p[0], p[1], "30.00", "MYR", key());

            assertStatus(response, 201);
            assertThat(json(response).get("amount").decimalValue()).isEqualByComparingTo("30.00");
            assertNoOriginalAmount(json(response));
            JsonNode tx = json(get("/api/transfers/" + text(response, "transactionReference"), p[0].token()));
            assertThat(tx.get("type").asText()).isEqualTo("TRANSFER");
            assertNoOriginalAmount(tx);
            assertThat(balanceOf(p[1].account())).isEqualByComparingTo("50.00");
        }

        @Test
        @DisplayName("no currency field → MYR")
        void noCurrency() {
            Customer[] p = aliceAndBob();

            ResponseEntity<String> response = transferIn(p[0], p[1], "30.00", null, key());

            assertStatus(response, 201);
            assertNoOriginalAmount(json(response));
            assertThat(balanceOf(p[0].account())).isEqualByComparingTo("90.00");
        }
    }

    @Nested
    @DisplayName("refused (400, nothing moves)")
    class Refused {

        @Test
        @DisplayName("JPY with decimals → 'JPY amounts cannot have decimal places'")
        void jpyDecimals() {
            Customer[] p = aliceAndBob();

            ResponseEntity<String> response = transferIn(p[0], p[1], "1000.50", "JPY", key());

            assertStatus(response, 400);
            assertThat(message(response)).isEqualTo("JPY amounts cannot have decimal places");
            assertThat(balanceOf(p[0].account())).isEqualByComparingTo("120.00");
        }

        @Test
        @DisplayName("a currency the bank doesn't offer → 'Currency XYZ is not supported'")
        void unsupported() {
            Customer[] p = aliceAndBob();

            ResponseEntity<String> response = transferIn(p[0], p[1], "10.00", "XYZ", key());

            assertStatus(response, 400);
            assertThat(message(response)).isEqualTo("Currency XYZ is not supported");
            assertThat(balanceOf(p[0].account())).isEqualByComparingTo("120.00");
        }

        @Test
        @DisplayName("a lower-case code fails the request validation (3 capital letters)")
        void lowerCase() {
            Customer[] p = aliceAndBob();

            assertStatus(transferIn(p[0], p[1], "10.00", "usd", key()), 400);
            assertThat(balanceOf(p[0].account())).isEqualByComparingTo("120.00");
        }
    }

    @Nested
    @DisplayName("idempotency compares the original currency and amount")
    class Idempotency {

        @Test
        @DisplayName("same key again → 201 duplicate with the original MYR amount and rate")
        void duplicate() {
            Customer[] p = aliceAndBob();
            String key = key();
            String reference = text(transferIn(p[0], p[1], "20.00", "USD", key), "transactionReference");

            ResponseEntity<String> retry = transferIn(p[0], p[1], "20.00", "USD", key);

            assertStatus(retry, 201);
            assertThat(text(retry, "transactionReference")).isEqualTo(reference);
            assertThat(text(retry, "message")).isEqualTo("Duplicate request detected. Returning existing transaction.");
            assertThat(json(retry).get("amount").decimalValue()).isEqualByComparingTo("95.00");
            assertThat(json(retry).get("exchangeRate").decimalValue()).isEqualByComparingTo("4.75");
            assertThat(balanceOf(p[0].account())).isEqualByComparingTo("25.00");
        }

        @Test
        @DisplayName("booked at an older rate (4.70), retried now (4.75) → still the duplicate, with the booked values")
        void retryAfterRateChange() {
            Customer[] p = aliceAndBob();
            String key = key();
            String reference = text(transferIn(p[0], p[1], "20.00", "USD", key), "transactionReference");
            // As if the transfer had been booked yesterday, at yesterday's rate.
            jdbc.update("UPDATE transactions SET exchange_rate = 4.70, amount = 94.00 WHERE reference_number = ?",
                    reference);

            ResponseEntity<String> retry = transferIn(p[0], p[1], "20.00", "USD", key);

            assertStatus(retry, 201);
            assertThat(text(retry, "transactionReference")).isEqualTo(reference);
            assertThat(json(retry).get("amount").decimalValue()).isEqualByComparingTo("94.00");
            assertThat(json(retry).get("exchangeRate").decimalValue()).isEqualByComparingTo("4.70");
            assertThat(balanceOf(p[0].account())).isEqualByComparingTo("25.00");   // nothing moved again
        }

        @Test
        @DisplayName("same key, a different original amount or a different currency → 409, nothing moves")
        void mismatch() {
            Customer[] p = aliceAndBob();
            String key = key();
            assertStatus(transferIn(p[0], p[1], "20.00", "USD", key), 201);

            ResponseEntity<String> otherAmount = transferIn(p[0], p[1], "21.00", "USD", key);
            ResponseEntity<String> otherCurrency = transferIn(p[0], p[1], "20.00", "SGD", key);
            ResponseEntity<String> sameMyrAmount = transferIn(p[0], p[1], "95.00", "MYR", key);

            assertStatus(otherAmount, 409);
            assertThat(message(otherAmount))
                    .isEqualTo("idempotencyKey has already been used for a transfer with a different amount or recipient");
            assertStatus(otherCurrency, 409);
            assertStatus(sameMyrAmount, 409);
            assertThat(balanceOf(p[0].account())).isEqualByComparingTo("25.00");
        }
    }
}
