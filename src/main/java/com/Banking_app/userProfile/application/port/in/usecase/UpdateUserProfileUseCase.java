package com.Banking_app.userProfile.application.port.in.usecase;

import com.Banking_app.userProfile.application.port.in.command.UpdateUserProfileCommand;
import com.Banking_app.userProfile.domain.models.userprofile.UserProfile;

public interface UpdateUserProfileUseCase {
    UserProfile updateProfile(UpdateUserProfileCommand command);
}
