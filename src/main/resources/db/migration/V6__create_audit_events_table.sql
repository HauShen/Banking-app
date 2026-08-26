CREATE TABLE audit_events (
                              id             VARCHAR(36)  PRIMARY KEY,
                              action         VARCHAR(32)  NOT NULL,
                              actor_id       VARCHAR(36),
                              target_user_id VARCHAR(36)  NOT NULL,
                              detail         VARCHAR(500),
                              occurred_at    TIMESTAMPTZ  NOT NULL
);

CREATE INDEX idx_audit_events_target ON audit_events (target_user_id);
CREATE INDEX idx_audit_events_at     ON audit_events (occurred_at);