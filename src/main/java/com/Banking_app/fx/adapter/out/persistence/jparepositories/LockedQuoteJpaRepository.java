package com.Banking_app.fx.adapter.out.persistence.jparepositories;

import com.Banking_app.fx.adapter.out.persistence.entities.LockedQuoteJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LockedQuoteJpaRepository extends JpaRepository<LockedQuoteJpaEntity, String> {
}
