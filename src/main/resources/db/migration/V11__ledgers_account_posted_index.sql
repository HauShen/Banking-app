-- The balance history reads one account's ledger entries by time: the last one before a day, and all since then.
-- id is included so entries posted in the same microsecond keep their order without a sort.
CREATE INDEX idx_ledgers_account_created ON ledgers (account_id, created_at, id);
