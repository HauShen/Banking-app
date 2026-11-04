-- Refresh tokens (rotating, one-time use). Only a SHA-256 hash of each token is stored: a database leak does not
-- leak usable tokens. Every login starts a new "family"; each refresh replaces the token with a new one in the
-- same family. Using an already-replaced token again means someone has a copy -> the whole family is revoked.
CREATE TABLE refresh_tokens (
    id          VARCHAR(36) PRIMARY KEY,
    user_id     VARCHAR(36) NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    family_id   VARCHAR(36) NOT NULL,
    token_hash  VARCHAR(64) NOT NULL UNIQUE,
    created_at  TIMESTAMPTZ NOT NULL,
    expires_at  TIMESTAMPTZ NOT NULL,
    revoked_at  TIMESTAMPTZ,
    replaced_by VARCHAR(36)
);

CREATE INDEX idx_refresh_tokens_family ON refresh_tokens (family_id);
CREATE INDEX idx_refresh_tokens_user   ON refresh_tokens (user_id);
