## US-018: Fraud Analyst — Unblock or Reject a Transaction

**Epic:** EPIC-04
**Status:** 🔵 BACKLOG
**Tags:** `[UI]` `[API]` `[DB]` `[Messaging]`

**As a** Fraud Analyst,
**I want to** unblock a legitimate transaction that was incorrectly flagged, or permanently reject a truly fraudulent one,
**So that** customers receive their funds when appropriate and fraudulent transfers are permanently stopped.

**Acceptance Criteria:**
- [ ] AC1: Fraud Analyst can call `POST /api/v1/fraud/alerts/{alertId}/approve` to unblock a `BLOCKED` transaction; this triggers the fund transfer (debit source, credit destination) and sets the transaction `status` to `COMPLETED`
- [ ] AC2: On successful unblock, the `FraudAlert.reviewStatus` is set to `REVIEWED` with `reviewedBy` (analyst user ID) and `reviewedAt` timestamp persisted
- [ ] AC3: If the source account has insufficient balance at the time of unblock (e.g., funds were used elsewhere), the API returns 422 with message `"Insufficient funds at time of unblock"`
- [ ] AC4: Fraud Analyst can call `POST /api/v1/fraud/alerts/{alertId}/reject` to permanently reject a `BLOCKED` transaction; transaction `status` is set to `REJECTED` and no funds are moved
- [ ] AC5: On rejection, the `FraudAlert.reviewStatus` is set to `REVIEWED` with `reviewedBy` and `reviewedAt` persisted
- [ ] AC6: Attempting to approve or reject an alert whose transaction is already `COMPLETED` or `REJECTED` returns 409 with message `"Transaction is already resolved"`
- [ ] AC7: Non-fraud-analyst users receive 403

**Out of Scope:** Customer-initiated appeal flow, partial fund release, bulk approve/reject

**Dependencies:** US-015, US-017
