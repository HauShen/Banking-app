package com.Banking_app.userProfile.application.service;

import com.Banking_app.userProfile.application.port.in.command.RegisterUserProfileCommand;
import com.Banking_app.userProfile.application.port.in.usecase.BootstrapFirstAdminUseCase;
import com.Banking_app.userProfile.application.port.out.*;
import com.Banking_app.userProfile.domain.models.userprofile.UserProfile;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

import java.beans.Transient;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class BootstrapFirstAdminService implements BootstrapFirstAdminUseCase {
    private final LoadUserProfilePort loadUserProfilePort;
    private final SaveUserProfilePort saveUserProfilePort;
    private final PasswordHasherPort passwordHasher;
    private final AuditPort auditPort;
    private final Clock clock;

    public BootstrapFirstAdminService(LoadUserProfilePort loadUserProfilePort,
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
    public Optional<UserProfile> bootstrapIfNoAdminExists(RegisterUserProfileCommand command){
        if (loadUserProfilePort.existsAnyAdmin()) {
            return Optional.empty();
        }
        // Defensive: the admin table is empty but this username/email may be taken by a customer.
        if (loadUserProfilePort.existsByUsername(command.username())
                || loadUserProfilePort.existsByEmail(command.email())) {
            return Optional.empty();
        }

        Instant now = clock.instant();

        UserProfile firstAdmin = saveUserProfilePort.save(UserProfile.registerAsAdmin(
                UUID.randomUUID().toString(),
                command.username(),
                command.fullName(),
                command.email(),
                passwordHasher.hash(command.rawPassword()),
                now));

        // actorId is null: no human did this, the system did. That is itself worth recording.
        auditPort.record(AuditAction.ADMIN_BOOTSTRAPPED, null, firstAdmin.getId(),
                "bootstrap admin " + firstAdmin.getUsername(), now);

        return Optional.of(firstAdmin);
    }

}
