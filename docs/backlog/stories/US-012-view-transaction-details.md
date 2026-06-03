## US-012: View Transaction Details

**Epic:** EPIC-03
**Status:** 🔵 BACKLOG
**Tags:** `[UI]` `[API]`

**As a** logged-in customer,
**I want to** view the full details of a specific transaction,
**So that** I have a complete, auditable record of that movement.

**Acceptance Criteria:**
- [ ] AC1: Authenticated customer can call `GET /api/v1/transactions/{id}` to retrieve a transaction
- [ ] AC2: The transaction must be associated with an account owned by the authenticated customer; otherwise return 403
- [ ] AC3: Response includes: `id`, `type`, `amount`, `sourceAccountNumber`, `destinationAccountNumber`, `status`, `timestamp`, `description`, `fraudFlagged` (boolean)
- [ ] AC4: Non-existent transaction ID returns 404
- [ ] AC5: Unauthenticated requests return 401

**Out of Scope:** Transaction dispute flow, chargeback initiation

**Dependencies:** US-010
