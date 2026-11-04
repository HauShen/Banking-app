package com.Banking_app.security.refresh;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface RefreshTokenJpaRepository extends JpaRepository<RefreshTokenJpaEntity, String> {

    /** Row lock: two refreshes with the same token are handled one after the other, never both successfully. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from RefreshTokenJpaEntity r where r.tokenHash = :tokenHash")
    Optional<RefreshTokenJpaEntity> findByTokenHashForUpdate(@Param("tokenHash") String tokenHash);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update RefreshTokenJpaEntity r set r.revokedAt = :now where r.familyId = :familyId and r.revokedAt is null")
    int revokeFamily(@Param("familyId") String familyId, @Param("now") Instant now);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update RefreshTokenJpaEntity r set r.revokedAt = :now where r.userId = :userId and r.revokedAt is null")
    int revokeAllForUser(@Param("userId") String userId, @Param("now") Instant now);
}
