package com.Banking_app.userProfile.adapter.out.audit;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditEventJpaRepository extends JpaRepository<AuditEventJpaEntity, String> {
}
