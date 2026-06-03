## US-014: Define & Manage Fraud Detection Rules

**Epic:** EPIC-04
**Status:** 🔵 BACKLOG
**Tags:** `[UI]` `[API]` `[DB]`

**As a** Fraud Analyst,
**I want to** create, update, and manage fraud detection rules,
**So that** the system knows which transaction patterns to automatically flag and block.

**Acceptance Criteria:**
- [ ] AC1: Fraud Analyst can call `POST /api/v1/fraud/rules` to create a rule with: `name` (unique string), `conditionType` (enum: `AMOUNT_EXCEEDS`, `TRANSACTION_FREQUENCY`, `UNUSUAL_HOUR`), `thresholdValue` (decimal for amount/frequency, or hour range string `"HH:mm-HH:mm"` for UNUSUAL_HOUR), `active` (boolean)
- [ ] AC2: Fraud Analyst can call `GET /api/v1/fraud/rules` to list all rules (both active and inactive), paginated with default page size 20
- [ ] AC3: Fraud Analyst can call `PATCH /api/v1/fraud/rules/{id}` to update `name`, `thresholdValue`, or `active` status
- [ ] AC4: Fraud Analyst can call `DELETE /api/v1/fraud/rules/{id}` to soft-delete a rule (sets status to `DELETED`; the rule is excluded from evaluation)
- [ ] AC5: Attempting to delete the last remaining active rule returns 409 with message `"At least one active rule must remain"`
- [ ] AC6: Rule name must be unique; duplicate name returns 409 with message `"Rule name already exists"`
- [ ] AC7: Non-fraud-analyst users calling any `/fraud/rules` endpoint receive 403

**Out of Scope:** ML-based rules, rule versioning/history, rule scheduling, rule testing/simulation

**Dependencies:** None
