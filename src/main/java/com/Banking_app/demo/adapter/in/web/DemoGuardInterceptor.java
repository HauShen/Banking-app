package com.Banking_app.demo.adapter.in.web;

import com.Banking_app.demo.DemoAccount;
import com.Banking_app.demo.DemoCast;
import com.Banking_app.demo.DemoUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;

import java.security.Principal;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The live demo's guard (plan step C1): the seeded users and accounts are shared by every visitor and the demoadmin
 * password is public, so the few requests that would break the tour for everyone else are refused with 403. Same
 * idea as Snipe-IT's public demo. Everything that shows the app working stays allowed (transfers, top-ups,
 * remittances, opening accounts, creating customers), and anything a visitor creates can be changed freely; the
 * nightly reset cleans it all up.
 *
 * <p>Refused:
 * <ul>
 *   <li>an admin editing, demoting, deleting or signing out a seeded user (the id in the URL is a seeded user's:
 *       {@link DemoUser#id()} is the same after every reset, so no lookup is needed);</li>
 *   <li>a seeded user changing their own profile or password (it's a shared login);</li>
 *   <li>locking, unlocking or deleting a seeded account (the id in the URL is a database id, which changes after
 *       every reset, so it is looked up by account number).</li>
 * </ul>
 *
 * <p>Runs after Spring Security, so the user is known; matching uses the controller's URL pattern and path variables
 * as Spring MVC matched them, so it needs nothing from the modules or the security package. Only registered with
 * the {@code demo} profile ({@link DemoGuardConfig}).
 */
public class DemoGuardInterceptor implements HandlerInterceptor {

    static final String SHARED_USER = "This is a shared demo user, so it can't be changed. "
            + "Log in as demoadmin and create your own customer to try this.";
    static final String SHARED_LOGIN = "This is a shared demo login, so its profile and password can't be changed. "
            + "Log in as demoadmin and create your own customer to try this.";
    static final String SHARED_ACCOUNT = "This is a shared demo account, so it can't be locked, unlocked or deleted. "
            + "Open an account for a customer you created to try this.";

    /** "METHOD pattern" as Spring MVC matched it; {id} is a user id. */
    private static final Set<String> SEEDED_USER_ROUTES = Set.of(
            "PUT /api/admin/users/{id}",
            "PATCH /api/admin/users/{id}/role",
            "DELETE /api/admin/users/{id}",
            "POST /api/admin/users/{id}/revoke-tokens");
    /** Changes the logged-in user makes to themselves. */
    private static final Set<String> OWN_LOGIN_ROUTES = Set.of(
            "PUT /api/users/me",
            "POST /api/auth/change-password");
    /** {id} is an account's database id. Top-ups are allowed: that's how demoadmin refills weiming's account. */
    private static final Set<String> SEEDED_ACCOUNT_ROUTES = Set.of(
            "PATCH /api/accounts/{id}/status",
            "DELETE /api/accounts/{id}");

    private final JdbcTemplate jdbc;
    private final Set<String> userIds;
    private final Set<String> usernames;
    private final Set<String> accountNumbers;

    public DemoGuardInterceptor(JdbcTemplate jdbc, DemoCast cast) {
        this.jdbc = jdbc;
        this.userIds = cast.users().stream().map(DemoUser::id).collect(Collectors.toUnmodifiableSet());
        this.usernames = cast.users().stream().map(DemoUser::username).collect(Collectors.toUnmodifiableSet());
        this.accountNumbers = cast.accounts().stream().map(DemoAccount::accountNumber)
                .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        Object pattern = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        if (pattern == null) {
            return true;   // not a controller request (static resource, error page)
        }
        String route = request.getMethod() + " " + pattern;

        if (SEEDED_USER_ROUTES.contains(route) && userIds.contains(pathVariable(request, "id"))) {
            throw new DemoDataProtectedException(SHARED_USER);
        }
        if (OWN_LOGIN_ROUTES.contains(route) && isSeededLogin(request.getUserPrincipal())) {
            throw new DemoDataProtectedException(SHARED_LOGIN);
        }
        if (SEEDED_ACCOUNT_ROUTES.contains(route) && isSeededAccount(pathVariable(request, "id"))) {
            throw new DemoDataProtectedException(SHARED_ACCOUNT);
        }
        return true;
    }

    private boolean isSeededLogin(Principal principal) {
        return principal != null && usernames.contains(principal.getName());   // getName() = the username
    }

    /** False for an id that isn't a number or isn't in the table: the controller then answers 400 / 404 as usual. */
    private boolean isSeededAccount(String id) {
        long accountId;
        try {
            accountId = Long.parseLong(id);
        } catch (NumberFormatException e) {
            return false;
        }
        return jdbc.queryForList("SELECT account_number FROM accounts WHERE id = ?", String.class, accountId)
                .stream().anyMatch(accountNumbers::contains);
    }

    private static String pathVariable(HttpServletRequest request, String name) {
        Object variables = request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
        return variables instanceof Map<?, ?> map && map.get(name) instanceof String value ? value : "";
    }
}
