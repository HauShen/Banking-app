package com.Banking_app.userProfile.application.service;

import com.Banking_app.userProfile.application.exception.DuplicateUserException;
import com.Banking_app.userProfile.application.port.in.command.CreateAdminCommand;
import com.Banking_app.userProfile.application.port.in.command.RegisterUserProfileCommand;
import com.Banking_app.userProfile.application.port.in.usecase.CreateAdminUserProfileUseCase;
import com.Banking_app.userProfile.application.port.out.*;
import com.Banking_app.userProfile.domain.models.userprofile.UserProfile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

@Service
public class CreateAdminUserProfileService implements CreateAdminUserProfileUseCase {
    private final LoadUserProfilePort loadUserProfilePort;
    private final SaveUserProfilePort saveUserProfilePort;
    private final PasswordHasherPort passwordHasher;
    private final AuditPort auditPort;
    private final Clock clock;

    public CreateAdminUserProfileService(LoadUserProfilePort loadUserProfilePort,
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
    public UserProfile createAdmin(CreateAdminCommand command)  {
        RegisterUserProfileCommand registration = command.registration();


        if (loadUserProfilePort.existsByUsername(registration.username())) {
            throw new DuplicateUserException("Username already exists");
        }
        if (loadUserProfilePort.existsByEmail(registration.email())) {
            throw new DuplicateUserException("Email already exists");
        }

        Instant now = clock.instant();

        UserProfile newAdmin = UserProfile.registerAsAdmin(
                UUID.randomUUID().toString(),
                registration.username(),
                registration.fullName(),
                registration.email(),
                passwordHasher.hash(registration.rawPassword()),
                now);

        UserProfile saved = saveUserProfilePort.save(newAdmin);

        auditPort.record(AuditAction.ADMIN_CREATED, command.actingAdminId(), saved.getId(),
                "created ADMIN " + saved.getUsername(), now);

        return saved;
    }

}
