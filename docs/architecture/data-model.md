# Data Model — US-001: Customer Self-Registration

## ERD — US-001

```
TABLE customers
  id             UUID         PRIMARY KEY  DEFAULT gen_random_uuid()
  first_name     VARCHAR(100) NOT NULL
  last_name      VARCHAR(100) NOT NULL
  email          VARCHAR(255) NOT NULL  UNIQUE            -- uq_customers_email (AC2)
  phone_number   VARCHAR(20)  NOT NULL                    -- E.164 string (AC4)
  date_of_birth  DATE         NOT NULL                    -- @Past
  password_hash  VARCHAR(72)  NOT NULL                    -- BCrypt hash only, never plaintext
  status         VARCHAR(30)  NOT NULL  DEFAULT 'PENDING_VERIFICATION'  -- CustomerStatus (AC5)
  created_at     TIMESTAMPTZ  NOT NULL  DEFAULT now()
  updated_at     TIMESTAMPTZ  NOT NULL  DEFAULT now()

CONSTRAINTS
  PK            : id
  UNIQUE        : uq_customers_email (email)
  CHECK         : chk_customers_status
                  status IN ('PENDING_VERIFICATION','ACTIVE','SUSPENDED','CLOSED')

INDEXES
  uq_customers_email : UNIQUE B-tree on (email)  -- enforces AC2, serves email lookups
```

## Column Reference

| Column | Type | Nullable | Default | Constraint / Rule | AC |
|--------|------|----------|---------|-------------------|----|
| `id` | UUID | No | `gen_random_uuid()` | Primary key; returned to client | AC6 |
| `first_name` | VARCHAR(100) | No | — | `@NotBlank`, ≤100 | AC1 |
| `last_name` | VARCHAR(100) | No | — | `@NotBlank`, ≤100 | AC1 |
| `email` | VARCHAR(255) | No | — | `@NotBlank`/`@Email`, ≤255, UNIQUE, lower-cased | AC1, AC2 |
| `phone_number` | VARCHAR(20) | No | — | E.164 `^\+[1-9]\d{1,14}$` | AC1, AC4 |
| `date_of_birth` | DATE | No | — | `@NotNull`, `@Past` | AC1 |
| `password_hash` | VARCHAR(72) | No | — | BCrypt(strength 12); plaintext never stored | AC1 |
| `status` | VARCHAR(30) | No | `PENDING_VERIFICATION` | CHECK enum | AC5 |
| `created_at` | TIMESTAMPTZ | No | `now()` | Audit | — |
| `updated_at` | TIMESTAMPTZ | No | `now()` | Audit | — |

## Enum — CustomerStatus

| Value | Meaning | Set by US-001? |
|-------|---------|----------------|
| `PENDING_VERIFICATION` | Registered, awaiting email verification | ✅ initial (AC5) |
| `ACTIVE` | Verified and active | No (future story) |
| `SUSPENDED` | Temporarily disabled | No |
| `CLOSED` | Account closed | No |

## Relationships

US-001 introduces a single, standalone aggregate (`customers`). No foreign keys yet. Future stories (accounts, verification tokens, auth sessions) will reference `customers.id`.

## Notes

- `email` is normalized to lower-case before persistence so uniqueness is case-insensitive in practice.
- PII columns (`first_name`, `last_name`, `email`, `phone_number`, `date_of_birth`) require encryption-at-rest and a retention policy at the platform level.
- `password_hash` and any password material must be excluded from logs and API responses.
