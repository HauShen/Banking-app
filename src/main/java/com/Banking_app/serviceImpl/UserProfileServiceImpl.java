package com.Banking_app.serviceImpl;
import com.Banking_app.dto.UserProfileMapper;
import com.Banking_app.exception.ResourceNotFoundException;
import com.Banking_app.models.UserProfile;
import com.Banking_app.models.enums.UserRole;
import com.Banking_app.repositories.UserProfileRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.Banking_app.service.UserProfileService;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class UserProfileServiceImpl implements UserProfileService {
    private final UserProfileRepository userProfileRepository;
    private final PasswordEncoder passwordEncoder;
    @Autowired
    public UserProfileServiceImpl(UserProfileRepository userProfileRepository, PasswordEncoder passwordEncoder){
        this.userProfileRepository = userProfileRepository;
        this.passwordEncoder = passwordEncoder;
    }
    @Override
    public UserProfile register (String username, String fullName, String email, String rawPassword, UserRole role){
        if (userProfileRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("Username already exists");
        }
        if (userProfileRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email already exists");
        }
        Instant now  = Instant.now();
        UserProfile user = new UserProfile();
        user.setId(UUID.randomUUID().toString());
        user.setUsername(username);
        user.setFullName(fullName);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setRole(role == null ? UserRole.CUSTOMER : role);
        user.setCreatedAt(now);
        user.setUpdatedAt(now);

        return userProfileRepository.save(user);
    }
    @Override
    public Optional<UserProfile> getById(String id) {
        return userProfileRepository.findById(id);
    }
    @Override
    public Optional<UserProfile> getByUsername(String username) {
        return userProfileRepository.findByUsername(username);
    }
    @Override
    public UserProfile updateProfile(String id, String fullName, String email) {
        UserProfile user = userProfileRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        user.setFullName(fullName);
        user.setEmail(email);
        user.setUpdatedAt(Instant.now());

        return userProfileRepository.save(user);
    }
    @Override
    public UserProfile updateRole(String id, UserRole role) {
        UserProfile user = userProfileRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        user.setRole(role);
        user.setUpdatedAt(Instant.now());
        return userProfileRepository.save(user);
    }
    @Override
    public void deleteById(String id) {
        if (!userProfileRepository.existsById(id)) {
            throw new ResourceNotFoundException("User not found");
        }
        userProfileRepository.deleteById(id);
    }

}
