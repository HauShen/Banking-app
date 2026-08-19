package com.Banking_app.userProfile.application.serviceImpl;
import com.Banking_app.exception.ResourceNotFoundException;
import com.Banking_app.userProfile.adapter.out.persistence.entities.UserProfileJpaEntity;
import com.Banking_app.userProfile.application.service.UserProfileService;
import com.Banking_app.userProfile.domain.enums.UserRole;
import com.Banking_app.userProfile.adapter.out.persistence.jparepositories.UserProfileJpaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.data.domain.Sort;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class UserProfileServiceImpl implements UserProfileService {
    private final UserProfileJpaRepository userProfileJpaRepository;
    private final PasswordEncoder passwordEncoder;
    @Autowired
    public UserProfileServiceImpl(UserProfileJpaRepository userProfileJpaRepository, PasswordEncoder passwordEncoder){
        this.userProfileJpaRepository = userProfileJpaRepository;
        this.passwordEncoder = passwordEncoder;
    }
    @Override
    public UserProfileJpaEntity register(String username, String fullName, String email, String rawPassword){
        if (userProfileJpaRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("Username already exists");
        }
        if (userProfileJpaRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email already exists");
        }
        Instant now  = Instant.now();
        UserProfileJpaEntity user = new UserProfileJpaEntity();
        user.setId(UUID.randomUUID().toString());
        user.setUsername(username);
        user.setFullName(fullName);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setRole(UserRole.CUSTOMER);
        user.setCreatedAt(now);
        user.setUpdatedAt(now);

        return userProfileJpaRepository.save(user);
    }
    @Override
    public UserProfileJpaEntity registerAsAdmin(String username, String fullName, String email, String rawPassword){
        if (userProfileJpaRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("Username already exists");
        }
        if (userProfileJpaRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email already exists");
        }
        Instant now  = Instant.now();
        UserProfileJpaEntity user = new UserProfileJpaEntity();
        user.setId(UUID.randomUUID().toString());
        user.setUsername(username);
        user.setFullName(fullName);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setRole(UserRole.ADMIN);
        user.setCreatedAt(now);
        user.setUpdatedAt(now);

        return userProfileJpaRepository.save(user);
    }
    @Override
    public Page<UserProfileJpaEntity> findAllUsers(int page, int elements){
        Pageable pageable = PageRequest.of(page, elements, Sort.by(Sort.Direction.ASC, "username"));
        return userProfileJpaRepository.findAll(pageable);
    }
    @Override
    public Optional<UserProfileJpaEntity> getById(String id) {
        return userProfileJpaRepository.findById(id);
    }
    @Override
    public Optional<UserProfileJpaEntity> getByUsername(String username) {
        return userProfileJpaRepository.findByUsername(username);
    }
    @Override
    public UserProfileJpaEntity updateProfile(String id, String fullName, String email) {
        UserProfileJpaEntity user = userProfileJpaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        user.setFullName(fullName);
        user.setEmail(email);
        user.setUpdatedAt(Instant.now());

        return userProfileJpaRepository.save(user);
    }
    @Override
    public UserProfileJpaEntity updateRole(String id, UserRole role) {
        UserProfileJpaEntity user = userProfileJpaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        user.setRole(role);
        user.setUpdatedAt(Instant.now());
        return userProfileJpaRepository.save(user);
    }
    @Override
    public void deleteById(String id) {
        if (!userProfileJpaRepository.existsById(id)) {
            throw new ResourceNotFoundException("User not found");
        }
        userProfileJpaRepository.deleteById(id);
    }

}
