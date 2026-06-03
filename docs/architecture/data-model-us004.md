# Data Model — US-004: Password Reset

Builds on US-001 (`customers`). US-002 owns Flyway **V2**; this story owns **V3**.

---

## ERD — US-004

```
TABLE customers                         -- existing (V1); V3 ADDS one column
  id                   UUID         PRIMARY KEY
  ...                                                    -- (unchanged US-001 columns)
  password_changed_at  TIMESTAMPTZ  NULL                 -- NEW (V3): session-invalidation cutoff (AC5)

TABLE password_reset_tokens             -- NEW (V3)
  id            UUID         PRIMARY KEY  DEFAULT gen_random_uuid()
  customer_id   UUID         NOT NULL     FK → customers.id  ON DELETE CASCADE
  token_hash    VARCHAR(64)  NOT NULL     UNIQUE             -- SHA-256 hex of raw token
  expires_at    TIMESTAMPTZ  NOT NULL                        -- created_at + 1 hour (AC2)
  used          BOOLEAN      NOT NULL     DEFAULT FALSE       -- single-use flag (AC7)
  created_at    TIMESTAMPTZ  NOT NULL     DEFAULT now()

RELATIONSHIP
  customers (1) ──< (N) password_reset_tokens
```

---

## Flyway Migration — `V3__password_reset.sql`

```sql
-- V3__password_reset.sql
-- Story: US-004 — Password Reset
-- Depends on: V1 (customers). V2 is owned by US-002.

-- 1) Session-invalidation cutoff (AC5)
ALTER TABLE customers
    ADD COLUMN password_changed_at TIMESTAMPTZ NULL;

-- 2) Reset-token store (hash only, never raw token)
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
```

---

## Token Lifecycle

```
[issue]   POST /api/v1/auth/forgot-password
            raw  = Base64URL(SecureRandom 32 bytes)    (43 chars)
            hash = SHA-256(raw)                        (stored as token_hash)
            row: used=false, expires_at = now()+1h
            email link: /reset-password?token=<raw>

[verify]  POST /api/v1/auth/reset-password
            incomingHash = SHA-256(request.token)
            lookup by token_hash = incomingHash
              ├─ no row            → 400 "Invalid or expired reset token"
              ├─ used = true       → 400 "Invalid or expired reset token"
              ├─ expires_at < now()→ 400 "Invalid or expired reset token"
              └─ valid → (one @Transactional):
                         customers.password_hash       = BCrypt(newPassword)
                         customers.password_changed_at = now()   -- invalidates all JWTs (AC5)
                         password_reset_tokens.used    = true    -- single use (AC7)
```

---

## Notes

- SHA-256 (unsalted) is appropriate for high-entropy `SecureRandom` tokens — do **not** substitute BCrypt here.
- `password_changed_at` is consumed by the US-002 JWT filter (`reject if iat < password_changed_at`).
- Never log raw `token`, `token_hash`, `newPassword`, or `password_hash`.