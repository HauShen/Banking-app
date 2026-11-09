-- Phase 5A step 1: transaction type + columns for foreign-currency transactions (plan: claude/foreign-currency-plan.md).
-- Accounts stay MYR only; transactions.amount stays the MYR amount that moved.

ALTER TABLE transactions
    ADD COLUMN transaction_type  VARCHAR(30),
    ADD COLUMN original_currency VARCHAR(3),
    ADD COLUMN original_amount   NUMERIC(19,2),
    ADD COLUMN exchange_rate     NUMERIC(19,8);

-- Existing rows: no from-account meant "cash deposit (top-up)"; everything else is a transfer.
UPDATE transactions
   SET transaction_type = CASE WHEN from_account_id IS NULL THEN 'CASH_DEPOSIT' ELSE 'TRANSFER' END;

ALTER TABLE transactions
    ALTER COLUMN transaction_type SET NOT NULL;

ALTER TABLE transactions
    ADD CONSTRAINT chk_transactions_type
        CHECK (transaction_type IN ('TRANSFER', 'CASH_DEPOSIT', 'FOREIGN_REMITTANCE'));

-- A sender (from-account) exists exactly for transfers.
ALTER TABLE transactions
    ADD CONSTRAINT chk_transactions_sender_matches_type
        CHECK ((transaction_type = 'TRANSFER') = (from_account_id IS NOT NULL));

-- The three foreign-currency columns are all set or all empty.
-- The IS NOT NULL tests are needed: a CHECK only rejects a row when the condition is FALSE, and a comparison with
-- NULL (e.g. NULL > 0) gives NULL, which would let a half-filled row through.
ALTER TABLE transactions
    ADD CONSTRAINT chk_transactions_fx_all_or_nothing
        CHECK ((original_currency IS NULL AND original_amount IS NULL AND exchange_rate IS NULL)
            OR (original_currency IS NOT NULL AND original_amount IS NOT NULL AND exchange_rate IS NOT NULL
                AND original_amount > 0 AND exchange_rate > 0));
