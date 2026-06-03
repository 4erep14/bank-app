## US-017: Fraud Analyst — Review Blocked Transactions

**Epic:** EPIC-04
**Status:** 🔵 BACKLOG
**Tags:** `[UI]` `[API]`

**As a** Fraud Analyst,
**I want to** view all fraud alerts with their associated transaction details,
**So that** I can investigate each case and decide whether to unblock or permanently reject it.

**Acceptance Criteria:**
- [ ] AC1: Fraud Analyst can call `GET /api/v1/fraud/alerts` to retrieve a paginated list of all fraud alerts; default page size is 20
- [ ] AC2: Each alert record includes: `alertId`, `transactionId`, `amount`, `customerFullName`, `accountNumber`, `triggeredRuleName`, `ruleConditionType`, `timestamp`, `reviewStatus` (`PENDING_REVIEW` or `REVIEWED`)
- [ ] AC3: Analyst can filter by `dateFrom`, `dateTo`, `reviewStatus`, and `ruleConditionType`
- [ ] AC4: Fraud Analyst can call `GET /api/v1/fraud/alerts/{alertId}` to retrieve full detail of a single alert including the complete transaction record
- [ ] AC5: Non-fraud-analyst users receive 403 on all `/fraud/alerts` endpoints

**Out of Scope:** Bulk review, SLA tracking, alert assignment to specific analysts

**Dependencies:** US-015
