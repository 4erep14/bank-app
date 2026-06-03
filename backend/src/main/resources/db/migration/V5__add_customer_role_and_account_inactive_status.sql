-- Story: US-009
-- Adds a role column to customers (CUSTOMER | ADMIN) and extends the
-- accounts status check constraint to include INACTIVE.

-- 1. Add role column to customers (default CUSTOMER for all existing rows)
ALTER TABLE customers
    ADD COLUMN IF NOT EXISTS role VARCHAR(20) NOT NULL DEFAULT 'CUSTOMER';

ALTER TABLE customers
    ADD CONSTRAINT chk_customers_role
    CHECK (role IN ('CUSTOMER', 'ADMIN'));

-- 2. Extend accounts status constraint to include INACTIVE (AC4/AC5)
ALTER TABLE accounts
    DROP CONSTRAINT IF EXISTS chk_accounts_status;

ALTER TABLE accounts
    ADD CONSTRAINT chk_accounts_status
    CHECK (status IN ('ACTIVE', 'CLOSED', 'FROZEN', 'INACTIVE'));
