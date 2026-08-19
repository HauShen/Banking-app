package com.Banking_app.userProfile.application.port.out;

import com.Banking_app.userProfile.domain.models.userprofile.UserProfile;

import java.util.Optional;

public interface LoadUserProfilePort {
    Optional<UserProfile> findById(String id);

    Optional<UserProfile> findByUsername(String username);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);
}
