## US-019: Admin — Manage Customer User Accounts

**Epic:** EPIC-05
**Status:** 🔵 BACKLOG
**Tags:** `[UI]` `[API]`

**As a** Bank Admin,
**I want to** view, deactivate, and unlock customer user accounts,
**So that** I can enforce platform policies and assist customers locked out of their accounts.

**Acceptance Criteria:**
- [ ] AC1: Admin can call `GET /api/v1/admin/customers` to retrieve a paginated list of all customers; default page size is 20; each record includes: `id`, `fullName`, `email`, `phone`, `status`, `createdAt`
- [ ] AC2: Admin can filter customers by `status` (`ACTIVE`, `LOCKED`, `INACTIVE`)
- [ ] AC3: Admin can call `PATCH /api/v1/admin/customers/{id}/deactivate` to set a customer's status to `INACTIVE`; all active JWT sessions for that customer are immediately invalidated
- [ ] AC4: Admin can call `PATCH /api/v1/admin/customers/{id}/unlock` to set a `LOCKED` customer (locked via US-002 AC4) back to `ACTIVE`
- [ ] AC5: An Admin cannot deactivate another Admin account; attempting to do so returns 403 with message `"Cannot deactivate an admin account"`
- [ ] AC6: Non-admin users calling any `/admin/customers` endpoint receive 403

**Out of Scope:** Customer data export, GDPR deletion requests, bulk operations, admin account management

**Dependencies:** US-001
