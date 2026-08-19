package com.Banking_app.userProfile.domain.models.userprofile;

import com.Banking_app.userProfile.domain.enums.UserRole;

import java.time.Instant;
import java.util.Objects;
import java.util.regex.Pattern;


public final class UserProfile {

    private static final int USERNAME_MIN_LENGTH = 3;
    private static final int USERNAME_MAX_LENGTH = 30;
    private static final int FULL_NAME_MAX_LENGTH = 100;

    /** Deliberately permissive: the domain rejects nonsense, the edge does strict validation. */
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    private final String id;
    private final String username;
    private final UserRole role;
    private final String fullName;
    private final String email;
    /** Already-hashed password. The domain never sees or produces a raw password. */
    private final String passwordHash;
    private final Instant createdAt;
    private final Instant updatedAt;

    private UserProfile(String id,
                        String username,
                        UserRole role,
                        String fullName,
                        String email,
                        String passwordHash,
                        Instant createdAt,
                        Instant updatedAt) {
        this.id = requireText(id, "id");
        this.username = normaliseUsername(username);
        this.role = Objects.requireNonNull(role, "role is required");
        this.fullName = normaliseFullName(fullName);
        this.email = normaliseEmail(email);
        this.passwordHash = requireText(passwordHash, "passwordHash");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt is required");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt is required");

        if (this.updatedAt.isBefore(this.createdAt)) {
            throw new IllegalArgumentException("updatedAt cannot be before createdAt");
        }
    }

    // ---------------------------------------------------------------- factories

    /**
     * Creates a brand new customer. The caller (application service) supplies the id,
     * the already-hashed password and the clock reading, so this class stays deterministic
     * and trivially testable.
     */
    public static UserProfile register(String id,
                                       String username,
                                       String fullName,
                                       String email,
                                       String passwordHash,
                                       Instant registeredAt) {
        return new UserProfile(id, username, UserRole.CUSTOMER, fullName, email,
                passwordHash, registeredAt, registeredAt);
    }

    /** Creates a brand new administrator (bootstrap / admin-created accounts). */
    public static UserProfile registerAsAdmin(String id,
                                              String username,
                                              String fullName,
                                              String email,
                                              String passwordHash,
                                              Instant registeredAt) {
        return new UserProfile(id, username, UserRole.ADMIN, fullName, email,
                passwordHash, registeredAt, registeredAt);
    }

    /**
     * Rebuilds an existing user from stored state. Only persistence adapters should call this:
     * it bypasses the "new users start as CUSTOMER, created == updated" rules on purpose.
     */
    public static UserProfile reconstitute(String id,
                                           String username,
                                           UserRole role,
                                           String fullName,
                                           String email,
                                           String passwordHash,
                                           Instant createdAt,
                                           Instant updatedAt) {
        return new UserProfile(id, username, role, fullName, email,
                passwordHash, createdAt, updatedAt);
    }

    // ---------------------------------------------------------------- behaviour

    /** Returns a copy with a new display name and email. Username and role are unchanged. */
    public UserProfile updateProfile(String newFullName, String newEmail, Instant updatedAt) {
        return new UserProfile(id, username, role, newFullName, newEmail,
                passwordHash, createdAt, updatedAt);
    }

    /** Returns a copy carrying a different role; returns {@code this} if the role is unchanged. */
    public UserProfile changeRole(UserRole newRole, Instant updatedAt) {
        Objects.requireNonNull(newRole, "role is required");
        if (newRole == this.role) {
            return this;
        }
        return new UserProfile(id, username, newRole, fullName, email,
                passwordHash, createdAt, updatedAt);
    }

    /**
     * Returns a copy with a new password hash.
     *
     * @param newPasswordHash the hash produced by an outbound port, never a raw password
     */
    public UserProfile changePassword(String newPasswordHash, Instant updatedAt) {
        if (Objects.equals(this.passwordHash, requireText(newPasswordHash, "passwordHash"))) {
            throw new IllegalArgumentException("New password must differ from the current one");
        }
        return new UserProfile(id, username, role, fullName, email,
                newPasswordHash, createdAt, updatedAt);
    }

    public boolean isAdmin() {
        return role == UserRole.ADMIN;
    }

    public boolean isCustomer() {
        return role == UserRole.CUSTOMER;
    }

    /** True when this user may act on the account belonging to {@code ownerId}. */
    public boolean canAccessAccountsOf(String ownerId) {
        return isAdmin() || this.id.equals(ownerId);
    }

    // ---------------------------------------------------------------- accessors

    public String getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public UserRole getRole() {
        return role;
    }

    public String getFullName() {
        return fullName;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    // ---------------------------------------------------------------- invariants

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }

    private static String normaliseUsername(String value) {
        String username = requireText(value, "username");
        if (username.length() < USERNAME_MIN_LENGTH || username.length() > USERNAME_MAX_LENGTH) {
            throw new IllegalArgumentException(
                    "username must be " + USERNAME_MIN_LENGTH + "-" + USERNAME_MAX_LENGTH + " characters");
        }
        return username;
    }

    private static String normaliseFullName(String value) {
        String fullName = requireText(value, "fullName");
        if (fullName.length() > FULL_NAME_MAX_LENGTH) {
            throw new IllegalArgumentException(
                    "fullName must be at most " + FULL_NAME_MAX_LENGTH + " characters");
        }
        return fullName;
    }

    private static String normaliseEmail(String value) {
        String email = requireText(value, "email").toLowerCase();
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            throw new IllegalArgumentException("email format is invalid");
        }
        return email;
    }

    // ---------------------------------------------------------------- identity

    /** Identity is the id alone: two snapshots of the same user are the same user. */
    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof UserProfile)) {
            return false;
        }
        return id.equals(((UserProfile) other).id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    /** Never prints the password hash. */
    @Override
    public String toString() {
        return "UserProfile{id='" + id + "', username='" + username + "', role=" + role + "}";
    }
}
