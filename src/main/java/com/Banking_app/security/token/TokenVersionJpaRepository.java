package com.Banking_app.security.token;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;

public interface TokenVersionJpaRepository extends JpaRepository<TokenVersionJpaEntity, String> {

    /**
     * Version + 1 in one atomic statement (PostgreSQL upsert): the first revocation inserts version 1,
     * later ones increment it. Two logouts at the same moment can't overwrite each other.
     */
    @Modifying(clearAutomatically = true)
    @Query(value = """
            INSERT INTO user_token_versions (user_id, version, updated_at)
            VALUES (:userId, 1, :now)
            ON CONFLICT (user_id) DO UPDATE
                SET version = user_token_versions.version + 1,
                    updated_at = :now
            """, nativeQuery = true)
    void increment(@Param("userId") String userId, @Param("now") Instant now);
}
