## US-001: Customer Self-Registration

**Epic:** EPIC-01
**Status:** ✅ DONE
**Tags:** `[UI]` `[API]` `[DB]`

**As a** new customer,
**I want to** register an account with my personal details,
**So that** I can access the banking platform.

**Acceptance Criteria:**
- [x] AC1: Customer can submit a registration form with: first name, last name, email, phone number, date of birth, and password
- [x] AC2: If email is already registered, the API returns 409 Conflict with message `"Email already registered"`
- [x] AC3: Password must be ≥ 8 characters and contain at least one uppercase letter, one lowercase letter, one digit, and one special character; violations return 400 with field-level error messages
- [x] AC4: Phone number must match E.164 format; invalid format returns 400 with field-level error
- [x] AC5: On successful registration, a Customer record is persisted with status `PENDING_VERIFICATION`
- [x] AC6: On successful registration, the API returns 201 with the new customer's `id`

**Out of Scope:** Email verification flow, admin-created accounts, OAuth2 registration

**Dependencies:** None

**Notes / Open Questions:**
- Assumption: `PENDING_VERIFICATION` status does not block login in this iteration; full email verification is a future story.
