CREATE TABLE accounts (
    id BIGSERIAL PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    account_number VARCHAR(30) NOT NULL UNIQUE,
    account_type VARCHAR(30) NOT NULL,
    account_status VARCHAR(30) NOT NULL,
    account_currency VARCHAR(3) NOT NULL DEFAULT 'MYR',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    current_balance NUMERIC(19,2) NOT NULL DEFAULT 20.00,

    CONSTRAINT fk_accounts_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE RESTRICT,

    CONSTRAINT chk_accounts_min_balance
        CHECK (current_balance >= 20.00),

    CONSTRAINT chk_accounts_currency
        CHECK (account_currency IN ('MYR'))
);