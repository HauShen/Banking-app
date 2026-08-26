package com.Banking_app.userProfile.application.port.in.usecase;

import com.Banking_app.userProfile.domain.models.userprofile.UserProfile;

public interface GetUserProfileUseCase {
    /**
     * @throws com.Banking_app.exception.ResourceNotFoundException if no such user exists
     */
    UserProfile byId(String id);

    /**
     * @throws com.Banking_app.exception.ResourceNotFoundException if no such user exists
     */
    UserProfile byUsername(String username);
}
