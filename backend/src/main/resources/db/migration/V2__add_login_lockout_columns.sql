-- Story: US-002 — Customer Login
-- Flyway migration V2: add login-lockout columns and extend status constraint

ALTER TABLE customers
    ADD COLUMN failed_login_attempts INT         NOT NULL DEFAULT 0,
    ADD COLUMN locked_at             TIMESTAMP   NULL,
    -- password_changed_at: column added now; V3 will back-fill existing rows (US-004)
    ADD COLUMN password_changed_at   TIMESTAMPTZ NULL;

-- Drop old status check (LOCKED not yet included)
ALTER TABLE customers DROP CONSTRAINT chk_customers_status;

-- Re-add with LOCKED included
ALTER TABLE customers
    ADD CONSTRAINT chk_customers_status
        CHECK (status IN (
            'PENDING_VERIFICATION',
            'ACTIVE',
            'SUSPENDED',
            'CLOSED',
            'LOCKED'
        ));

COMMENT ON COLUMN customers.failed_login_attempts IS 'Consecutive failed login attempts since last success. Reset to 0 on successful login.';
COMMENT ON COLUMN customers.locked_at             IS 'Timestamp when the account was locked due to too many failed attempts.';
COMMENT ON COLUMN customers.password_changed_at   IS 'Timestamp of last password change; used by JwtAuthenticationFilter to invalidate pre-change tokens (US-004).';
