-- Idempotent consumers: one row per (consumer, event) that has been handled. A consumer inserts its row in the same
-- transaction as the work the event causes; if the row already exists, the event is a redelivery and is skipped.
-- Each consumer (notifications now, alerts later) keeps its own rows, so one event is handled once by each.
CREATE TABLE processed_events (
    consumer     VARCHAR(64) NOT NULL,
    event_id     UUID        NOT NULL,
    processed_at TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (consumer, event_id)
);

-- In-app notifications, created from transaction events (notification module). Deleting a user deletes theirs.
CREATE TABLE notifications (
    id         BIGSERIAL     PRIMARY KEY,
    event_id   UUID          NOT NULL,              -- the event it came from (a transfer gives two notifications)
    user_id    VARCHAR(36)   NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    type       VARCHAR(32)   NOT NULL,
    message    VARCHAR(255)  NOT NULL,
    reference  VARCHAR(100)  NOT NULL,              -- transactions.reference_number
    amount     NUMERIC(19,2) NOT NULL,
    created_at TIMESTAMPTZ   NOT NULL,              -- when the money moved
    read_at    TIMESTAMPTZ,                         -- NULL = unread
    CONSTRAINT chk_notifications_amount CHECK (amount > 0)
);

-- A user's list, newest first; and the unread count for the header bell.
CREATE INDEX idx_notifications_user_created ON notifications (user_id, created_at DESC, id DESC);
CREATE INDEX idx_notifications_user_unread ON notifications (user_id) WHERE read_at IS NULL;
