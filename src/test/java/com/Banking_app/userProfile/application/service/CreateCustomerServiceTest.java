package com.Banking_app.userProfile.application.service;

import com.Banking_app.shared.PageResult;
import com.Banking_app.userProfile.application.exception.DuplicateUserException;
import com.Banking_app.userProfile.application.port.in.command.CreateCustomerCommand;
import com.Banking_app.userProfile.application.port.in.command.RegisterUserProfileCommand;
import com.Banking_app.userProfile.application.port.in.query.ListUserProfilesQuery;
import com.Banking_app.userProfile.application.port.out.AuditPort;
import com.Banking_app.userProfile.application.port.out.LoadUserProfilePort;
import com.Banking_app.userProfile.application.port.out.PasswordHasherPort;
import com.Banking_app.userProfile.application.port.out.SaveUserProfilePort;
import com.Banking_app.userProfile.domain.enums.UserRole;
import com.Banking_app.userProfile.domain.models.userprofile.UserProfile;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** CreateCustomerService with in-memory ports (same style as ChangePasswordServiceTest). */
class CreateCustomerServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-27T01:00:00Z");

    /** "Hashes" by prefixing, so the test can see that the hash, not the raw password, is stored. */
    private static final PasswordHasherPort HASHER = new PasswordHasherPort() {
        @Override
        public String hash(String rawPassword) {
            return "hash:" + rawPassword;
        }

        @Override
        public boolean matches(String rawPassword, String passwordHash) {
            return passwordHash.equals("hash:" + rawPassword);
        }
    };

    private final Map<String, UserProfile> users = new HashMap<>();
    private final List<String> audit = new ArrayList<>();

    private final LoadUserProfilePort load = new LoadUserProfilePort() {
        public Optional<UserProfile> findById(String id) { return Optional.ofNullable(users.get(id)); }
        public Optional<UserProfile> findByUsername(String username) { throw new UnsupportedOperationException(); }
        public boolean existsByUsername(String username) {
            return users.values().stream().anyMatch(u -> u.getUsername().equals(username));
        }
        public boolean existsByEmail(String email) {
            return users.values().stream().anyMatch(u -> u.getEmail().equals(email));
        }
        public Optional<UserProfile> findByEmail(String email) { throw new UnsupportedOperationException(); }
        public boolean existsAnyAdmin() { throw new UnsupportedOperationException(); }
        public long countAdmins() { throw new UnsupportedOperationException(); }
        public PageResult<UserProfile> findAll(ListUserProfilesQuery query) { throw new UnsupportedOperationException(); }
    };

    private final SaveUserProfilePort save = new SaveUserProfilePort() {
        public UserProfile save(UserProfile userProfile) { users.put(userProfile.getId(), userProfile); return userProfile; }
        public void deleteById(String id) { throw new UnsupportedOperationException(); }
    };

    private final AuditPort auditPort = (action, actorId, targetUserId, detail, at) ->
            audit.add(action + " " + actorId + " " + targetUserId + " " + detail + " " + at);

    private final CreateCustomerService service = new CreateCustomerService(load, save, HASHER, auditPort,
            Clock.fixed(NOW, ZoneOffset.UTC));

    @Test
    @DisplayName("creates a CUSTOMER with the hashed password and audits it with the admin as actor")
    void createsCustomer() {
        UserProfile created = service.createCustomer(command("alice", "alice@example.com"));

        assertThat(created.getRole()).isEqualTo(UserRole.CUSTOMER);
        assertThat(created.getUsername()).isEqualTo("alice");
        assertThat(created.getEmail()).isEqualTo("alice@example.com");
        assertThat(users).containsKey(created.getId());
        assertThat(HASHER.matches("Initial123!", users.get(created.getId()).getPasswordHash())).isTrue();
        assertThat(audit).containsExactly(
                "CUSTOMER_CREATED admin-1 " + created.getId() + " created CUSTOMER alice " + NOW);
    }

    @Test
    @DisplayName("a taken username is rejected, nothing is saved or audited")
    void duplicateUsername() {
        service.createCustomer(command("alice", "alice@example.com"));

        assertThatThrownBy(() -> service.createCustomer(command("alice", "other@example.com")))
                .isInstanceOf(DuplicateUserException.class).hasMessage("Username already exists");
        assertThat(users).hasSize(1);
        assertThat(audit).hasSize(1);
    }

    @Test
    @DisplayName("a taken email is rejected (case doesn't matter: the command lowercases it)")
    void duplicateEmail() {
        service.createCustomer(command("alice", "alice@example.com"));

        assertThatThrownBy(() -> service.createCustomer(command("alice2", "ALICE@example.com")))
                .isInstanceOf(DuplicateUserException.class).hasMessage("Email already exists");
        assertThat(users).hasSize(1);
    }

    private static CreateCustomerCommand command(String username, String email) {
        return new CreateCustomerCommand(
                new RegisterUserProfileCommand(username, "Test " + username, email, "Initial123!"), "admin-1");
    }
}
