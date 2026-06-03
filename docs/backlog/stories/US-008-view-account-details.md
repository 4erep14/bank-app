## US-008: View Account Details

**Epic:** EPIC-02
**Status:** 🔵 BACKLOG
**Tags:** `[UI]` `[API]`

**As a** logged-in customer,
**I want to** view the full details of a specific account,
**So that** I have complete information about that account at hand.

**Acceptance Criteria:**
- [ ] AC1: Authenticated customer can call `GET /api/v1/accounts/{id}` to retrieve details of a specific account
- [ ] AC2: Response includes: `accountNumber`, `type`, `balance`, `status`, `createdAt`
- [ ] AC3: If the account belongs to a different customer, the API returns 403
- [ ] AC4: If the account ID does not exist, the API returns 404
- [ ] AC5: Unauthenticated requests return 401

**Out of Scope:** Account statements, PDF export, interest rate information

**Dependencies:** US-006
