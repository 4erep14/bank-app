## US-002: Customer Login

**Epic:** EPIC-01
**Status:** ✅ DONE
**Tags:** `[UI]` `[API]`

**As a** registered customer,
**I want to** log in with my email and password,
**So that** I can begin the authentication flow to access my dashboard.

**Acceptance Criteria:**
- [x] AC1: Customer can submit login with email and password via `POST /api/v1/auth/login`
- [x] AC2: On valid credentials, the system returns 200 with body `{ "status": "2FA_REQUIRED", "sessionToken": "<temp-token>" }` to initiate the 2FA step
- [x] AC3: On invalid credentials, the API returns 401 with message `"Invalid email or password"`
- [x] AC4: After 5 consecutive failed login attempts, the customer account status is set to `LOCKED` and the API returns 423 with message `"Account locked due to too many failed attempts"`
- [x] AC5: A `LOCKED` account cannot log in; it must be unlocked by a Bank Admin (US-019)

**Out of Scope:** Remember-me functionality, SSO, biometric login, session management UI

**Dependencies:** US-001

**Notes / Open Questions:**
- Assumption: The temporary `sessionToken` returned in AC2 is short-lived (5 minutes) and used solely to correlate the 2FA OTP submission.
