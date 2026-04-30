package com.Banking_app.repositories;

import com.Banking_app.models.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserProfileRepository extends JpaRepository<UserProfile, String> {
    Optional<UserProfile> findByUsername(String userName);
    boolean existsByUsername(String userName);
    boolean existsByEmail(String email);
}
