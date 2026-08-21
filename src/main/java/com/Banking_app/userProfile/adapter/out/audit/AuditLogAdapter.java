package com.Banking_app.userProfile.adapter.out.audit;
import com.Banking_app.userProfile.application.port.out.AuditAction;
import com.Banking_app.userProfile.application.port.out.AuditPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
public class AuditLogAdapter implements AuditPort {
    private static final Logger log = LoggerFactory.getLogger(AuditLogAdapter.class);

    private final AuditEventJpaRepository repository;

    public AuditLogAdapter(AuditEventJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public void record(AuditAction action, String actorId, String targetUserId,
                       String detail, Instant at) {

        repository.save(new AuditEventJpaEntity(
                UUID.randomUUID().toString(), action, actorId, targetUserId, detail, at));

        log.info("AUDIT action={} actor={} target={} detail=\"{}\" at={}",
                action, actorId == null ? "SYSTEM" : actorId, targetUserId, detail, at);
    }
}
