package com.Banking_app.service;

import com.Banking_app.models.UserProfile;
import com.Banking_app.models.enums.UserRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface UserProfileService {
    UserProfile register(String username, String fullName, String email, String rawPassword);
    UserProfile registerAsAdmin(String username, String fullName, String email, String rawPassword);
    Page<UserProfile> findAllUsers(int page, int elements);
    Optional<UserProfile> getById(String id);
    Optional<UserProfile> getByUsername(String username);
    UserProfile updateProfile(String id, String fullName, String email);
    UserProfile updateRole(String id, UserRole role);
    void deleteById(String id);
}
