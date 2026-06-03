## US-004: Password Reset

**Epic:** EPIC-01
**Status:** ✅ DONE
**Tags:** `[UI]` `[API]` `[DB]`

**As a** customer who has forgotten their password,
**I want to** reset my password via a link sent to my registered email,
**So that** I can regain access to my account without contacting support.

**Acceptance Criteria:**
- [x] AC1: Customer submits `POST /api/v1/auth/forgot-password` with their email; the API always returns 200 regardless of whether the email exists (prevents user enumeration)
- [x] AC2: If the email exists, a password-reset token is generated, stored with a 1-hour expiry, and the reset link is sent to the email (email delivery is stubbed in this iteration)
- [x] AC3: Customer submits `POST /api/v1/auth/reset-password` with the token and a new password; the new password must meet the same complexity rules as US-001 AC3
- [x] AC4: On a valid, unexpired token and compliant password, the password is updated and the API returns 200
- [x] AC5: On successful reset, all active sessions (JWT/refresh tokens) for that customer are invalidated
- [x] AC6: An expired or already-used token returns 400 with message `"Invalid or expired reset token"`
- [x] AC7: Each reset token is single-use; reusing a consumed token returns 400

**Out of Scope:** SMS-based reset, admin-forced password reset, security questions

**Dependencies:** US-001

**Notes / Open Questions:**
- Assumption: Email delivery will be stubbed (no-op log) until an email provider is integrated.
