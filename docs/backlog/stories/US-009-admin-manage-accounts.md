## US-009: Admin — View & Manage Customer Accounts

**Epic:** EPIC-02
**Status:** 🔵 BACKLOG
**Tags:** `[UI]` `[API]`

**As a** Bank Admin,
**I want to** view and manage all customer accounts across the platform,
**So that** I can oversee the account portfolio and take corrective action when needed.

**Acceptance Criteria:**
- [ ] AC1: Admin can call `GET /api/v1/admin/accounts` to receive a paginated list of all accounts; default page size is 20
- [ ] AC2: Each record includes: `accountNumber`, `type`, `balance`, `status`, `ownerFullName`, `ownerEmail`
- [ ] AC3: Admin can filter by `customerId`, `type` (`CHECKING`/`SAVINGS`), and `status` (`ACTIVE`/`INACTIVE`)
- [ ] AC4: Admin can call `PATCH /api/v1/admin/accounts/{id}/deactivate` to set an `ACTIVE` account to `INACTIVE`
- [ ] AC5: Admin can call `PATCH /api/v1/admin/accounts/{id}/activate` to set an `INACTIVE` account back to `ACTIVE`
- [ ] AC6: Non-admin users calling any `/admin/` endpoint receive 403

**Out of Scope:** Account deletion, balance adjustments, bulk operations, account closure workflow

**Dependencies:** US-006
