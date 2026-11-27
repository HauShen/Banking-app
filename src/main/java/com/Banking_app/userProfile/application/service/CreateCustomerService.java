package com.Banking_app.userProfile.application.service;

import com.Banking_app.userProfile.application.exception.DuplicateUserException;
import com.Banking_app.userProfile.application.port.in.command.CreateCustomerCommand;
import com.Banking_app.userProfile.application.port.in.command.RegisterUserProfileCommand;
import com.Banking_app.userProfile.application.port.in.usecase.CreateCustomerUseCase;
import com.Banking_app.userProfile.application.port.out.AuditAction;
import com.Banking_app.userProfile.application.port.out.AuditPort;
import com.Banking_app.userProfile.application.port.out.LoadUserProfilePort;
import com.Banking_app.userProfile.application.port.out.PasswordHasherPort;
import com.Banking_app.userProfile.application.port.out.SaveUserProfilePort;
import com.Banking_app.userProfile.domain.models.userprofile.UserProfile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

/**
 * Creates a CUSTOMER on an admin's behalf and records it in the audit log (CUSTOMER_CREATED, the admin as actor),
 * like CreateAdminUserProfileService does for admins. No tokens: the admin isn't logging in as the customer. The
 * admin hands over the initial password; the customer changes it with POST /api/auth/change-password.
 */
@Service
public class CreateCustomerService implements CreateCustomerUseCase {

    private final LoadUserProfilePort loadUserProfilePort;
    private final SaveUserProfilePort saveUserProfilePort;
    private final PasswordHasherPort passwordHasher;
    private final AuditPort auditPort;
    private final Clock clock;

    public CreateCustomerService(LoadUserProfilePort loadUserProfilePort,
                                 SaveUserProfilePort saveUserProfilePort,
                                 PasswordHasherPort passwordHasher,
                                 AuditPort auditPort,
                                 Clock clock) {
        this.loadUserProfilePort = loadUserProfilePort;
        this.saveUserProfilePort = saveUserProfilePort;
        this.passwordHasher = passwordHasher;
        this.auditPort = auditPort;
        this.clock = clock;
    }

    @Override
    @Transactional
    public UserProfile createCustomer(CreateCustomerCommand command) {
        RegisterUserProfileCommand registration = command.registration();
        if (loadUserProfilePort.existsByUsername(registration.username())) {
            throw new DuplicateUserException("Username already exists");
        }
        if (loadUserProfilePort.existsByEmail(registration.email())) {
            throw new DuplicateUserException("Email already exists");
        }

        Instant now = clock.instant();
        UserProfile customer = UserProfile.register(
                UUID.randomUUID().toString(),
                registration.username(),
                registration.fullName(),
                registration.email(),
                passwordHasher.hash(registration.rawPassword()),
                now);
        UserProfile saved = saveUserProfilePort.save(customer);

        auditPort.record(AuditAction.CUSTOMER_CREATED, command.actingAdminId(), saved.getId(),
                "created CUSTOMER " + saved.getUsername(), now);
        return saved;
    }
}
