package com.Banking_app.fx.adapter.in.web;

import com.Banking_app.support.AbstractIntegrationTest;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * /api/fx over real HTTP, with the round test rates from src/test/resources/application.properties
 * (USD buy 4.60 / sell 4.75, SGD 3.40 / 3.50, EUR 4.90 / 5.10, JPY 0.0290 / 0.0305). This also proves the
 * fx.rates.* properties bind as intended.
 */
class FxEndpointTest extends AbstractIntegrationTest {

    private String customerToken() {
        return register(uniqueName("fx_user"));
    }

    private static String message(ResponseEntity<String> response) {
        return json(response).get("message").asText();
    }

    @Nested
    @DisplayName("GET /api/fx/rates")
    class Rates {

        @Test
        @DisplayName("a customer gets every offered currency, sorted by code, with buy and sell rate and asOf")
        void listsRates() {
            ResponseEntity<String> response = get("/api/fx/rates", customerToken());

            assertStatus(response, 200);
            JsonNode body = json(response);
            List<String> currencies = new ArrayList<>();
            body.forEach(rate -> currencies.add(rate.get("currency").asText()));
            assertThat(currencies).containsExactly("EUR", "JPY", "SGD", "USD");

            JsonNode usd = body.get(3);
            assertThat(usd.get("buyRate").decimalValue()).isEqualByComparingTo("4.60");
            assertThat(usd.get("sellRate").decimalValue()).isEqualByComparingTo("4.75");
            assertThat(Instant.parse(usd.get("asOf").asText())).isBeforeOrEqualTo(Instant.now());
            assertThat(body.get(1).get("sellRate").decimalValue()).isEqualByComparingTo("0.0305");
        }

        @Test
        @DisplayName("without a token → 401")
        void needsLogin() {
            assertStatus(get("/api/fx/rates", null), 401);
        }
    }

    @Nested
    @DisplayName("GET /api/fx/quote")
    class QuoteEndpoint {

        @Test
        @DisplayName("PAY 100.00 USD → 200, sell rate 4.75, RM 475.00; the request is echoed")
        void pay() {
            ResponseEntity<String> response =
                    get("/api/fx/quote?currency=USD&amount=100.00&direction=PAY", customerToken());

            assertStatus(response, 200);
            JsonNode body = json(response);
            assertThat(body.get("currency").asText()).isEqualTo("USD");
            assertThat(body.get("amount").decimalValue()).isEqualByComparingTo("100.00");
            assertThat(body.get("direction").asText()).isEqualTo("PAY");
            assertThat(body.get("exchangeRate").decimalValue()).isEqualByComparingTo("4.75");
            assertThat(body.get("myrAmount").decimalValue()).isEqualByComparingTo("475.00");
            assertThat(body.hasNonNull("asOf")).isTrue();
        }

        @Test
        @DisplayName("RECEIVE 100.00 USD → buy rate 4.60, RM 460.00")
        void receive() {
            ResponseEntity<String> response =
                    get("/api/fx/quote?currency=USD&amount=100.00&direction=RECEIVE", customerToken());

            assertStatus(response, 200);
            assertThat(json(response).get("exchangeRate").decimalValue()).isEqualByComparingTo("4.60");
            assertThat(json(response).get("myrAmount").decimalValue()).isEqualByComparingTo("460.00");
        }

        @Test
        @DisplayName("no direction → PAY; 10000 JPY → RM 305.00")
        void defaultDirectionAndJpy() {
            ResponseEntity<String> response = get("/api/fx/quote?currency=JPY&amount=10000", customerToken());

            assertStatus(response, 200);
            assertThat(text(response, "direction")).isEqualTo("PAY");
            assertThat(json(response).get("myrAmount").decimalValue()).isEqualByComparingTo("305.00");
        }

        @Test
        @DisplayName("unsupported currency → 400 'Currency XYZ is not supported'; MYR → 400 'MYR needs no conversion'")
        void unsupportedCurrency() {
            String token = customerToken();

            ResponseEntity<String> unknown = get("/api/fx/quote?currency=XYZ&amount=10.00", token);
            assertStatus(unknown, 400);
            assertThat(message(unknown)).isEqualTo("Currency XYZ is not supported");

            ResponseEntity<String> myr = get("/api/fx/quote?currency=MYR&amount=10.00", token);
            assertStatus(myr, 400);
            assertThat(message(myr)).isEqualTo("MYR needs no conversion");
        }

        @Test
        @DisplayName("JPY with decimals, USD with 3 decimals, zero → 400 with the domain's message")
        void badAmounts() {
            String token = customerToken();

            ResponseEntity<String> jpy = get("/api/fx/quote?currency=JPY&amount=1000.5", token);
            assertStatus(jpy, 400);
            assertThat(message(jpy)).isEqualTo("JPY amounts cannot have decimal places");

            ResponseEntity<String> usd = get("/api/fx/quote?currency=USD&amount=10.500", token);
            assertStatus(usd, 400);
            assertThat(message(usd)).isEqualTo("USD amounts can have at most 2 decimal places");

            ResponseEntity<String> zero = get("/api/fx/quote?currency=USD&amount=0", token);
            assertStatus(zero, 400);
            assertThat(message(zero)).isEqualTo("Amount must be greater than 0");
        }

        @Test
        @DisplayName("missing or malformed parameters → 400 with a clear message, never 500")
        void badParameters() {
            String token = customerToken();

            ResponseEntity<String> noCurrency = get("/api/fx/quote?amount=10.00", token);
            assertStatus(noCurrency, 400);
            assertThat(message(noCurrency)).isEqualTo("currency is required");

            ResponseEntity<String> noAmount = get("/api/fx/quote?currency=USD", token);
            assertStatus(noAmount, 400);
            assertThat(message(noAmount)).isEqualTo("amount is required");

            ResponseEntity<String> notANumber = get("/api/fx/quote?currency=USD&amount=abc", token);
            assertStatus(notANumber, 400);
            assertThat(message(notANumber)).isEqualTo("amount must be a number");

            ResponseEntity<String> huge = get("/api/fx/quote?currency=USD&amount=1e999999999", token);
            assertStatus(huge, 400);
            assertThat(message(huge)).isEqualTo("amount can have at most 17 digits before the decimal point");

            ResponseEntity<String> badDirection = get("/api/fx/quote?currency=USD&amount=1&direction=SELL", token);
            assertStatus(badDirection, 400);
            assertThat(message(badDirection)).isEqualTo("direction must be PAY or RECEIVE");
        }

        @Test
        @DisplayName("without a token → 401")
        void needsLogin() {
            assertStatus(get("/api/fx/quote?currency=USD&amount=1", null), 401);
        }
    }
}
