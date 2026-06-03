-- V4__otp_and_refresh_tokens.sql
-- Story: US-003 — Two-Factor Authentication via SMS
-- Depends on: V1 (customers), V2 (login lockout), V3 (password reset)

-- OTP session store.
-- session_token_hash: SHA-256 hex of the raw SESSION JWT — never the raw token itself.
-- Correlates the one-time code with the ADR-002 sessionToken.
CREATE TABLE otp_sessions (
    id                  UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_id         UUID        NOT NULL REFERENCES customers(id) ON DELETE CASCADE,
    session_token_hash  CHAR(64)    NOT NULL,
    otp_code            CHAR(6)     NOT NULL,
    expires_at          TIMESTAMPTZ NOT NULL,
    failed_attempts     INT         NOT NULL DEFAULT 0,
    invalidated         BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_otp_sessions_token_hash UNIQUE (session_token_hash)
);

CREATE INDEX idx_otp_sessions_customer_id ON otp_sessions (customer_id);

-- Refresh token store.
-- token_hash: SHA-256 hex of the raw opaque refresh token — never the raw token itself.
CREATE TABLE refresh_tokens (
    id           UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_id  UUID        NOT NULL REFERENCES customers(id) ON DELETE CASCADE,
    token_hash   CHAR(64)    NOT NULL,
    expires_at   TIMESTAMPTZ NOT NULL,
    revoked      BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_refresh_tokens_token_hash UNIQUE (token_hash)
);

CREATE INDEX idx_refresh_tokens_customer_id ON refresh_tokens (customer_id);

COMMENT ON TABLE otp_sessions  IS 'Short-lived OTP sessions for the 2FA SMS verification step (US-003).';
COMMENT ON TABLE refresh_tokens IS 'Long-lived opaque refresh tokens issued on successful 2FA completion (US-003).';
