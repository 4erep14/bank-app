## US-011: View Transaction History

**Epic:** EPIC-03
**Status:** 🔵 BACKLOG
**Tags:** `[UI]` `[API]`

**As a** logged-in customer,
**I want to** view the paginated transaction history for one of my accounts,
**So that** I can track all movements and their outcomes.

**Acceptance Criteria:**
- [ ] AC1: Authenticated customer can call `GET /api/v1/accounts/{accountId}/transactions` to retrieve paginated history; default page size is 20
- [ ] AC2: Only transactions on accounts owned by the authenticated customer are accessible; other accounts return 403
- [ ] AC3: Each transaction record includes: `id`, `type`, `amount`, `sourceAccountNumber`, `destinationAccountNumber`, `status`, `timestamp`, `description`
- [ ] AC4: Customer can filter by `dateFrom` and `dateTo` (ISO-8601 date strings)
- [ ] AC5: Customer can filter by `status` (`COMPLETED`, `BLOCKED`, `PENDING`)
- [ ] AC6: Results are ordered by `timestamp` descending by default
- [ ] AC7: Unauthenticated requests return 401

**Out of Scope:** CSV/PDF export, account statements, analytics, spending categories

**Dependencies:** US-010
