package com.Banking_app.userProfile.application.port.in.usecase;

import com.Banking_app.userProfile.application.port.in.command.DeleteUserProfileCommand;

public interface DeleteUserProfileUseCase {
    void delete(DeleteUserProfileCommand command);
}
