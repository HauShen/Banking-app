package com.Banking_app.userProfile.application.port.out;

import com.Banking_app.userProfile.domain.models.userprofile.UserProfile;

public interface UserProfileRepository {
    UserProfile findById(String id);
    UserProfile findByUsername(String username);
    void save(UserProfile userProfile);
    void deleteById(String id);
}
