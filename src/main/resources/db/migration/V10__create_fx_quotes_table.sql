-- Locked exchange-rate quotes (fx module, foreign-currency plan Phase 5C). A customer locks the bank's sell rate for
-- a short time (fx.quote-validity, 60 s by default) and then pays a transfer in that foreign currency at exactly
-- this rate. Single-use: used_at is set by the transfer that uses the quote. Only the user who locked it can use it;
-- deleting the user deletes their quotes. Old rows are not cleaned up yet (README: known limitations).
CREATE TABLE fx_quotes (
    id            VARCHAR(36)   PRIMARY KEY,
    user_id       VARCHAR(36)   NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    currency      VARCHAR(3)    NOT NULL,
    amount        NUMERIC(19,2) NOT NULL,
    exchange_rate NUMERIC(19,8) NOT NULL,
    myr_amount    NUMERIC(19,2) NOT NULL,
    created_at    TIMESTAMPTZ   NOT NULL,
    expires_at    TIMESTAMPTZ   NOT NULL,
    used_at       TIMESTAMPTZ,
    CONSTRAINT chk_fx_quotes_positive CHECK (amount > 0 AND exchange_rate > 0 AND myr_amount > 0),
    CONSTRAINT chk_fx_quotes_expiry   CHECK (expires_at > created_at)
);

CREATE INDEX idx_fx_quotes_user ON fx_quotes (user_id);
