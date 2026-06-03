## US-020: Admin — View System Audit Log

**Epic:** EPIC-05
**Status:** 🔵 BACKLOG
**Tags:** `[UI]` `[API]` `[DB]`

**As a** Bank Admin,
**I want to** view a chronological, tamper-proof audit log of all significant system actions,
**So that** I can investigate security incidents and demonstrate compliance.

**Acceptance Criteria:**
- [ ] AC1: Admin can call `GET /api/v1/admin/audit-logs` to retrieve a paginated list of audit entries; default page size is 50
- [ ] AC2: Each entry includes: `id`, `actorId`, `actorRole`, `actionType`, `targetEntityType`, `targetEntityId`, `timestamp`, `ipAddress`
- [ ] AC3: The following action types are captured automatically by the system:
  - `LOGIN_SUCCESS`, `LOGIN_FAILURE`, `LOGOUT`
  - `ACCOUNT_OPENED`, `ACCOUNT_DEACTIVATED`, `ACCOUNT_ACTIVATED`
  - `TRANSFER_SUBMITTED`, `TRANSFER_BLOCKED`, `TRANSFER_COMPLETED`
  - `FRAUD_RULE_CREATED`, `FRAUD_RULE_UPDATED`, `FRAUD_RULE_DELETED`
  - `TRANSACTION_UNBLOCKED`, `TRANSACTION_REJECTED`
  - `CUSTOMER_DEACTIVATED`, `CUSTOMER_UNLOCKED`
- [ ] AC4: Admin can filter by `actorId`, `actionType`, `dateFrom`, `dateTo`
- [ ] AC5: Audit log entries are immutable — no `PUT`, `PATCH`, or `DELETE` endpoints exist for audit logs; any such request returns 405
- [ ] AC6: Non-admin users calling `/admin/audit-logs` receive 403

**Out of Scope:** Log export to file, SIEM/Splunk integration, real-time alerting on log events

**Dependencies:** US-001

**Notes / Open Questions:**
- Assumption: Audit entries are written via an internal cross-cutting mechanism (e.g., Spring AOP or event listeners) — no manual logging by individual service methods.
