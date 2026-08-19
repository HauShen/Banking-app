package com.Banking_app.userProfile.adapter.out.persistence.mappers;

import com.Banking_app.userProfile.adapter.out.persistence.entities.UserProfileJpaEntity;
import com.Banking_app.userProfile.domain.models.userprofile.UserProfile;
import org.springframework.stereotype.Component;

@Component
public class UserProfilePersistenceMapper {
    public UserProfile toDomain(UserProfileJpaEntity entity) {
        return UserProfile.reconstitute(
                entity.getId(),
                entity.getUsername(),
                entity.getRole(),
                entity.getFullName(),
                entity.getEmail(),
                entity.getPassword(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }

    public UserProfileJpaEntity toNewEntity(UserProfile userProfile) {
        return new UserProfileJpaEntity(
                userProfile.getId(),
                userProfile.getUsername(),
                userProfile.getRole(),
                userProfile.getFullName(),
                userProfile.getEmail(),
                userProfile.getPasswordHash(),
                userProfile.getCreatedAt(),
                userProfile.getUpdatedAt());
    }

    public void updateEntity(UserProfileJpaEntity entity, UserProfile userProfile) {
        entity.setUsername(userProfile.getUsername());
        entity.setRole(userProfile.getRole());
        entity.setFullName(userProfile.getFullName());
        entity.setEmail(userProfile.getEmail());
        entity.setPassword(userProfile.getPasswordHash());
        entity.setUpdatedAt(userProfile.getUpdatedAt());
    }
}
