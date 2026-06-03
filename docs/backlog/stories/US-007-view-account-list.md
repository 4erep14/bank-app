## US-007: View Account List & Balances

**Epic:** EPIC-02
**Status:** 🔵 BACKLOG
**Tags:** `[UI]` `[API]`

**As a** logged-in customer,
**I want to** see all my accounts with their current balances,
**So that** I can get a quick overview of my financial position.

**Acceptance Criteria:**
- [ ] AC1: Authenticated customer can call `GET /api/v1/accounts` and receive a list of their accounts
- [ ] AC2: Each item in the list includes: `accountNumber`, `type`, `balance`, `status`
- [ ] AC3: Only accounts belonging to the authenticated customer are returned
- [ ] AC4: If the customer has no accounts, the API returns 200 with an empty array `[]`
- [ ] AC5: Balances are always returned formatted to 2 decimal places
- [ ] AC6: Unauthenticated requests return 401

**Out of Scope:** Balance history, scheduled/projected balance, interest calculations

**Dependencies:** US-006
