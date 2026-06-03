## US-015: Auto-Detect and Block Fraudulent Transactions

**Epic:** EPIC-04
**Status:** 🔵 BACKLOG
**Tags:** `[API]` `[DB]` `[Messaging]`

**As** the fraud detection system,
**I want to** evaluate every incoming transaction against all active rules in real time,
**So that** suspicious transactions are automatically blocked before funds are moved.

**Acceptance Criteria:**
- [ ] AC1: Every transaction submitted via US-010 publishes a fraud-evaluation event; the Fraud Detection Service consumes this event synchronously before the transfer completes
- [ ] AC2: If the transaction `amount` exceeds the threshold of any active `AMOUNT_EXCEEDS` rule, the transaction is flagged
- [ ] AC3: If the originating customer has submitted more transactions within the last 60 seconds than the threshold of any active `TRANSACTION_FREQUENCY` rule, the transaction is flagged
- [ ] AC4: If the transaction timestamp falls within the hour range of any active `UNUSUAL_HOUR` rule, the transaction is flagged
- [ ] AC5: A flagged transaction has its `status` set to `BLOCKED`; funds are **not** debited or credited
- [ ] AC6: A `FraudAlert` record is created for every blocked transaction containing: `transactionId`, `ruleName`, `ruleConditionType`, `thresholdValue`, `actualValue`, `timestamp`, `reviewStatus = PENDING_REVIEW`
- [ ] AC7: A transaction matching no active rules proceeds with `status = COMPLETED` and funds are transferred normally

**Out of Scope:** Manual review at detection time (US-017/US-018), ML-based scoring, customer-facing dispute

**Dependencies:** US-010, US-014
