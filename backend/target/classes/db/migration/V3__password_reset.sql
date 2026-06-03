-- V3__password_reset.sql
-- Story: US-004 — Password Reset
-- Depends on: V1 (customers), V2 (login lockout).
-- NOTE: password_changed_at was already added in V2; it is NOT added again here.

-- Reset-token store.
-- Only the SHA-256 hash of the raw token is stored — never the raw token itself.
CREATE TABLE password_reset_tokens (
    id           UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_id  UUID         NOT NULL,
    token_hash   VARCHAR(64)  NOT NULL,
    expires_at   TIMESTAMPTZ  NOT NULL,
    used         BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT fk_prt_customer
        FOREIGN KEY (customer_id) REFERENCES customers (id) ON DELETE CASCADE,
    CONSTRAINT uq_prt_token_hash UNIQUE (token_hash)
);

CREATE INDEX idx_prt_customer_id ON password_reset_tokens (customer_id);
