package com.Banking_app.userProfile.adapter.out.persistence.jparepositories;

import com.Banking_app.userProfile.adapter.out.persistence.entities.UserProfileJpaEntity;
import com.Banking_app.userProfile.domain.enums.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserProfileJpaRepository extends JpaRepository<UserProfileJpaEntity, String> {
    boolean existsByRole(UserRole role);
    long countByRole(UserRole role);
    Optional<UserProfileJpaEntity> findByUsername(String userName);

    boolean existsByUsername(String userName);
    boolean existsByEmail(String email);
}
