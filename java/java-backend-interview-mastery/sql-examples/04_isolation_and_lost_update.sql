-- 04_isolation_and_lost_update.sql — chapter 17. Run after 01_schema_and_seed.sql.
-- Open TWO psql sessions (S1, S2) and run the marked steps in order to see the race.

-- =========================================================================
-- (A) The LOST UPDATE race at READ COMMITTED (the default)
-- =========================================================================
-- S1: BEGIN;
-- S1: SELECT balance FROM account WHERE id = 1;          -- 100
-- S2: BEGIN;
-- S2: SELECT balance FROM account WHERE id = 1;          -- 100
-- S1: UPDATE account SET balance = 110 WHERE id = 1;      -- adds 10 (app computed 100+10)
-- S1: COMMIT;
-- S2: UPDATE account SET balance = 110 WHERE id = 1;      -- also writes 110 -> S1's +10 is LOST
-- S2: COMMIT;
-- Result: balance = 110, not 120.

-- =========================================================================
-- FIX A: atomic in-place update (DB does the read-modify-write under a row lock)
-- =========================================================================
UPDATE account SET balance = balance + 10 WHERE id = 1;   -- concurrent runs serialize correctly

-- =========================================================================
-- FIX B: pessimistic lock (serialize the readers)
-- =========================================================================
-- S1: BEGIN; SELECT balance FROM account WHERE id = 1 FOR UPDATE;  -- locks the row
-- S2: BEGIN; SELECT balance FROM account WHERE id = 1 FOR UPDATE;  -- BLOCKS until S1 commits
-- S1: UPDATE account SET balance = balance + 10 WHERE id = 1; COMMIT;
-- S2: (unblocks, sees the committed value) UPDATE ...; COMMIT;

-- =========================================================================
-- FIX C: optimistic version check (no held lock; retry on 0 rows)
-- =========================================================================
-- Read current version in the app, then:
UPDATE account
SET balance = balance + 10, version = version + 1
WHERE id = 1 AND version = 0;    -- 0 rows affected => someone else updated => reload + retry

-- =========================================================================
-- FIX D: SERIALIZABLE (one transaction aborts with a serialization failure -> retry)
-- =========================================================================
-- S1/S2: BEGIN TRANSACTION ISOLATION LEVEL SERIALIZABLE; ... COMMIT;
-- One commit fails: PostgreSQL "could not serialize access" (Oracle: ORA-08177). Retry it.

-- =========================================================================
-- (B) Idempotent write (prevents duplicates from retried requests) — chapter 17/18
-- =========================================================================
-- A unique key makes concurrent/retried inserts safe.
CREATE TABLE IF NOT EXISTS payment (
    idem_key VARCHAR(64) PRIMARY KEY,     -- client-supplied idempotency key / business key
    amount   NUMERIC(12,2) NOT NULL
);
INSERT INTO payment(idem_key, amount) VALUES ('abc-123', 50.00)
ON CONFLICT (idem_key) DO NOTHING;        -- retry is a no-op; duplicate cannot be created
-- Oracle: use MERGE INTO payment ... WHEN NOT MATCHED THEN INSERT ...

-- =========================================================================
-- (C) Deadlock avoidance: always update rows in a consistent order
-- =========================================================================
-- DEADLOCK-PRONE (opposite order in two sessions):
--   S1: UPDATE account SET balance=balance-1 WHERE id=1;  then WHERE id=2;
--   S2: UPDATE account SET balance=balance-1 WHERE id=2;  then WHERE id=1;  -- cycle
-- FIX: both sessions touch ids in ASCENDING order (1 then 2) -> no cycle.
-- When a deadlock is detected the DB aborts one victim (PG: "deadlock detected",
-- Oracle: ORA-00060); the app should retry the aborted transaction.
