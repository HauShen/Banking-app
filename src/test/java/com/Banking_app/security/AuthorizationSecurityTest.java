package com.Banking_app.security;

import com.Banking_app.support.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * What may you do? Logged-in users who are not allowed get 403 - the role checks
 * (URL rules and @PreAuthorize) and the ownership checks in the use cases.
 */
class AuthorizationSecurityTest extends AbstractIntegrationTest {

    @Test
    @DisplayName("Customer on an /api/admin/** URL -> 403 (not 401: the /error forward must keep the login)")
    void customer_onAdminUrl_gets403() {
        String customer = register(uniqueName("cust"));

        ResponseEntity<String> response = get("/api/admin/users", customer);

        assertStatus(response, 403);
        assertThat(text(response, "message")).isEqualTo("You do not have permission to perform this action");
    }

    @Test
    @DisplayName("Admin on an /api/admin/** URL -> 200")
    void admin_onAdminUrl_gets200() {
        assertStatus(get("/api/admin/users", adminToken()), 200);
    }

    @Test
    @DisplayName("Customer on an admin-only method (@PreAuthorize) -> 403")
    void customer_onAdminOnlyMethod_gets403() {
        String customer = register(uniqueName("cust"));

        ResponseEntity<String> response = post("/api/accounts/create", customer,
                Map.of("userId", userIdOf(customer), "accountType", "SAVINGS"));

        assertStatus(response, 403);
    }

    @Test
    @DisplayName("Creating a customer ignores a 'role' field: it always makes a CUSTOMER, never an admin")
    void createCustomer_ignoresRoleField() {
        String username = uniqueName("sneaky");
        ResponseEntity<String> created = post("/api/admin/users/customers", adminToken(), Map.of(
                "username", username,
                "fullName", "Sneaky User",
                "email", username + "@example.com",
                "password", PASSWORD,
                "role", "ADMIN"));
        assertStatus(created, 201);
        assertThat(text(created, "role")).isEqualTo("CUSTOMER");

        String token = login(username, PASSWORD);
        assertThat(text(get("/api/users/me", token), "role")).isEqualTo("CUSTOMER");
        assertStatus(get("/api/admin/users", token), 403);
    }

    @Test
    @DisplayName("No public sign-up: POST /api/auth/register -> 404, nobody can create their own account")
    void publicSignUp_isGone() {
        String username = uniqueName("walkin");

        ResponseEntity<String> response = post("/api/auth/register", null, Map.of(
                "username", username,
                "fullName", "Walk-in User",
                "email", username + "@example.com",
                "password", PASSWORD));

        assertStatus(response, 404);
    }

    @Test
    @DisplayName("Customers can't create customers: the old /api/users/create -> 404, the admin endpoint -> 403")
    void customer_cantCreateCustomers() {
        String customer = register(uniqueName("cust"));
        String username = uniqueName("friend");
        Map<String, String> body = Map.of(
                "username", username,
                "fullName", "A Friend",
                "email", username + "@example.com",
                "password", PASSWORD);

        assertStatus(post("/api/users/create", customer, body), 404);
        assertStatus(post("/api/admin/users/customers", customer, body), 403);
    }

    @Test
    @DisplayName("A customer can view their own account but not someone else's")
    void accountView_ownerOnly() {
        Customer alice = newCustomer("alice");
        Customer sally = newCustomer("sally");

        assertStatus(get("/api/accounts/" + alice.account().id(), alice.token()), 200);
        assertStatus(get("/api/accounts/" + alice.account().id(), sally.token()), 403);
        assertStatus(get("/api/accounts/number/" + alice.account().number(), sally.token()), 403);
    }

    @Test
    @DisplayName("A customer cannot top up someone else's account, and the balance stays the same")
    void topUp_ownerOnly() {
        Customer alice = newCustomer("alice");
        Customer sally = newCustomer("sally");
        BigDecimal before = balanceOf(alice.account());

        ResponseEntity<String> response =
                patch("/api/accounts/" + alice.account().id() + "/topup?amount=50", sally.token());

        assertStatus(response, 403);
        assertThat(balanceOf(alice.account())).isEqualByComparingTo(before);
    }

    @Test
    @DisplayName("An admin cannot top up, not even an account the admin owns -> 403, balance unchanged")
    void topUp_customersOnly_notAdmins() {
        String admin = adminToken();
        Account adminsOwnAccount = openAccount(admin);
        BigDecimal before = balanceOf(adminsOwnAccount);

        ResponseEntity<String> response =
                patch("/api/accounts/" + adminsOwnAccount.id() + "/topup?amount=50", admin);

        assertStatus(response, 403);
        assertThat(text(response, "message")).isEqualTo("You do not have permission to perform this action");
        assertThat(balanceOf(adminsOwnAccount)).isEqualByComparingTo(before);
    }
}
