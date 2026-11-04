-- Token version per user, owned by the security layer (not the user profile domain).
-- Every JWT carries the version it was issued with (claim "ver"). Increasing the number here revokes every
-- older token of that user at once: logout ("log out everywhere") or an admin revoking a compromised account.
-- No row means version 0. Deleting the user deletes the row.
CREATE TABLE user_token_versions (
    user_id    VARCHAR(36) PRIMARY KEY REFERENCES users (id) ON DELETE CASCADE,
    version    INT         NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);
