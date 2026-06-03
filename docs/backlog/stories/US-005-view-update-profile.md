## US-005: View & Update Customer Profile

**Epic:** EPIC-01
**Status:** ✅ DONE
**Tags:** `[UI]` `[API]` `[DB]`

**As a** logged-in customer,
**I want to** view and update my personal profile details,
**So that** my information stays current and accurate.

**Acceptance Criteria:**
- [x] AC1: Authenticated customer can call `GET /api/v1/profile` and receive: first name, last name, email, phone number, date of birth
- [x] AC2: Customer can call `PATCH /api/v1/profile` to update first name, last name, and phone number
- [x] AC3: Email and date of birth are read-only; any attempt to update them returns 400 with message `"Field is not editable"`
- [x] AC4: Updated phone number must pass E.164 format validation; invalid format returns 400 with field-level error
- [x] AC5: Profile changes are persisted and returned correctly in the next `GET /api/v1/profile` call
- [x] AC6: Unauthenticated requests to either endpoint return 401

**Out of Scope:** Profile photo upload, address management, KYC document upload, email-change flow

**Dependencies:** US-001, US-003
