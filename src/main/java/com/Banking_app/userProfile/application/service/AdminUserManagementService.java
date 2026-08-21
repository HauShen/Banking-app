package com.Banking_app.userProfile.application.service;

import com.Banking_app.exception.ResourceNotFoundException;
import com.Banking_app.userProfile.application.exception.LastAdminException;
import com.Banking_app.userProfile.application.port.in.command.ChangeUserRoleCommand;
import com.Banking_app.userProfile.application.port.in.command.DeleteUserProfileCommand;
import com.Banking_app.userProfile.application.port.in.usecase.ChangeUserRoleUseCase;
import com.Banking_app.userProfile.application.port.in.usecase.DeleteUserProfileUseCase;
import com.Banking_app.userProfile.application.port.out.AuditAction;
import com.Banking_app.userProfile.application.port.out.AuditPort;
import com.Banking_app.userProfile.application.port.out.LoadUserProfilePort;
import com.Banking_app.userProfile.application.port.out.SaveUserProfilePort;
import com.Banking_app.userProfile.domain.enums.UserRole;
import com.Banking_app.userProfile.domain.models.userprofile.UserProfile;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;


@Service
public class AdminUserManagementService implements ChangeUserRoleUseCase, DeleteUserProfileUseCase {
    private final LoadUserProfilePort loadUserProfilePort;
    private final SaveUserProfilePort saveUserProfilePort;
    private final AuditPort auditPort;
    private final Clock clock;

    public AdminUserManagementService(LoadUserProfilePort loadUserProfilePort,
                                      SaveUserProfilePort saveUserProfilePort,
                                      AuditPort auditPort,
                                      Clock clock) {
        this.loadUserProfilePort = loadUserProfilePort;
        this.saveUserProfilePort = saveUserProfilePort;
        this.auditPort = auditPort;
        this.clock = clock;
    }

    private UserProfile require(String id) {
        return loadUserProfilePort.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private void guardLastAdmin(String message) {
        if (loadUserProfilePort.countAdmins() <= 1) {
            throw new LastAdminException(message);
        }
    }

    @Override
    @Transactional
    public UserProfile changeRole(ChangeUserRoleCommand command){

        UserProfile target = require(command.targetUserId());

        if (target.getId().equals(command.actingAdminId())) {
            throw new LastAdminException("An admin cannot change their own role");
        }
        if (target.isAdmin() && command.newRole() != UserRole.ADMIN) {
            guardLastAdmin("Cannot demote the last remaining admin");
        }

        Instant now = clock.instant();
        UserRole previousRole = target.getRole();

        UserProfile updated = saveUserProfilePort.save(target.changeRole(command.newRole(), now));

        auditPort.record(AuditAction.ROLE_CHANGED, command.actingAdminId(), updated.getId(),
                previousRole + " -> " + updated.getRole(), now);

        return updated;
    }

    @Override
    @Transactional
    public void delete(DeleteUserProfileCommand command) {
        UserProfile target = require(command.targetUserId());

        if (target.getId().equals(command.actingAdminId())) {
            throw new LastAdminException("An admin cannot delete their own account");
        }
        if (target.isAdmin()) {
            guardLastAdmin("Cannot delete the last remaining admin");
        }

        Instant now = clock.instant();
        saveUserProfilePort.deleteById(target.getId());

        auditPort.record(AuditAction.USER_DELETED, command.actingAdminId(), target.getId(),
                "deleted " + target.getRole() + " " + target.getUsername(), now);
    }

}
