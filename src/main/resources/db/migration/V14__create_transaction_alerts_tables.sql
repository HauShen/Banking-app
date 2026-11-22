-- Transaction watch (alert module): transfers that an admin should look at. No foreign keys to users or accounts:
-- an alert is a record of what happened and stays even if the user or account is deleted later.
CREATE TABLE transaction_alerts (
    id             BIGSERIAL     PRIMARY KEY,
    event_id       UUID          NOT NULL,           -- the transfer's event
    rule           VARCHAR(32)   NOT NULL,           -- LARGE_TRANSFER, RAPID_TRANSFERS
    account_number VARCHAR(30)   NOT NULL,           -- the sending account
    owner_id       VARCHAR(36)   NOT NULL,
    reference      VARCHAR(100)  NOT NULL,
    amount         NUMERIC(19,2) NOT NULL,
    detail         VARCHAR(255)  NOT NULL,
    created_at     TIMESTAMPTZ   NOT NULL,           -- when the transfer happened
    status         VARCHAR(16)   NOT NULL DEFAULT 'OPEN',
    reviewed_at    TIMESTAMPTZ,
    reviewed_by    VARCHAR(36),                      -- the admin's user id
    CONSTRAINT uk_transaction_alerts_event_rule UNIQUE (event_id, rule),   -- one alert per rule and transfer
    CONSTRAINT chk_transaction_alerts_review CHECK ((status = 'REVIEWED') = (reviewed_at IS NOT NULL))
);

CREATE INDEX idx_transaction_alerts_status_created ON transaction_alerts (status, created_at DESC);
CREATE INDEX idx_transaction_alerts_account_rule ON transaction_alerts (account_number, rule, created_at);

-- The alert module's own memory of recent outgoing transfers, for the rapid-transfers rule. Entries older than the
-- rule's window are deleted as new transfers of the account come in.
CREATE TABLE alert_transfer_log (
    event_id       UUID        PRIMARY KEY,
    account_number VARCHAR(30) NOT NULL,
    occurred_at    TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_alert_transfer_log_account_time ON alert_transfer_log (account_number, occurred_at);
