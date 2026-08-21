package com.Banking_app.userProfile.application.port.out;

import java.time.Instant;

public interface AuditPort {
    void record(AuditAction action, String actorId, String targetUserId, String detail, Instant at);
}
