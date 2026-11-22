package com.Banking_app.transaction.adapter.out.persistence.jparepositories;

import com.Banking_app.transaction.adapter.out.persistence.entities.OutboxEventJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OutboxEventJpaRepository extends JpaRepository<OutboxEventJpaEntity, Long> {
}
