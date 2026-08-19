package com.Banking_app.userProfile.application.port.out;

import com.Banking_app.userProfile.domain.models.userprofile.UserProfile;

public interface SaveUserProfilePort {
    UserProfile save(UserProfile userProfile);

    void deleteById(String id);
}
