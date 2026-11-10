package com.Banking_app.fx.adapter.out.persistence.jparepositories;

import com.Banking_app.fx.adapter.out.persistence.entities.LockedQuoteJpaEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface LockedQuoteJpaRepository extends JpaRepository<LockedQuoteJpaEntity, String> {

    /** SELECT … FOR UPDATE: the row stays locked until the surrounding transaction ends. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT q FROM LockedQuoteJpaEntity q WHERE q.id = :id")
    Optional<LockedQuoteJpaEntity> findByIdForUpdate(@Param("id") String id);
}
