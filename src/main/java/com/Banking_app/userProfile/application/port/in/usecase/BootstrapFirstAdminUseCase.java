package com.Banking_app.userProfile.application.port.in.usecase;

import com.Banking_app.userProfile.application.port.in.command.RegisterUserProfileCommand;
import com.Banking_app.userProfile.domain.models.userprofile.UserProfile;

import java.util.Optional;

public interface BootstrapFirstAdminUseCase {
    Optional<UserProfile> bootstrapIfNoAdminExists(RegisterUserProfileCommand command);
}
