package com.Banking_app.userProfile.application.port.in.usecase;
import com.Banking_app.userProfile.application.port.in.command.CreateAdminCommand;
import com.Banking_app.userProfile.application.port.in.command.RegisterUserProfileCommand;
import com.Banking_app.userProfile.domain.models.userprofile.UserProfile;

public interface CreateAdminUserProfileUseCase {
    UserProfile createAdmin(CreateAdminCommand command);
}
