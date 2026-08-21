package com.Banking_app.userProfile.adapter.out.audit;
import com.Banking_app.userProfile.application.port.out.AuditAction;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "audit_events")
public class AuditEventJpaEntity {
    @Id
    @Column(name = "id", nullable = false)
    private String id;

    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false)
    private AuditAction action;

    /** Null when the system acted rather than a person — e.g. bootstrap. */
    @Column(name = "actor_id")
    private String actorId;

    @Column(name = "target_user_id", nullable = false)
    private String targetUserId;

    @Column(name = "detail")
    private String detail;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    protected AuditEventJpaEntity() {
        // for JPA
    }

    public AuditEventJpaEntity(String id, AuditAction action, String actorId,
                               String targetUserId, String detail, Instant occurredAt) {
        this.id = id;
        this.action = action;
        this.actorId = actorId;
        this.targetUserId = targetUserId;
        this.detail = detail;
        this.occurredAt = occurredAt;
    }

    public String getId() {
        return id;
    }

    public AuditAction getAction() {
        return action;
    }

    public String getActorId() {
        return actorId;
    }

    public String getTargetUserId() {
        return targetUserId;
    }

    public String getDetail() {
        return detail;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }
}
