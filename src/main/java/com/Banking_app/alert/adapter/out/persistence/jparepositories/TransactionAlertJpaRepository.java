package com.Banking_app.alert.adapter.out.persistence.jparepositories;

import com.Banking_app.alert.adapter.out.persistence.entities.TransactionAlertJpaEntity;
import com.Banking_app.alert.domain.enums.AlertRule;
import com.Banking_app.alert.domain.enums.AlertStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;

public interface TransactionAlertJpaRepository extends JpaRepository<TransactionAlertJpaEntity, Long> {

    /** Sorting comes from the Pageable. */
    Page<TransactionAlertJpaEntity> findByStatus(AlertStatus status, Pageable pageable);

    long countByStatus(AlertStatus status);

    boolean existsByRuleAndAccountNumberAndCreatedAtAfter(AlertRule rule, String accountNumber, Instant after);
}
