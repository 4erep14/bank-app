## US-013: Admin — View All Transactions

**Epic:** EPIC-03
**Status:** 🔵 BACKLOG
**Tags:** `[UI]` `[API]`

**As a** Bank Admin,
**I want to** view all transactions across all accounts on the platform,
**So that** I can monitor system-wide activity and support investigations.

**Acceptance Criteria:**
- [ ] AC1: Admin can call `GET /api/v1/admin/transactions` to receive a paginated list of all transactions; default page size is 20
- [ ] AC2: Admin can filter by: `accountId`, `customerId`, `dateFrom`, `dateTo`, `status`, `amountMin`, `amountMax`
- [ ] AC3: Each record includes: `transactionId`, `sourceAccountNumber`, `destinationAccountNumber`, `amount`, `status`, `timestamp`, `fraudFlagged`
- [ ] AC4: Results are ordered by `timestamp` descending by default
- [ ] AC5: Non-admin users receive 403

**Out of Scope:** Transaction reversal, manual status changes by admin, bulk exports

**Dependencies:** US-010
