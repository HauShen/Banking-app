ALTER TABLE accounts
    DROP CONSTRAINT IF EXISTS chk_accounts_min_balance;

ALTER TABLE accounts
    ADD CONSTRAINT chk_accounts_min_balance
        CHECK (current_balance >= 0.00);


ALTER TABLE transactions
    ALTER COLUMN from_account_id DROP NOT NULL;


ALTER TABLE transactions
    DROP CONSTRAINT IF EXISTS chk_transactions_diff_accounts;


ALTER TABLE transactions
    ADD CONSTRAINT chk_transactions_diff_accounts
        CHECK (from_account_id IS NULL OR from_account_id <> to_account_id);