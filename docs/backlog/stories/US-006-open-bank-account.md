## US-006: Open a New Bank Account

**Epic:** EPIC-02
**Status:** 🔵 BACKLOG
**Tags:** `[UI]` `[API]` `[DB]`

**As a** logged-in customer,
**I want to** open a new Checking or Savings account,
**So that** I can manage my finances through the bank.

**Acceptance Criteria:**
- [ ] AC1: Authenticated customer can call `POST /api/v1/accounts` with account type (`CHECKING` or `SAVINGS`)
- [ ] AC2: A unique 10-digit numeric account number is generated and stored
- [ ] AC3: The new account is created with `balance = 0.00` and `status = ACTIVE`
- [ ] AC4: The API returns 201 with: `id`, `accountNumber`, `type`, `balance`, `status`, `createdAt`
- [ ] AC5: A customer may hold at most one `CHECKING` and one `SAVINGS` account; attempting to open a duplicate type returns 409 with message `"Account of this type already exists"`
- [ ] AC6: Unauthenticated requests return 401

**Out of Scope:** Credit accounts, loans, joint accounts, initial deposit at account opening, account closure

**Dependencies:** US-001, US-003
