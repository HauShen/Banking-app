package com.Banking_app.userProfile.application.port.in.usecase;

import com.Banking_app.userProfile.application.port.in.command.RegisterUserProfileCommand;
import com.Banking_app.userProfile.domain.models.userprofile.UserProfile;

public interface RegisterUserProfileUseCase {
    UserProfile register(RegisterUserProfileCommand command);
}
