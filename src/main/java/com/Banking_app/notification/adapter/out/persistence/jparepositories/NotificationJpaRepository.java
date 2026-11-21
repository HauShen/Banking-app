package com.Banking_app.notification.adapter.out.persistence.jparepositories;

import com.Banking_app.notification.adapter.out.persistence.entities.NotificationJpaEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;

public interface NotificationJpaRepository extends JpaRepository<NotificationJpaEntity, Long> {

    /** Sorting comes from the Pageable. */
    Page<NotificationJpaEntity> findByUserId(String userId, Pageable pageable);

    Page<NotificationJpaEntity> findByUserIdAndReadAtIsNull(String userId, Pageable pageable);

    long countByUserIdAndReadAtIsNull(String userId);

    @Modifying
    @Query("UPDATE NotificationJpaEntity n SET n.readAt = :at WHERE n.userId = :userId AND n.readAt IS NULL")
    int markAllRead(@Param("userId") String userId, @Param("at") Instant at);
}
