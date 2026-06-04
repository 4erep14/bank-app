-- Story: US-003
-- Align OTP and refresh-token string columns with Hibernate String/VARCHAR validation.

ALTER TABLE otp_sessions
    ALTER COLUMN session_token_hash TYPE VARCHAR(64),
    ALTER COLUMN otp_code TYPE VARCHAR(6);

ALTER TABLE refresh_tokens
    ALTER COLUMN token_hash TYPE VARCHAR(64);