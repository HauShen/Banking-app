package com.Banking_app.userProfile.application.service;
import com.Banking_app.exception.ResourceNotFoundException;
import com.Banking_app.userProfile.application.exception.DuplicateUserException;
import com.Banking_app.userProfile.application.port.in.command.UpdateUserProfileCommand;
import com.Banking_app.userProfile.application.port.in.usecase.UpdateUserProfileUseCase;
import com.Banking_app.userProfile.application.port.out.AuditAction;
import com.Banking_app.userProfile.application.port.out.AuditPort;
import com.Banking_app.userProfile.application.port.out.LoadUserProfilePort;
import com.Banking_app.userProfile.application.port.out.SaveUserProfilePort;
import com.Banking_app.userProfile.domain.models.userprofile.UserProfile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Clock;
import java.time.Instant;

@Service
public class UpdateUserProfileService implements UpdateUserProfileUseCase {
    private final LoadUserProfilePort loadUserProfilePort;
    private final SaveUserProfilePort saveUserProfilePort;
    private final AuditPort auditPort;
    private final Clock clock;

    public UpdateUserProfileService(LoadUserProfilePort loadUserProfilePort,
                                    SaveUserProfilePort saveUserProfilePort,
                                    AuditPort auditPort,
                                    Clock clock) {
        this.loadUserProfilePort = loadUserProfilePort;
        this.saveUserProfilePort = saveUserProfilePort;
        this.auditPort = auditPort;
        this.clock = clock;
    }

    @Override
    @Transactional
    public UserProfile updateProfile(UpdateUserProfileCommand command) {
        UserProfile target = loadUserProfilePort.findById(command.targetUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        requireEmailFree(command.email(), target.getId());

        Instant now = clock.instant();
        UserProfile updated = saveUserProfilePort.save(
                target.updateProfile(command.fullName(), command.email(), now));

        // Only an edit performed on somebody else is a privileged act worth a permanent record.
        // A user changing their own display name is ordinary self-service.
        if (!command.isSelfEdit()) {
            auditPort.record(AuditAction.PROFILE_UPDATED, command.actingUserId(), updated.getId(),
                    "profile edited by another user", now);
        }

        return updated;
    }

    /** Rejects the email only when it is held by a <em>different</em> user. */
    private void requireEmailFree(String email, String targetUserId) {
        loadUserProfilePort.findByEmail(email)
                .filter(owner -> !owner.getId().equals(targetUserId))
                .ifPresent(owner -> {
                    throw new DuplicateUserException("Email already exists");
                });
    }
}
