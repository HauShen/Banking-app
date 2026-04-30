package com.Banking_app.service;

import com.Banking_app.models.UserProfile;
import com.Banking_app.models.enums.UserRole;

import java.util.Optional;

public interface UserProfileService {
    UserProfile register(String username, String fullName, String email, String rawPassword, UserRole role);
    Optional<UserProfile> getById(String id);
    Optional<UserProfile> getByUsername(String username);
    UserProfile updateProfile(String id, String fullName, String email);
    UserProfile updateRole(String id, UserRole role);
    void deleteById(String id);
}
