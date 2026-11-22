-- notification-service's own database (notifications_db). The same two tables as V13 of the main app, with one
-- difference: user_id has no foreign key, because the users table is in the main app's database. Deleting a user
-- there leaves their notifications here (nobody can read them any more: no token carries that user id again).

-- Idempotent consumer: one row per event this service has handled. The row is inserted in the same transaction as
-- the notifications; if it already exists, the event is a redelivery and is skipped.
CREATE TABLE processed_events (
    consumer     VARCHAR(64) NOT NULL,
    event_id     UUID        NOT NULL,
    processed_at TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (consumer, event_id)
);

CREATE TABLE notifications (
    id         BIGSERIAL     PRIMARY KEY,
    event_id   UUID          NOT NULL,              -- the event it came from (a transfer gives two notifications)
    user_id    VARCHAR(36)   NOT NULL,              -- users.id in the main app's database
    type       VARCHAR(32)   NOT NULL,
    message    VARCHAR(255)  NOT NULL,
    reference  VARCHAR(100)  NOT NULL,              -- transactions.reference_number in the main app
    amount     NUMERIC(19,2) NOT NULL,
    created_at TIMESTAMPTZ   NOT NULL,              -- when the money moved
    read_at    TIMESTAMPTZ,                         -- NULL = unread
    CONSTRAINT chk_notifications_amount CHECK (amount > 0)
);

-- A user's list, newest first; and the unread count for the header bell.
CREATE INDEX idx_notifications_user_created ON notifications (user_id, created_at DESC, id DESC);
CREATE INDEX idx_notifications_user_unread ON notifications (user_id) WHERE read_at IS NULL;
