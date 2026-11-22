package com.Banking_app.userProfile.application.port.in.usecase;

import com.Banking_app.userProfile.application.port.in.command.ChangePasswordCommand;
import com.Banking_app.userProfile.domain.models.userprofile.UserProfile;

public interface ChangePasswordUseCase {

    /**
     * @return the user with the new password hash
     * @throws com.Banking_app.userProfile.application.exception.IncorrectCurrentPasswordException  wrong current password
     * @throws com.Banking_app.userProfile.application.exception.PasswordChangeRejectedException    new = current
     */
    UserProfile changePassword(ChangePasswordCommand command);
}
