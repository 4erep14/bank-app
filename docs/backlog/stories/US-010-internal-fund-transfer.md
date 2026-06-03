## US-010: Initiate Internal Fund Transfer

**Epic:** EPIC-03
**Status:** 🔵 BACKLOG
**Tags:** `[UI]` `[API]` `[DB]` `[Messaging]`

**As a** logged-in customer,
**I want to** transfer funds between my own accounts,
**So that** I can move money as my needs require.

**Acceptance Criteria:**
- [ ] AC1: Authenticated customer can call `POST /api/v1/transactions/transfer` with: `sourceAccountId`, `destinationAccountId`, `amount` (positive decimal), and optional `description`
- [ ] AC2: Both source and destination accounts must belong to the authenticated customer; otherwise return 403
- [ ] AC3: Source and destination accounts must be different; same-account transfer returns 400 with message `"Source and destination accounts must differ"`
- [ ] AC4: Source account must have sufficient balance; insufficient funds return 422 with message `"Insufficient funds"`
- [ ] AC5: `amount` must be greater than `0.00` and at most `10,000.00`; violations return 400 with field-level errors
- [ ] AC6: On a clean transfer, source balance is debited and destination balance is credited atomically; both changes are visible immediately on subsequent account reads
- [ ] AC7: A transaction record is created with `status = COMPLETED`, `type = TRANSFER`, `timestamp`, `amount`, `sourceAccountId`, `destinationAccountId`, and `description`
- [ ] AC8: The transfer triggers a fraud-evaluation event (consumed by US-015); if flagged, `status` is set to `BLOCKED` and funds are **not** moved
- [ ] AC9: Neither source nor destination may have `status = INACTIVE`; inactive account returns 422 with message `"Account is inactive"`
- [ ] AC10: API returns 201 with `transactionId` and `status`
- [ ] AC11: Unauthenticated requests return 401

**Out of Scope:** Scheduled/recurring transfers, external transfers, batch transfers, transfer cancellation

**Dependencies:** US-006, US-014 (fraud rules must exist before AC8 can evaluate)
