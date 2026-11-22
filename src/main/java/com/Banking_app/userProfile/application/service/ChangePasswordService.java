package com.Banking_app.userProfile.application.service;

import com.Banking_app.exception.ResourceNotFoundException;
import com.Banking_app.userProfile.application.exception.IncorrectCurrentPasswordException;
import com.Banking_app.userProfile.application.exception.PasswordChangeRejectedException;
import com.Banking_app.userProfile.application.port.in.command.ChangePasswordCommand;
import com.Banking_app.userProfile.application.port.in.usecase.ChangePasswordUseCase;
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

/**
 * Checks the current password, refuses an unchanged one, stores the new hash and writes an audit event.
 * Ending the user's other sessions is the caller's job (AuthService): tokens are a security concern, not a
 * user-profile one.
 */
@Service
public class ChangePasswordService implements ChangePasswordUseCase {

    private final LoadUserProfilePort loadUserProfilePort;
    private final SaveUserProfilePort saveUserProfilePort;
    private final PasswordHasherPort passwordHasher;
    private final AuditPort auditPort;
    private final Clock clock;

    public ChangePasswordService(LoadUserProfilePort loadUserProfilePort,
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
    public UserProfile changePassword(ChangePasswordCommand command) {
        UserProfile user = loadUserProfilePort.findById(command.userId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!passwordHasher.matches(command.currentPassword(), user.getPasswordHash())) {
            throw new IncorrectCurrentPasswordException();
        }
        // Compared with the hasher: BCrypt hashes are salted, so two hashes of the same password never match.
        if (passwordHasher.matches(command.newPassword(), user.getPasswordHash())) {
            throw PasswordChangeRejectedException.sameAsCurrent();
        }

        Instant now = clock.instant();
        UserProfile changed = saveUserProfilePort.save(
                user.changePassword(passwordHasher.hash(command.newPassword()), now));
        auditPort.record(AuditAction.PASSWORD_CHANGED, user.getId(), user.getId(), "password changed by the user",
                now);
        return changed;
    }
}
