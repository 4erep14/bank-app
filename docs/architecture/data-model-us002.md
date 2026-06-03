# Data Model — US-002: Customer Login

**Story:** US-002
**Related ADR:** [ADR-002-customer-login.md](./ADR-002-customer-login.md)
**Baseline:** US-001 `V1__create_customers_table.sql` (the `customers` table already exists)

This story is **additive** to the existing `customers` table — no new tables.

---

## V2 Migration

**File:** `backend/src/main/resources/db/migration/V2__add_login_lockout_columns.sql`

```sql
-- V2__add_login_lockout_columns.sql
-- Story: US-002 — Customer Login (account lockout support)

-- 1) Lockout tracking columns on the existing customers table
ALTER TABLE customers
    ADD COLUMN failed_login_attempts INT NOT NULL DEFAULT 0,
    ADD COLUMN locked_at             TIMESTAMP NULL;

-- 2) Allow the new LOCKED status value.
ALTER TABLE customers
    DROP CONSTRAINT chk_customers_status;

ALTER TABLE customers
    ADD CONSTRAINT chk_customers_status
        CHECK (status IN (
            'PENDING_VERIFICATION',
            'ACTIVE',
            'SUSPENDED',
            'CLOSED',
            'LOCKED'
        ));
```

---

## Updated `customers` Table — ERD

```
TABLE customers (post-V2)
  id                     UUID         PRIMARY KEY  DEFAULT gen_random_uuid()
  first_name             VARCHAR(100) NOT NULL
  last_name              VARCHAR(100) NOT NULL
  email                  VARCHAR(255) NOT NULL      UNIQUE
  phone_number           VARCHAR(20)  NOT NULL
  date_of_birth          DATE         NOT NULL
  password_hash          VARCHAR(255) NOT NULL
  status                 VARCHAR(30)  NOT NULL      DEFAULT 'PENDING_VERIFICATION'
                                                    CHECK IN (PENDING_VERIFICATION, ACTIVE,
                                                     SUSPENDED, CLOSED, LOCKED)   [+LOCKED in V2]
  failed_login_attempts  INT          NOT NULL      DEFAULT 0                      [+V2]
  locked_at              TIMESTAMP    NULL                                         [+V2]
  created_at             TIMESTAMP    NOT NULL      DEFAULT NOW()
  updated_at             TIMESTAMP    NOT NULL      DEFAULT NOW()
```

---

## CustomerStatus Enum (updated)

```
PENDING_VERIFICATION   -- US-001 initial state
ACTIVE
SUSPENDED
CLOSED
LOCKED                 -- (+) US-002: set after 5 consecutive failed login attempts (AC4)
```

---

## State Transitions

```
ACTIVE ──(5th consecutive failed login)──► LOCKED  [locked_at = now()]
ACTIVE ──(successful credential check)───► ACTIVE  [failed_login_attempts reset to 0]
LOCKED ──(login attempt)─────────────────► LOCKED (423) [password not checked]
```