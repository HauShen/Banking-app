package com.Banking_app.demo.adapter.in.web;

import com.Banking_app.demo.DemoCast;
import com.Banking_app.exception.ForbiddenException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.HandlerMapping;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Every rule of the demo guard (plan step C1), without Spring or a database: requests are built the way Spring MVC
 * leaves them before an interceptor runs (matched URL pattern + path variables + the logged-in user). The
 * account lookup is a mocked JdbcTemplate. DemoProfileStartupTest checks the same rules end to end.
 */
class DemoGuardInterceptorTest {

    private static final DemoCast CAST = DemoCast.standard(90);
    private static final String WEIMING_ID = CAST.user("weiming").id();
    private static final String DEMOADMIN_ID = CAST.user("demoadmin").id();
    private static final String VISITOR_ID = "11111111-2222-3333-4444-555555555555";
    private static final long SEEDED_ACCOUNT_ID = 1L;
    private static final long VISITOR_ACCOUNT_ID = 42L;

    private JdbcTemplate jdbc;
    private DemoGuardInterceptor guard;

    @BeforeEach
    void setUp() {
        jdbc = mock(JdbcTemplate.class);
        when(jdbc.queryForList(anyString(), eq(String.class), eq(SEEDED_ACCOUNT_ID)))
                .thenReturn(List.of(DemoCast.MAIN_SAVINGS));
        when(jdbc.queryForList(anyString(), eq(String.class), eq(VISITOR_ACCOUNT_ID)))
                .thenReturn(List.of("2000000001"));
        guard = new DemoGuardInterceptor(jdbc, CAST);
    }

    @ParameterizedTest(name = "{0} {1}")
    @CsvSource({
            "PUT,    /api/admin/users/{id}",
            "PATCH,  /api/admin/users/{id}/role",
            "DELETE, /api/admin/users/{id}",
            "POST,   /api/admin/users/{id}/revoke-tokens"})
    void anAdmin_cantChangeASeededUser(String method, String pattern) {
        assertRefused(request(method, pattern, WEIMING_ID, "demoadmin"), DemoGuardInterceptor.SHARED_USER);
        assertRefused(request(method, pattern, DEMOADMIN_ID, "demoadmin"), DemoGuardInterceptor.SHARED_USER);
    }

    @ParameterizedTest(name = "{0} {1}")
    @CsvSource({
            "PUT,    /api/admin/users/{id}",
            "PATCH,  /api/admin/users/{id}/role",
            "DELETE, /api/admin/users/{id}",
            "POST,   /api/admin/users/{id}/revoke-tokens"})
    void anAdmin_canChangeAUserAVisitorCreated(String method, String pattern) {
        assertAllowed(request(method, pattern, VISITOR_ID, "demoadmin"));
    }

    @Test
    void anAdmin_canStillViewSeededUsers() {
        assertAllowed(request("GET", "/api/admin/users/{id}", WEIMING_ID, "demoadmin"));
    }

    @ParameterizedTest(name = "{0} {1}")
    @CsvSource({"PUT, /api/users/me", "POST, /api/auth/change-password"})
    void aSeededUser_cantChangeTheirOwnProfileOrPassword(String method, String pattern) {
        assertRefused(request(method, pattern, null, "weiming"), DemoGuardInterceptor.SHARED_LOGIN);
        assertRefused(request(method, pattern, null, "demoadmin"), DemoGuardInterceptor.SHARED_LOGIN);
    }

    @ParameterizedTest(name = "{0} {1}")
    @CsvSource({"PUT, /api/users/me", "POST, /api/auth/change-password"})
    void aCustomerAVisitorCreated_canChangeTheirOwnProfileAndPassword(String method, String pattern) {
        assertAllowed(request(method, pattern, null, "visitor1"));
    }

    @ParameterizedTest(name = "{0} {1}")
    @CsvSource({"PATCH, /api/accounts/{id}/status", "DELETE, /api/accounts/{id}"})
    void aSeededAccount_cantBeLockedUnlockedOrDeleted(String method, String pattern) {
        assertRefused(request(method, pattern, String.valueOf(SEEDED_ACCOUNT_ID), "demoadmin"),
                DemoGuardInterceptor.SHARED_ACCOUNT);
    }

    @ParameterizedTest(name = "{0} {1}")
    @CsvSource({"PATCH, /api/accounts/{id}/status", "DELETE, /api/accounts/{id}"})
    void anAccountAVisitorOpened_canBeLockedAndDeleted(String method, String pattern) {
        assertAllowed(request(method, pattern, String.valueOf(VISITOR_ACCOUNT_ID), "demoadmin"));
    }

    @Test
    void anUnknownOrMalformedAccountId_isLeftToTheController() {
        when(jdbc.queryForList(anyString(), eq(String.class), eq(999L))).thenReturn(List.of());

        assertAllowed(request("DELETE", "/api/accounts/{id}", "999", "demoadmin"));      // → 404 there
        assertAllowed(request("DELETE", "/api/accounts/{id}", "not-a-number", "demoadmin"));   // → 400 there
    }

    @Test
    void topUpsAndTransfers_onSeededAccounts_areAllowed() {
        assertAllowed(request("PATCH", "/api/accounts/{id}/topup", String.valueOf(SEEDED_ACCOUNT_ID), "weiming"));
        assertAllowed(request("POST", "/api/transfers", null, "weiming"));
    }

    @Test
    void creatingCustomers_isAllowed() {
        assertAllowed(request("POST", "/api/admin/users/customers", null, "demoadmin"));
    }

    @Test
    void aRequestWithoutAMatchedPattern_isIgnored_withoutTouchingTheDatabase() {
        MockHttpServletRequest request = new MockHttpServletRequest("DELETE", "/api/accounts/1");

        assertAllowed(request);
        verifyNoInteractions(jdbc);
    }

    @Test
    void theRefusal_isA403ForTheGlobalExceptionHandler() {
        assertThatThrownBy(() -> guard.preHandle(request("DELETE", "/api/admin/users/{id}", WEIMING_ID, "demoadmin"),
                new MockHttpServletResponse(), new Object()))
                .isInstanceOf(ForbiddenException.class);
    }

    /** A request as Spring MVC leaves it for interceptors; {@code id} null = no path variable. */
    private static MockHttpServletRequest request(String method, String pattern, String id, String username) {
        String uri = pattern.replace("{id}", String.valueOf(id));
        MockHttpServletRequest request = new MockHttpServletRequest(method, uri);
        request.setAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE, pattern);
        request.setAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE, id == null ? Map.of() : Map.of("id", id));
        request.setUserPrincipal(() -> username);
        return request;
    }

    private void assertRefused(MockHttpServletRequest request, String message) {
        assertThatThrownBy(() -> guard.preHandle(request, new MockHttpServletResponse(), new Object()))
                .as(request.getMethod() + " " + request.getRequestURI() + " as " + request.getUserPrincipal().getName())
                .isInstanceOf(DemoDataProtectedException.class)
                .hasMessage(message);
    }

    private void assertAllowed(MockHttpServletRequest request) {
        try {
            assertThat(guard.preHandle(request, new MockHttpServletResponse(), new Object())).isTrue();
        } catch (Exception e) {
            throw new AssertionError(request.getMethod() + " " + request.getRequestURI() + " was refused", e);
        }
    }
}
