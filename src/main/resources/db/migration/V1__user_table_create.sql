CREATE TABLE users (
    id VARCHAR(36) PRIMARY KEY, -- Specific length for UUID strings
    role VARCHAR(20) NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    email VARCHAR(150) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL, -- Better for java.time.Instant
    updated_at TIMESTAMPTZ NOT NULL  -- Better for java.time.Instant
);