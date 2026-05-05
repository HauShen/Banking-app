CREATE TABLE transactions(
    id BIGSERIAL PRIMARY KEY,
    reference_number VARCHAR(100) NOT NULL,
    from_account_id BIGINT NOT NULL,
    to_account_id BIGINT NOT NULL,
    amount NUMERIC(19,2) NOT NULL,
    status VARCHAR(30) NOT NULL,
    description VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    success_at TIMESTAMPTZ,
    idempotency_key VARCHAR(100) NOT NULL,

    CONSTRAINT fk_transactions_from_account
        FOREIGN KEY (from_account_id)
        REFERENCES accounts(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_transactions_to_account
        FOREIGN KEY (to_account_id)
        REFERENCES accounts(id)
        ON DELETE RESTRICT,

    CONSTRAINT uk_transactions_reference_number
        UNIQUE (reference_number),

    CONSTRAINT uk_transactions_idempotency_key
        UNIQUE (idempotency_key),

    CONSTRAINT chk_transactions_amount_positive
        CHECK (amount > 0),

    CONSTRAINT chk_transactions_diff_accounts
        CHECK (from_account_id <> to_account_id)

);