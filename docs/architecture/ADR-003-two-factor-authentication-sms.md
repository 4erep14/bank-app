## ADR-003: Two-Factor Authentication via SMS (OTP)

**Story:** US-003
**Status:** Accepted

---

### Context

US-003 introduces the **second authentication step** of the NorthBank two-factor login flow.
A customer who has already passed password verification (US-002) must submit a 6-digit
one-time passcode (OTP) delivered to their registered phone number. Only on valid OTP
submission does the system issue the full JWT access token (+ refresh token) that authorises
subsequent secured calls.

**What already exists (baseline this ADR builds on):**

| ADR | Key facts |
|-----|-----------|
| ADR-001 (Accepted) | `customers` table (V1), BCrypt encoder bean, RFC 7807 `GlobalExceptionHandler`, package root `com.northbank` |
| ADR-002 (Accepted) | `POST /api/v1/auth/login` → `{ status:"2FA_REQUIRED", sessionToken:"<jwt>" }`; sessionToken is a 5-min HS256 JWT (`sub`=UUID, `type="SESSION"`); `jjwt` 0.12.x; Spring Security STATELESS; V2 migration |
| ADR-004 (Accepted) | `password_reset_tokens` table; `EmailService`/`StubEmailService` pattern; V3 migration; `password_changed_at` on `customers` |

**Next available Flyway version: `V4`.**
V1 = registration, V2 = login lockout, V3 = password reset.

**What this story must decide:**

1. Where to store the OTP and its session context.
2. How to generate the OTP securely.
3. How to correlate the incoming `sessionToken` with the OTP session.
4. How to enforce the 3-attempt lockout.
5. How to stub SMS delivery.
6. How to generate and store access + refresh tokens on success.
7. The full API contract for `verify-otp` and `resend-otp`.

**Conflict check vs existing ADRs:**

- **ADR-001:** No conflict. This story adds new tables via V4; `customers` table unchanged.
- **ADR-002:** One additive dependency (flagged below). The login service must be **extended** to
  create an `otp_sessions` row and call `SmsService` immediately after issuing the
  `sessionToken`. The ADR-002 response contract (`2FA_REQUIRED` + `sessionToken`) is
  **unchanged** — this is a behind-the-scenes addition.
- **ADR-004:** No conflict. V3 is already reserved by password reset; this story uses V4.

> ⚠️ **ADR-002 additive dependency:** `AuthService.login()` must be updated to create
> an `otp_sessions` row and invoke `SmsService.sendOtp()` on successful credential check.
> No ADR-002 endpoints or response shapes change.

---

### Decision

Implement 2FA via two new **synchronous REST** endpoints (`verify-otp`, `resend-otp`) backed
by **PostgreSQL** (two new tables via V4) inside the existing modular monolith. No messaging
or external integrations (SMS delivery is stubbed).

**1. OTP storage → new `otp_sessions` DB table**  
OTP state (code, expiry, attempt counter, invalidation flag) is persisted in a dedicated
`otp_sessions` table. The sessionToken from US-002 is the correlation handle: its raw JWT
string is SHA-256-hashed (`session_token_hash`) and stored as the unique lookup key, mirroring
the hash-at-rest pattern established by ADR-004.

**2. OTP generation → `SecureRandom` 6-digit integer, zero-padded**  
`SecureRandom.nextInt(1_000_000)` produces a uniform distribution over [0, 999 999].
Formatted with `String.format("%06d", n)` to always produce exactly 6 digits.

**3. Session correlation → SHA-256(raw sessionToken JWT string)**  
No changes to the ADR-002 sessionToken structure are required. The raw JWT compact string
received from the client is hashed with SHA-256 to produce the 64-char hex `session_token_hash`
stored in `otp_sessions`. The session is validated (signature + expiry + `type="SESSION"`)
before hashing for lookup.

**4. Lockout (AC5) → `invalidated` boolean on `otp_sessions`**  
After 3 consecutive wrong-OTP attempts the row's `invalidated` column is set to `true`.
All further verify attempts against the same `session_token_hash` return `401`. The client
must restart from US-002 (`POST /api/v1/auth/login`).

**5. SMS delivery → `SmsService` interface + `StubSmsService`**  
Mirrors the ADR-004 `EmailService`/`StubEmailService` pattern exactly. `StubSmsService`
is annotated `@Primary @Profile({"dev","test"})` and logs via SLF4J at `INFO`. A real
implementation (Twilio, AWS SNS, etc.) is out of scope for this iteration.

**6. Access token → HS256 JWT, 15-min, `type="ACCESS"`**  
Same `jjwt` library and `security.jwt.secret` key as ADR-002. `sub`=customer UUID,
`type="ACCESS"`, `iat`/`exp` standard claims.

**7. Refresh token → `SecureRandom` 32 bytes → Base64URL (43 chars), stored hashed (SHA-256)
in new `refresh_tokens` table, 7-day expiry**  
Same generation pattern as ADR-004 reset tokens. The 43-char Base64URL raw token is
returned to the client; only its SHA-256 hash is stored. The token-refresh endpoint
(using this token to obtain a new access token) is out of scope for US-003 but the
table is provisioned now.

**8. `remainingAttempts` extension in 401 `ProblemDetail`**  
All `401` responses from `POST /api/v1/auth/verify-otp` include a `remainingAttempts`
integer extension field. Value = `max(0, 3 − failed_attempts_after_this_call)`. This
allows the UI to render a countdown (e.g., "2 attempts remaining") without exposing
internal state beyond what is necessary.

---

### Persistence — V4 Migration

**Choice: PostgreSQL (SQL).**

| Criterion | Assessment |
|-----------|-----------|
| Structured relational data | ✅ Fixed schema, 1:M with customers |
| ACID transactions required | ✅ Atomic attempt counter + invalidation |
| Auditable (banking) | ✅ Timestamped rows |
| High write throughput | ❌ Low frequency |
| Flexible schema | ❌ Not needed |

#### ERD — US-003 (incremental on top of V1–V3)

```
TABLE customers  (unchanged — exists from V1/V2/V3)
  id  UUID  PK
  ... (see ADR-001, ADR-002, ADR-004)

TABLE otp_sessions                                          [NEW — V4]
  id                  UUID         PK  DEFAULT gen_random_uuid()
  customer_id         UUID         NOT NULL  FK → customers(id)  ON DELETE CASCADE
  session_token_hash  CHAR(64)     NOT NULL  UNIQUE   -- SHA-256 hex of raw JWT string
  otp_code            CHAR(6)      NOT NULL            -- plaintext; 5-min TTL + 3-attempt lockout
  expires_at          TIMESTAMPTZ  NOT NULL            -- created_at + 5 min
  failed_attempts     INT          NOT NULL  DEFAULT 0  -- incremented on wrong OTP
  invalidated         BOOLEAN      NOT NULL  DEFAULT FALSE  -- true after 3 failures or after success
  created_at          TIMESTAMPTZ  NOT NULL  DEFAULT now()  -- also used for resend rate-limit

INDEXES
  PRIMARY KEY (id)
  UNIQUE  idx_otp_sessions_session_token_hash  (session_token_hash)
  B-tree  idx_otp_sessions_customer_id         (customer_id)

TABLE refresh_tokens                                        [NEW — V4]
  id           UUID         PK  DEFAULT gen_random_uuid()
  customer_id  UUID         NOT NULL  FK → customers(id)  ON DELETE CASCADE
  token_hash   CHAR(64)     NOT NULL  UNIQUE   -- SHA-256 hex of raw 43-char Base64URL token
  expires_at   TIMESTAMPTZ  NOT NULL           -- created_at + 7 days
  revoked      BOOLEAN      NOT NULL  DEFAULT FALSE
  created_at   TIMESTAMPTZ  NOT NULL  DEFAULT now()

INDEXES
  PRIMARY KEY (id)
  UNIQUE  idx_refresh_tokens_token_hash   (token_hash)
  B-tree  idx_refresh_tokens_customer_id  (customer_id)
```

#### Flyway V4 DDL

**File:** `backend/src/main/resources/db/migration/V4__create_otp_and_refresh_token_tables.sql`

```sql
-- V4__create_otp_and_refresh_token_tables.sql
-- Story: US-003 — Two-Factor Authentication via SMS

-- ── OTP Sessions ─────────────────────────────────────────────────────────────
-- One row per active login session (correlated via SHA-256 hash of sessionToken JWT).
-- Stores the 6-digit OTP, expiry, attempt counter, and invalidation flag.
CREATE TABLE otp_sessions (
    id                  UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_id         UUID        NOT NULL
                            REFERENCES customers(id) ON DELETE CASCADE,
    session_token_hash  CHAR(64)    NOT NULL,          -- SHA-256(raw sessionToken JWT string)
    otp_code            CHAR(6)     NOT NULL,           -- SecureRandom, zero-padded, 5-min TTL
    expires_at          TIMESTAMPTZ NOT NULL,
    failed_attempts     INT         NOT NULL DEFAULT 0,
    invalidated         BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT uq_otp_sessions_token_hash UNIQUE (session_token_hash)
);

CREATE INDEX idx_otp_sessions_customer_id
    ON otp_sessions (customer_id);

-- ── Refresh Tokens ────────────────────────────────────────────────────────────
-- Stores the SHA-256 hash of the 43-char Base64URL refresh token.
-- Raw token returned to client only at issuance; never stored in plain text.
CREATE TABLE refresh_tokens (
    id           UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_id  UUID        NOT NULL
                     REFERENCES customers(id) ON DELETE CASCADE,
    token_hash   CHAR(64)    NOT NULL,    -- SHA-256(raw Base64URL token)
    expires_at   TIMESTAMPTZ NOT NULL,
    revoked      BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT uq_refresh_tokens_token_hash UNIQUE (token_hash)
);

CREATE INDEX idx_refresh_tokens_customer_id
    ON refresh_tokens (customer_id);
```

> **Why `otp_code` is stored as plaintext (CHAR(6)):**  
> The OTP is 6 decimal digits (10⁶ = 1,000,000 possibilities). SHA-256 of a 6-digit string
> is trivially brute-forceable in microseconds, providing no meaningful protection beyond
> what is already achieved by: (a) 5-minute expiry, (b) 3-attempt lockout, (c) session
> invalidation, and (d) the sessionToken signature gate. Storing SHA-256(OTP) would create
> a false sense of security; plaintext is the honest, auditable choice here. See
> *Alternatives Considered* for the rejected hashing option.

---

### Communication

- **Pattern:** REST (synchronous). **No** RabbitMQ / Kafka.
- **Rationale:** Both endpoints require an immediate response. OTP validation and token
  issuance are low-latency, request/reply operations with no downstream event consumers
  in scope for this story. SMS delivery is fire-and-forget via an injectable service stub.

---

### API Endpoints

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| `POST` | `/api/v1/auth/verify-otp` | None (sessionToken in body) | Validate OTP; issue access + refresh tokens (AC1–AC5) |
| `POST` | `/api/v1/auth/resend-otp` | None (sessionToken in body) | Re-generate OTP and resend via SMS stub; rate-limited |

Both endpoints are **public** (added to Spring Security `permitAll()`). The sessionToken
embeds the customer identity and is signature-validated on every call.

---

#### `POST /api/v1/auth/verify-otp`

**Purpose:** Exchange a valid OTP for a JWT access token + refresh token.

**Request Body (`application/json`):**

| Field | Type | Required | Validation |
|-------|------|----------|-----------|
| `sessionToken` | string | yes | Valid HS256 JWT; `type="SESSION"`; not expired |
| `otp` | string | yes | Exactly 6 digits (`^\d{6}$`) |

```json
POST /api/v1/auth/verify-otp
Content-Type: application/json

{
  "sessionToken": "<session-jwt-from-login>",
  "otp": "482917"
}
```

**200 OK — Valid OTP (AC3):**

```json
HTTP/1.1 200 OK
Content-Type: application/json

{
  "accessToken":  "<hs256-jwt-15min>",
  "refreshToken": "<base64url-43char-raw-token>"
}
```

**401 Unauthorized — Invalid or expired OTP (AC2, AC4):**

```json
HTTP/1.1 401 Unauthorized
Content-Type: application/problem+json

{
  "type":              "https://api.bank.example/problems/invalid-otp",
  "title":             "Invalid OTP",
  "status":            401,
  "detail":            "Invalid or expired OTP",
  "instance":          "/api/v1/auth/verify-otp",
  "remainingAttempts": 2
}
```

> `remainingAttempts` = `max(0, 3 − failed_attempts_after_this_call)`.
> Present on **all** 401 responses from this endpoint, including expiry (AC2) and
> session-invalidated (AC5) cases. Value is `0` when the session is invalidated.

**401 Unauthorized — Invalid sessionToken (malformed / wrong type / expired JWT):**

```json
HTTP/1.1 401 Unauthorized
Content-Type: application/problem+json

{
  "type":   "https://api.bank.example/problems/invalid-session-token",
  "title":  "Invalid Session Token",
  "status": 401,
  "detail": "Invalid or expired session token",
  "instance": "/api/v1/auth/verify-otp"
}
```

---

#### `POST /api/v1/auth/resend-otp`

**Purpose:** Re-generate a new OTP and re-send it via SMS stub.
Rate-limited to **one resend per 60 seconds** per session.

**Request Body (`application/json`):**

| Field | Type | Required | Validation |
|-------|------|----------|-----------|
| `sessionToken` | string | yes | Valid HS256 JWT; `type="SESSION"`; not expired |

```json
POST /api/v1/auth/resend-otp
Content-Type: application/json

{
  "sessionToken": "<session-jwt-from-login>"
}
```

**200 OK — OTP resent:**

```json
HTTP/1.1 200 OK
Content-Type: application/json

{
  "message": "OTP sent"
}
```

**429 Too Many Requests — Rate limit exceeded:**

```json
HTTP/1.1 429 Too Many Requests
Content-Type: application/problem+json
Retry-After: 60

{
  "type":     "https://api.bank.example/problems/rate-limit-exceeded",
  "title":    "Rate Limit Exceeded",
  "status":   429,
  "detail":   "OTP was already sent. Please wait 60 seconds before requesting a new one.",
  "instance": "/api/v1/auth/resend-otp"
}
```

**401 Unauthorized — Session invalidated (AC5) or invalid JWT:**

```json
HTTP/1.1 401 Unauthorized
Content-Type: application/problem+json

{
  "type":   "https://api.bank.example/problems/invalid-session-token",
  "title":  "Invalid Session Token",
  "status": 401,
  "detail": "Session has been invalidated. Please restart the login process.",
  "instance": "/api/v1/auth/resend-otp"
}
```

---

### OTP Lifecycle Design

#### Phase 1 — Generation (triggered by `AuthService.login()` success in US-002)

```
1. Validate customer credentials → success (US-002 flow unchanged)
2. Generate sessionToken (HS256 JWT, 5-min, type=SESSION, sub=customerId)  ← US-002
3. Compute session_token_hash = SHA-256(rawJwtString)
4. Generate OTP:
     otp = String.format("%06d", SecureRandom().nextInt(1_000_000))
5. INSERT INTO otp_sessions (customer_id, session_token_hash, otp_code,
                              expires_at = now()+5min, failed_attempts=0, invalidated=false)
6. Fetch customer.phone_number from customers table
7. smsService.sendOtp(phoneNumber, otp)  ← StubSmsService logs at INFO in dev/test
8. Return { status: "2FA_REQUIRED", sessionToken: <jwt> }  ← unchanged AC from ADR-002
```

#### Phase 2 — Validation (`POST /api/v1/auth/verify-otp`)

```
1.  Parse & validate sessionToken:
      - Signature valid (HS256, security.jwt.secret)?
      - exp > now()?
      - type == "SESSION"?
      → FAIL → 401 "Invalid or expired session token" (no otp_sessions lookup)

2.  Compute session_token_hash = SHA-256(rawSessionToken)

3.  SELECT * FROM otp_sessions WHERE session_token_hash = ?
      → NOT FOUND → 401 "Invalid or expired OTP", remainingAttempts=0

4.  IF invalidated = true
      → 401 "Invalid or expired OTP", remainingAttempts=0

5.  IF expires_at < now()
      → 401 "Invalid or expired OTP", remainingAttempts = max(0, 3-failed_attempts)
        (AC2 — expiry does NOT increment failed_attempts)

6.  Compare request.otp with otp_sessions.otp_code:
      MATCH:
        a. UPDATE otp_sessions SET invalidated=true (prevent replay)
        b. Generate access token:
             - 32B SecureRandom header; sub=customerId, type=ACCESS, exp=now()+15min
             - Sign with HS256, security.jwt.secret
        c. Generate refresh token:
             - rawToken = Base64URL(SecureRandom.nextBytes(32))  → 43 chars
             - tokenHash = SHA-256(rawToken)
             - INSERT INTO refresh_tokens (customer_id, token_hash, expires_at=now()+7days)
        d. Return 200 { accessToken, refreshToken }

      NO MATCH:
        a. failed_attempts = failed_attempts + 1
        b. IF failed_attempts >= 3:
               UPDATE otp_sessions SET failed_attempts=3, invalidated=true
               → 401 "Invalid or expired OTP", remainingAttempts=0   (AC5)
           ELSE:
               UPDATE otp_sessions SET failed_attempts=failed_attempts
               → 401 "Invalid or expired OTP",
                  remainingAttempts = 3 - failed_attempts              (AC4)
```

#### Phase 3 — Resend (`POST /api/v1/auth/resend-otp`)

```
1. Parse & validate sessionToken (same as Phase 2 step 1)
   → FAIL → 401 "Invalid or expired session token"

2. Compute session_token_hash = SHA-256(rawSessionToken)

3. SELECT * FROM otp_sessions WHERE session_token_hash = ?
   → NOT FOUND → 401

4. IF invalidated = true
   → 401 "Session has been invalidated. Please restart the login process."

5. Rate-limit check:
   IF now() - otp_sessions.created_at < 60 seconds
   → 429, Retry-After: 60

6. Generate new OTP (same SecureRandom method)
7. UPDATE otp_sessions SET
       otp_code       = newOtp,
       expires_at     = now() + 5min,
       failed_attempts = 0,           -- reset counter on fresh OTP
       created_at     = now()         -- reset rate-limit window
   (in-place update preserves the UNIQUE session_token_hash)

8. smsService.sendOtp(phoneNumber, newOtp)
9. Return 200 { "message": "OTP sent" }
```

#### State Machine — `otp_sessions.invalidated`

```
[ACTIVE: invalidated=false]
    │
    ├─ correct OTP submitted ──────────────────► [USED: invalidated=true]  (success path)
    │
    ├─ 3rd consecutive wrong OTP ──────────────► [LOCKED: invalidated=true] (AC5)
    │
    ├─ wrong OTP (attempts 1 or 2) ────────────► [ACTIVE] (failed_attempts incremented)
    │
    ├─ OTP expired + resend requested ─────────► [ACTIVE] (row updated, timer reset)
    │
    └─ invalidated=true + verify attempted ────► 401 (AC5)

Note: Expiry (expires_at < now()) does NOT set invalidated. The session remains
      ACTIVE to allow resend-otp to generate a new code without forcing a full
      re-login. Only 3 failed OTP attempts force a restart from US-002.
```

---

### JWT / Refresh Token Design

#### Access Token

| Attribute | Value |
|-----------|-------|
| Library | `io.jsonwebtoken:jjwt` 0.12.x (same as ADR-002) |
| Algorithm | HS256 |
| Signing key | `security.jwt.secret` (same env var as sessionToken) |
| `sub` | Customer UUID (string) |
| `type` | `"ACCESS"` |
| `iat` | Issued-at epoch seconds |
| `exp` | `iat + 900` (15 minutes) |
| Storage | **None** — stateless |

> **US-004 compatibility:** The JWT filter (ADR-002 / ADR-004) must enforce
> `iat >= customers.password_changed_at` for `type="ACCESS"` tokens. This behaviour is
> already planned in ADR-004 (confirmed handoff item). ADR-003 adds no new constraint.

#### Refresh Token

| Attribute | Value |
|-----------|-------|
| Generation | `SecureRandom.nextBytes(32)` → `Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)` → 43 chars |
| Storage | `refresh_tokens.token_hash` = `SHA-256(rawToken)` hex — raw token never stored |
| Expiry | 7 days (`expires_at = now() + 7 days`) |
| Returned to client | Raw 43-char Base64URL string |
| Revocation | `revoked = true` (future story: token-refresh endpoint) |

> **Future story scope:** The endpoint `POST /api/v1/auth/refresh-token` (which accepts a
> refresh token and returns a new access token) is **out of scope for US-003**. The
> `refresh_tokens` table is provisioned now to avoid a schema migration in that story.

---

### SMS Stub Design

The stub follows the identical `interface + @Primary @Profile` pattern established by
ADR-004's `EmailService`.

#### Interface (production contract)

```
package com.northbank.auth.sms

interface SmsService {
    // Sends a one-time passcode to the specified E.164 phone number.
    // Implementations must be non-blocking from the caller's perspective.
    void sendOtp(String toPhoneNumber, String otpCode);
}
```

#### Stub Implementation

```
@Service
@Primary
@Profile({"dev", "test"})
class StubSmsService implements SmsService {
    // Logs the OTP at INFO level via SLF4J.
    // Pattern: "[STUB SMS] OTP for +14155552671 : 482917"
    // ⚠ Never log OTP at INFO or above in a production-profile implementation.
}
```

#### Production implementation placeholder

A production `TwilioSmsService` (or `AwsSnsService`) should be provided in a
`@Profile("prod")` bean, reading credentials exclusively from environment variables
(`SMS_PROVIDER_ACCOUNT_SID`, `SMS_PROVIDER_AUTH_TOKEN`, etc.). This is
**out of scope for this iteration**.

---

### Cross-Cutting Concerns

| Concern | Approach |
|---------|----------|
| **Authentication** | Both endpoints are `permitAll()` (pre-auth). The `sessionToken` JWT (ADR-002 HS256) is the access gate — validated by signature, expiry, and `type="SESSION"` claim before any DB lookup. |
| **Authorization** | None — these are unauthenticated endpoints by design. |
| **Error Handling** | RFC 7807 `ProblemDetail` via existing global `@RestControllerAdvice`. New exception types: `InvalidOtpException` (401), `InvalidSessionTokenException` (401), `OtpRateLimitException` (429). `remainingAttempts` added as ProblemDetail extension property on `InvalidOtpException`. |
| **Logging** | SLF4J structured JSON. OTP codes **must never be logged at INFO or above** outside `StubSmsService`. Correlation ID (customer UUID from JWT `sub`) should be included in log MDC. `session_token_hash` safe to log (hash, not raw token). |
| **Rate Limiting** | resend-otp: 60-second cooldown enforced at service layer by checking `otp_sessions.created_at`; returns `429` with `Retry-After: 60` header. General endpoint throttling (Bucket4j) deferred to a follow-up story. |
| **Idempotency** | verify-otp: not idempotent by design (each call may mutate attempt counter). resend-otp: idempotent within the rate-limit window (same OTP until 60 s pass). |
| **OTP security** | CSPRNG (`SecureRandom`); 5-min TTL; 3-attempt lockout; session invalidation prevents replay after success or lockout. |
| **Secrets** | `security.jwt.secret` already externalised (ADR-002). No new secrets introduced. SMS provider credentials (production, future) must use environment variables only — never hardcode. |
| **Token cleanup** | Expired `otp_sessions` and `refresh_tokens` rows accumulate. Periodic cleanup (batch job or scheduled task) is **recommended** but out of scope for this story. |

---

### Package Structure

```
com.northbank.auth
  ├── controller/
  │     └── AuthController.java          (extended: verify-otp, resend-otp endpoints)
  ├── service/
  │     ├── AuthService.java             (extended: OTP generation on login success)
  │     ├── OtpService.java              (new: verify + resend logic)
  │     └── dto/
  │           ├── VerifyOtpRequest.java
  │           ├── ResendOtpRequest.java
  │           └── TokenResponse.java     (accessToken + refreshToken)
  ├── domain/
  │   └── model/
  │         ├── OtpSession.java          (new JPA entity)
  │         └── RefreshToken.java        (new JPA entity)
  ├── repository/
  │     ├── OtpSessionRepository.java    (findBySessionTokenHash)
  │     └── RefreshTokenRepository.java  (findByTokenHash)
  └── sms/
        ├── SmsService.java              (new interface)
        └── StubSmsService.java          (new @Primary @Profile("dev","test"))
```

---

### Consequences

#### Positive

- **Simple, auditable persistence.** Both `otp_sessions` and `refresh_tokens` are standard
  SQL tables — visible in DB tooling, queryable for support, and compatible with horizontal
  scaling without requiring Redis or shared in-memory state.
- **Hash-at-rest for session correlation.** `session_token_hash` ensures that even a DB
  dump does not leak usable sessionTokens.
- **Consistent security patterns.** The hash-at-rest + Base64URL token generation mirrors
  ADR-004 exactly, reducing cognitive overhead for developers.
- **Stateless access token.** No server-side session — the access token is verified
  purely by JWT signature, consistent with ADR-002.
- **SMS stubbing identical to Email stubbing.** The `@Primary @Profile` pattern is
  already understood by the team (ADR-004).
- **`remainingAttempts` in 401 response.** Enables the frontend to drive UX without
  keeping client-side state.
- **Refresh token table provisioned early.** The token-refresh endpoint story will
  require no V5 schema migration.

#### Negative / Trade-offs

- **`otp_sessions` rows accumulate.** Expired rows are not auto-cleaned; a maintenance
  job is needed (deferred).
- **sessionToken cannot be revoked before its 5-min expiry.** Inherited from ADR-002
  (stateless JWT). An attacker who steals the sessionToken could attempt OTP verification,
  but they would still need the OTP delivered to the customer's phone.
- **OTP stored as plaintext.** For 6-digit codes, SHA-256 hashing is trivially
  reversible by brute force and creates false security. The 5-min TTL + 3-attempt lockout
  + CSPRNG generation provide equivalent real-world protection.
- **Rate limit (60 s) is session-scoped, not IP-scoped.** A compromised sessionToken
  could be rate-limited by an attacker before the legitimate user resends. IP-based
  rate limiting deferred to a follow-up story.
- **`security.jwt.secret` used for both SESSION and ACCESS tokens.** A single signing
  key means a breach of the key compromises both token types. Key rotation / separate
  keys are recommended for production hardening (deferred).
- **`AuthService.login()` (ADR-002) must be extended** — a small but real coupling
  between the US-002 and US-003 implementation tasks.

---

### Alternatives Considered

**OTP storage options:**

- **(b) Stateless OTP embedded in sessionToken claims** — rejected: the OTP would be
  visible to the client (JWT payload is base64-decoded, not encrypted). AC5 (attempt
  counter) also requires server-side mutable state, making full statefulness unavoidable.
- **(c) In-memory / Redis cache** — rejected: adds infrastructure complexity (new
  container, connection pooling, cache eviction tuning) with no benefit for an MVP that
  already uses PostgreSQL. Horizontal scaling works without Redis because the DB is the
  shared state store.

**OTP hashing:**

- **SHA-256(OTP)** — rejected: 10⁶ possible values makes this trivially brute-forceable
  in microseconds. False sense of security with no practical benefit given the existing
  mitigations (TTL, lockout). Honesty principle: store plaintext and document the
  reasoning.

**Session correlation:**

- **Add `jti` claim to sessionToken (modify ADR-002)** — considered: clean, canonical,
  and indexed. Rejected to avoid reopening an Accepted ADR. SHA-256(rawJwtString)
  achieves the same deterministic lookup without any change to the ADR-002 contract.

**Refresh token storage:**

- **JWT as refresh token** — rejected: AC for this story does not require refresh-token
  revocation introspection at the JWT layer, and ADR-004 already established the
  hash-at-rest opaque token pattern. A JWT refresh token would couple revocation to
  server-side blacklist anyway, negating its stateless advantage.

**Rate limiting mechanism:**

- **Bucket4j** — considered for the 60-second resend cooldown. Rejected in favour of a
  service-layer `created_at` timestamp check, which is simpler, requires no additional
  library, and is naturally consistent with the DB transaction boundary.

---

### Architect → Dev Handoff Checklist

- ✅ ADR written and **Accepted** for US-003
- ✅ Conflict check vs ADR-001, ADR-002, ADR-004 — no conflicts; one additive ADR-002 dependency flagged
- ✅ Persistence strategy decided: PostgreSQL (SQL); two new tables via V4 migration
- ✅ V4 DDL defined: `otp_sessions` + `refresh_tokens` (full `CREATE TABLE` + indexes)
- ✅ OTP generation algorithm specified: `SecureRandom.nextInt(1_000_000)` → `String.format("%06d", n)`
- ✅ Session correlation strategy: `SHA-256(raw sessionToken JWT string)` → `session_token_hash`
- ✅ OTP lifecycle fully designed: generation → storage → validation → expiry → lockout → success
- ✅ Lockout rule: 3 consecutive failures → `invalidated=true`; client restarts from US-002
- ✅ Access token specified: HS256 JWT, 15-min, `type="ACCESS"`, same key as ADR-002
- ✅ Refresh token specified: 32B SecureRandom → Base64URL (43 chars), SHA-256 hash at rest, 7-day expiry
- ✅ API endpoints defined: `POST /api/v1/auth/verify-otp` and `POST /api/v1/auth/resend-otp`
- ✅ Full request/response shapes specified (200/401/429)
- ✅ `remainingAttempts` ProblemDetail extension field specified
- ✅ `Retry-After: 60` header specified on 429 response
- ✅ SMS stub design specified: `SmsService` interface + `StubSmsService` `@Primary @Profile({"dev","test"})`
- ✅ Package structure defined: `com.northbank.auth.sms`, `OtpService`, `OtpSession`, `RefreshToken`
- ✅ Cross-cutting concerns specified: auth gate, RFC 7807 errors, logging rules, rate limit, no-secrets
- ✅ ADR-002 additive dependency clearly flagged: `AuthService.login()` must generate OTP session on success
- ✅ Token cleanup deferred noted (non-functional risk captured)
- ✅ Messaging topology: N/A (REST only)
- ✅ External integrations: N/A (SMS stubbed; production implementation out of scope)
- ✅ Non-functional notes: cleanup job deferred; IP rate limiting deferred; key rotation recommended
