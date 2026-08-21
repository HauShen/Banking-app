package com.Banking_app.userProfile.application.port.in.usecase;

import com.Banking_app.userProfile.application.port.in.command.ChangeUserRoleCommand;
import com.Banking_app.userProfile.domain.models.userprofile.UserProfile;

public interface ChangeUserRoleUseCase {
    UserProfile changeRole(ChangeUserRoleCommand command);
}
