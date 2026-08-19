package com.Banking_app.userProfile.application.service;

import com.Banking_app.userProfile.adapter.out.persistence.entities.UserProfileJpaEntity;
import com.Banking_app.userProfile.domain.enums.UserRole;
import org.springframework.data.domain.Page;

import java.util.Optional;

public interface UserProfileService {
    UserProfileJpaEntity register(String username, String fullName, String email, String rawPassword);
    UserProfileJpaEntity registerAsAdmin(String username, String fullName, String email, String rawPassword);
    Page<UserProfileJpaEntity> findAllUsers(int page, int elements);
    Optional<UserProfileJpaEntity> getById(String id);
    Optional<UserProfileJpaEntity> getByUsername(String username);
    UserProfileJpaEntity updateProfile(String id, String fullName, String email);
    UserProfileJpaEntity updateRole(String id, UserRole role);
    void deleteById(String id);
}
