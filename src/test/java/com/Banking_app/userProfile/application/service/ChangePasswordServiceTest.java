package com.Banking_app.userProfile.application.service;

import com.Banking_app.shared.PageResult;
import com.Banking_app.userProfile.application.exception.IncorrectCurrentPasswordException;
import com.Banking_app.userProfile.application.exception.PasswordChangeRejectedException;
import com.Banking_app.userProfile.application.port.in.command.ChangePasswordCommand;
import com.Banking_app.userProfile.application.port.in.query.ListUserProfilesQuery;
import com.Banking_app.userProfile.application.port.out.AuditAction;
import com.Banking_app.userProfile.application.port.out.AuditPort;
import com.Banking_app.userProfile.application.port.out.LoadUserProfilePort;
import com.Banking_app.userProfile.application.port.out.PasswordHasherPort;
import com.Banking_app.userProfile.application.port.out.SaveUserProfilePort;
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

class ChangePasswordServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-20T14:00:00Z");

    /** "Hashes" by prefixing, so the tests can read what was stored. */
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
        public boolean existsByUsername(String username) { throw new UnsupportedOperationException(); }
        public boolean existsByEmail(String email) { throw new UnsupportedOperationException(); }
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
            audit.add(action + " " + actorId + " " + targetUserId + " " + at);

    private final ChangePasswordService service = new ChangePasswordService(load, save, HASHER, auditPort,
            Clock.fixed(NOW, ZoneOffset.UTC));

    ChangePasswordServiceTest() {
        users.put("u1", UserProfile.register("u1", "alice", "Alice Tan", "alice@example.com", "hash:OldPassw0rd",
                Instant.parse("2026-11-01T00:00:00Z")));
    }

    @Test
    @DisplayName("right current password: the new hash is stored and the change is audited")
    void changesPassword() {
        UserProfile changed = service.changePassword(new ChangePasswordCommand("u1", "OldPassw0rd", "NewPassw0rd"));

        assertThat(changed.getPasswordHash()).isEqualTo("hash:NewPassw0rd");
        assertThat(users.get("u1").getPasswordHash()).isEqualTo("hash:NewPassw0rd");
        assertThat(audit).containsExactly(AuditAction.PASSWORD_CHANGED + " u1 u1 " + NOW);
    }

    @Test
    @DisplayName("wrong current password: rejected on currentPassword, nothing stored or audited")
    void wrongCurrentPassword() {
        assertThatThrownBy(() -> service.changePassword(new ChangePasswordCommand("u1", "Guess1234", "NewPassw0rd")))
                .isInstanceOf(IncorrectCurrentPasswordException.class)
                .hasMessage("Current password is incorrect")
                .satisfies(e -> assertThat(((PasswordChangeRejectedException) e).getField())
                        .isEqualTo("currentPassword"));
        assertThat(users.get("u1").getPasswordHash()).isEqualTo("hash:OldPassw0rd");
        assertThat(audit).isEmpty();
    }

    @Test
    @DisplayName("new password equal to the current one: rejected on newPassword")
    void unchangedPassword() {
        assertThatThrownBy(() -> service.changePassword(new ChangePasswordCommand("u1", "OldPassw0rd", "OldPassw0rd")))
                .isInstanceOf(PasswordChangeRejectedException.class)
                .isNotInstanceOf(IncorrectCurrentPasswordException.class)
                .hasMessage("New password must differ from the current one")
                .satisfies(e -> assertThat(((PasswordChangeRejectedException) e).getField())
                        .isEqualTo("newPassword"));
        assertThat(audit).isEmpty();
    }

    @Test
    @DisplayName("the command refuses a new password outside 8-100 characters and never prints passwords")
    void commandRules() {
        assertThatThrownBy(() -> new ChangePasswordCommand("u1", "OldPassw0rd", "short"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ChangePasswordCommand("u1", "OldPassw0rd", "x".repeat(101)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(new ChangePasswordCommand("u1", "OldPassw0rd", "NewPassw0rd").toString())
                .doesNotContain("OldPassw0rd", "NewPassw0rd");
    }
}
