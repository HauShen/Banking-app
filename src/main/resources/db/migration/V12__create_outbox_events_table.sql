-- Transactional outbox: an event is inserted in the same database transaction as the money movement it describes,
-- so it is committed or rolled back together with the money. A relay sends unpublished rows to Kafka and stamps
-- published_at; until then a row simply waits (e.g. while Kafka is down).
CREATE TABLE outbox_events (
    id            BIGSERIAL    PRIMARY KEY,                -- also the sending order
    event_id      UUID         NOT NULL UNIQUE,            -- consumers recognise a repeated message by it
    topic         VARCHAR(100) NOT NULL,
    message_key   VARCHAR(30)  NOT NULL,                   -- an account number (accounts.account_number is 30)
    event_type    VARCHAR(64)  NOT NULL,                   -- e.g. TRANSFER_COMPLETED
    event_version INT          NOT NULL,
    payload       JSONB        NOT NULL,
    occurred_at   TIMESTAMPTZ  NOT NULL,
    published_at  TIMESTAMPTZ,                             -- NULL = not sent to Kafka yet
    attempts      INT          NOT NULL DEFAULT 0          -- failed sends so far
);

-- The relay only ever looks for unpublished rows, oldest first; published rows don't make this index grow.
CREATE INDEX idx_outbox_events_unpublished ON outbox_events (id) WHERE published_at IS NULL;
