## US-003: Two-Factor Authentication via SMS

**Epic:** EPIC-01
**Status:** ✅ DONE
**Tags:** `[UI]` `[API]`

**As a** customer who has passed the password check,
**I want to** verify my identity with a one-time SMS code,
**So that** my account is protected against unauthorized access even if my password is compromised.

**Acceptance Criteria:**
- [x] AC1: Upon successful password verification (US-002), a 6-digit OTP is generated and "sent" to the customer's registered phone number (SMS delivery is stubbed in this iteration)
- [x] AC2: The OTP expires after 5 minutes; a request to verify an expired OTP returns 401 with message `"Invalid or expired OTP"`
- [x] AC3: On valid OTP submission via `POST /api/v1/auth/verify-otp`, the system returns 200 with a JWT access token (15-minute expiry) and a refresh token (7-day expiry)
- [x] AC4: On invalid OTP, the API returns 401 with message `"Invalid or expired OTP"`
- [x] AC5: After 3 consecutive invalid OTP attempts for the same session, the session is invalidated and the customer must restart login from US-002

**Out of Scope:** TOTP authenticator apps, email OTP, real SMS provider integration

**Dependencies:** US-002

**Notes / Open Questions:**
- Assumption: SMS sending will be implemented as a no-op stub (logs the OTP) until an SMS provider is integrated in a future epic.
