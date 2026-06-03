# API Contracts — US-001: Customer Self-Registration

All paths are versioned under `/api/v1/`. Error responses use RFC 7807 Problem Details
(`Content-Type: application/problem+json`). Every endpoint is documented via OpenAPI/Swagger.

---

## POST /api/v1/customers

Register a new customer. Public (anonymous) endpoint.

- **Auth:** None (Spring Security `permitAll()` for this path).
- **Content-Type:** `application/json`
- **Idempotency:** Natural via email uniqueness — duplicate email → `409`.

### Request Body

| Field | Type | Required | Validation |
|-------|------|----------|------------|
| `firstName` | string | yes | not blank, ≤100 |
| `lastName` | string | yes | not blank, ≤100 |
| `email` | string | yes | valid email, ≤255, unique (AC2) |
| `phoneNumber` | string | yes | E.164 `^\+[1-9]\d{1,14}$` (AC4) |
| `dateOfBirth` | string (`yyyy-MM-dd`) | yes | ISO date, must be in the past |
| `password` | string | yes | ≥8, ≥1 upper, ≥1 lower, ≥1 digit, ≥1 special (AC3); write-only |

**Example request:**

```json
POST /api/v1/customers
Content-Type: application/json

{
  "firstName": "Ada",
  "lastName": "Lovelace",
  "email": "ada.lovelace@example.com",
  "phoneNumber": "+14155552671",
  "dateOfBirth": "1990-12-10",
  "password": "Str0ng!Pass"
}
```

### Responses

#### 201 Created (AC5, AC6)

A `customers` row is persisted with `status = PENDING_VERIFICATION`. Body contains only the new `id`.

```json
HTTP/1.1 201 Created
Location: /api/v1/customers/9f1c2e7a-3b4d-4c5e-8a9b-0c1d2e3f4a5b
Content-Type: application/json

{
  "id": "9f1c2e7a-3b4d-4c5e-8a9b-0c1d2e3f4a5b"
}
```

#### 400 Bad Request — validation failure (AC3, AC4)

```json
HTTP/1.1 400 Bad Request
Content-Type: application/problem+json

{
  "type": "https://api.bank.example/problems/validation-error",
  "title": "Validation Failed",
  "status": 400,
  "detail": "One or more fields are invalid.",
  "instance": "/api/v1/customers",
  "errors": [
    { "field": "password", "message": "Password must contain at least one special character" },
    { "field": "phoneNumber", "message": "Phone number must be in E.164 format" }
  ]
}
```

#### 409 Conflict — email already registered (AC2)

```json
HTTP/1.1 409 Conflict
Content-Type: application/problem+json

{
  "type": "https://api.bank.example/problems/email-already-registered",
  "title": "Email Already Registered",
  "status": 409,
  "detail": "Email already registered",
  "instance": "/api/v1/customers"
}
```

#### 500 Internal Server Error

```json
HTTP/1.1 500 Internal Server Error
Content-Type: application/problem+json

{
  "type": "about:blank",
  "title": "Internal Server Error",
  "status": 500,
  "detail": "An unexpected error occurred. Please try again later.",
  "instance": "/api/v1/customers"
}
```

### Error → AC mapping

| Status | Trigger | Exception → Handler | AC |
|--------|---------|---------------------|----|
| 400 | Bean Validation / `@Password` / E.164 failure | `MethodArgumentNotValidException` | AC3, AC4 |
| 409 | Email already exists (pre-check or DB constraint) | `EmailAlreadyRegisteredException` / `DataIntegrityViolationException` | AC2 |
| 500 | Unexpected error | catch-all `Exception` | — |

### Field-error contract (frontend mapping)

The 400 `errors[]` items each carry `field` (camelCase, matches request body) and `message` (human-readable, tells how to fix). The frontend maps `field` to its form input to render inline errors.

### Security notes

- `password` is **write-only**: never returned in any response and never logged.
- Stored as BCrypt hash (`password_hash`, strength 12).
- Endpoint should be rate-limited in a follow-up story to deter abuse/enumeration.

---

# API Contracts — US-003: Two-Factor Authentication via SMS

All paths are versioned under `/api/v1/`. Error responses use RFC 7807 Problem Details
(`Content-Type: application/problem+json`). Both endpoints are **public** (Spring Security
`permitAll()`); authentication is handled by validating the `sessionToken` JWT in the
request body.

**Related ADR:** [ADR-003-two-factor-authentication-sms.md](./ADR-003-two-factor-authentication-sms.md)

---

## POST /api/v1/auth/verify-otp

Submit the 6-digit OTP to complete 2FA and receive JWT access + refresh tokens.

- **Auth:** None (public). The `sessionToken` field in the request body is the
  authentication gate — validated for HS256 signature, expiry, and `type="SESSION"` claim.
- **Content-Type:** `application/json`
- **Idempotency:** Not idempotent. Each call may mutate `failed_attempts` and, on the
  third failure, set `invalidated = true`.

### Request Body

| Field | Type | Required | Validation |
|-------|------|----------|-----------|
| `sessionToken` | string | yes | Valid HS256 JWT; `type="SESSION"` claim present; `exp` in the future |
| `otp` | string | yes | Exactly 6 decimal digits (`^\d{6}$`) |

**Example request:**

```json
POST /api/v1/auth/verify-otp
Content-Type: application/json

{
  "sessionToken": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiI5ZjFjMmU3YS0....<sig>",
  "otp": "482917"
}
```

### Responses

#### 200 OK — Valid OTP (AC3)

The OTP matched, was not expired, and the session was not already invalidated.
The `otp_sessions` row is marked `invalidated = true` to prevent replay.
An access token and a refresh token are issued.

```json
HTTP/1.1 200 OK
Content-Type: application/json

{
  "accessToken":  "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiI5ZjFjMmU3YS....<sig>",
  "refreshToken": "dGhpcyBpcyBhIDQzLWNoYXIgQmFzZTY0VVJM_example_43"
}
```

| Field | Description |
|-------|-------------|
| `accessToken` | HS256 JWT; `sub`=customer UUID; `type="ACCESS"`; expires in **15 minutes** |
| `refreshToken` | 43-char Base64URL opaque token; stored as SHA-256 hash in `refresh_tokens` table; expires in **7 days** |

#### 401 Unauthorized — Invalid or expired OTP (AC2, AC4)

Returned when:
- The submitted `otp` does not match the stored code (AC4), **or**
- The OTP's `expires_at` has passed (AC2), **or**
- The session's `invalidated` flag is `true` — either from a prior 3rd failure (AC5)
  or because the OTP was already used successfully.

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

> **`remainingAttempts` field (RFC 7807 extension):**
> - Present on **all** `401` responses from this endpoint.
> - Value = `max(0, 3 − failed_attempts_after_this_call)`.
> - `2` → 2 attempts left; `1` → 1 attempt left; `0` → session is invalidated (AC5).
> - Expiry (AC2) does **not** increment `failed_attempts`; value reflects current counter.

#### 401 Unauthorized — Invalid sessionToken (malformed JWT / wrong type / expired JWT)

```json
HTTP/1.1 401 Unauthorized
Content-Type: application/problem+json

{
  "type":     "https://api.bank.example/problems/invalid-session-token",
  "title":    "Invalid Session Token",
  "status":   401,
  "detail":   "Invalid or expired session token",
  "instance": "/api/v1/auth/verify-otp"
}
```

#### 400 Bad Request — Bean Validation failure

```json
HTTP/1.1 400 Bad Request
Content-Type: application/problem+json

{
  "type":   "https://api.bank.example/problems/validation-error",
  "title":  "Validation Failed",
  "status": 400,
  "detail": "One or more fields are invalid.",
  "errors": [
    { "field": "otp", "message": "OTP must be exactly 6 digits" }
  ]
}
```

### Error → AC mapping

| Status | Trigger | AC |
|--------|---------|----|
| 200 | OTP matches, not expired, session active | AC3 |
| 401 `invalid-otp` | OTP expired | AC2 |
| 401 `invalid-otp` | Wrong OTP (1st or 2nd attempt) | AC4 |
| 401 `invalid-otp` + `remainingAttempts: 0` | 3rd wrong OTP → session invalidated | AC4 + AC5 |
| 401 `invalid-otp` + `remainingAttempts: 0` | Session already invalidated (4th+ attempt) | AC5 |
| 401 `invalid-session-token` | JWT invalid / expired / wrong type | — |
| 400 | Field validation failure | — |

---

## POST /api/v1/auth/resend-otp

Re-generate a new 6-digit OTP and re-deliver it via SMS (stubbed). Rate-limited to
**one resend per 60 seconds** per session.

- **Auth:** None (public). The `sessionToken` field is the authentication gate.
- **Content-Type:** `application/json`
- **Rate limit:** 1 request per 60 seconds per sessionToken. Enforced at the service
  layer via `otp_sessions.created_at`.

### Request Body

| Field | Type | Required | Validation |
|-------|------|----------|-----------|
| `sessionToken` | string | yes | Valid HS256 JWT; `type="SESSION"` claim present; `exp` in the future |

**Example request:**

```json
POST /api/v1/auth/resend-otp
Content-Type: application/json

{
  "sessionToken": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiI5ZjFjMmU3YS0....<sig>"
}
```

### Responses

#### 200 OK — OTP resent

A new OTP has been generated, the `otp_sessions` row updated (new code, new `expires_at`,
`failed_attempts` reset to `0`), and `SmsService.sendOtp()` invoked.

```json
HTTP/1.1 200 OK
Content-Type: application/json

{
  "message": "OTP sent"
}
```

#### 429 Too Many Requests — Rate limit exceeded

Returned when less than 60 seconds have elapsed since the last OTP was sent
(`now() − otp_sessions.created_at < 60 s`).

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

> The `Retry-After: 60` response header is set to assist client-side countdown timers.

#### 401 Unauthorized — Session invalidated or invalid sessionToken

```json
HTTP/1.1 401 Unauthorized
Content-Type: application/problem+json

{
  "type":     "https://api.bank.example/problems/invalid-session-token",
  "title":    "Invalid Session Token",
  "status":   401,
  "detail":   "Session has been invalidated. Please restart the login process.",
  "instance": "/api/v1/auth/resend-otp"
}
```

### Error → AC mapping

| Status | Trigger |
|--------|---------|
| 200 | OTP regenerated and sent (cooldown elapsed) |
| 429 | Resend attempted < 60 s since last send |
| 401 | sessionToken invalid / expired / wrong type; OR session `invalidated=true` |

---

## Token Reference (US-003)

| Token | Type | Expiry | Storage | Signing / Encoding |
|-------|------|--------|---------|-------------------|
| `sessionToken` (from US-002) | HS256 JWT | 5 min | Stateless (no DB) | `security.jwt.secret`, `type="SESSION"` |
| `accessToken` (issued here) | HS256 JWT | 15 min | Stateless (no DB) | `security.jwt.secret`, `type="ACCESS"` |
| `refreshToken` (issued here) | Opaque 43-char Base64URL | 7 days | SHA-256 hash in `refresh_tokens` table | `SecureRandom` 32 bytes |

---

# API Contracts — US-005: View & Update Customer Profile

All paths versioned under `/api/v1/`. Error responses use RFC 7807 Problem Details
(`Content-Type: application/problem+json`). Both endpoints require a valid ACCESS JWT.

**Related ADR:** [ADR-005-view-update-profile.md](./ADR-005-view-update-profile.md)

---

## GET /api/v1/profile

Return the authenticated customer's personal profile details.

- **Auth:** `Authorization: Bearer <accessToken>` — HS256 ACCESS JWT required (AC6).
  Validated by `JwtAuthenticationFilter` before the request reaches the controller.
- **Content-Type (response):** `application/json`
- **Idempotency:** Safe (read-only). May be called repeatedly with identical results.

### Request

No body. No query parameters. Auth header only.

```
GET /api/v1/profile
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiI5ZjFjMmU3YS0....<sig>
```

### Responses

#### 200 OK (AC1)

```json
HTTP/1.1 200 OK
Content-Type: application/json

{
  "firstName":   "Ada",
  "lastName":    "Lovelace",
  "email":       "ada.lovelace@example.com",
  "phoneNumber": "+14155552671",
  "dateOfBirth": "1990-12-10"
}
```

| Field | Type | Source column | Mutable? |
|-------|------|--------------|----------|
| `firstName` | string | `customers.first_name` | Yes (via PATCH) |
| `lastName` | string | `customers.last_name` | Yes (via PATCH) |
| `email` | string | `customers.email` | **No — read-only (AC3)** |
| `phoneNumber` | string (E.164) | `customers.phone_number` | Yes (via PATCH) |
| `dateOfBirth` | string `yyyy-MM-dd` | `customers.date_of_birth` | **No — read-only (AC3)** |

#### 401 Unauthorized (AC6) — missing, malformed, or expired ACCESS token

```json
HTTP/1.1 401 Unauthorized
Content-Type: application/problem+json

{
  "type":     "https://api.bank.example/problems/unauthorized",
  "title":    "Unauthorized",
  "status":   401,
  "detail":   "Authentication required. Please provide a valid access token.",
  "instance": "/api/v1/profile"
}
```

#### 401 Unauthorized — token issued before password reset (ADR-004 AC5)

Returned when the token's `iat` (issued-at) is earlier than `customers.password_changed_at`,
meaning the session was created before the customer last reset their password.

```json
HTTP/1.1 401 Unauthorized
Content-Type: application/problem+json

{
  "type":     "https://api.bank.example/problems/unauthorized",
  "title":    "Unauthorized",
  "status":   401,
  "detail":   "Session invalidated. Please sign in again.",
  "instance": "/api/v1/profile"
}
```

### Error → AC mapping

| Status | Trigger | AC |
|--------|---------|----|
| 200 | Valid ACCESS token; customer found | AC1 |
| 401 `unauthorized` | Missing / malformed / expired / wrong-type token | AC6 |
| 401 `unauthorized` | `iat < password_changed_at` | ADR-004 AC5 |

---

## PATCH /api/v1/profile

Partially update mutable profile fields. Only `firstName`, `lastName`, and `phoneNumber`
may be changed. `email` and `dateOfBirth` are permanently read-only (AC3).

- **Auth:** `Authorization: Bearer <accessToken>` — ACCESS JWT required (AC6).
- **Content-Type (request):** `application/json`
- **Idempotency:** Idempotent — repeating the same PATCH produces the same state.
- **Partial update semantics:** Only fields present (non-null) in the request body are
  applied. Absent / null fields leave the corresponding column unchanged.

### Request Body

All three fields are individually optional. A PATCH may update one, two, or all three.

| Field | Type | Required | Validation |
|-------|------|----------|------------|
| `firstName` | string | no | If present: `@NotBlank`, ≤100 chars |
| `lastName` | string | no | If present: `@NotBlank`, ≤100 chars |
| `phoneNumber` | string | no | If present: E.164 `^\+[1-9]\d{1,14}$` via `@PhoneNumber` (AC4) |

> **Forbidden fields:** If `email` or `dateOfBirth` (or any unrecognised field) appear
> in the JSON body, the request is rejected immediately with `400 "Field is not editable"` (AC3).
> This is enforced at DTO deserialization time via a `@JsonAnySetter` sentinel — no service
> logic is executed.

**Example — update all three mutable fields:**

```json
PATCH /api/v1/profile
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiI5ZjFjMmU3YS0....<sig>
Content-Type: application/json

{
  "firstName":   "Ada",
  "lastName":    "Byron",
  "phoneNumber": "+14155559999"
}
```

**Example — update phone number only:**

```json
PATCH /api/v1/profile
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
Content-Type: application/json

{
  "phoneNumber": "+441234567890"
}
```

**Example — attempt to update a read-only field (AC3):**

```json
PATCH /api/v1/profile
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
Content-Type: application/json

{
  "firstName": "Ada",
  "email": "new@example.com"
}
```
→ Returns `400` (see below — email is forbidden regardless of other valid fields in body).

### Responses

#### 200 OK (AC2, AC5) — returns the complete updated profile

The response shape is identical to `GET /api/v1/profile`, enabling the frontend to
replace its cached profile state directly from the PATCH response.

```json
HTTP/1.1 200 OK
Content-Type: application/json

{
  "firstName":   "Ada",
  "lastName":    "Byron",
  "email":       "ada.lovelace@example.com",
  "phoneNumber": "+14155559999",
  "dateOfBirth": "1990-12-10"
}
```

#### 400 Bad Request — forbidden / read-only field present (AC3)

Triggered when `email`, `dateOfBirth`, or any other unrecognised field appears in the body.
The `detail` value `"Field is not editable"` is the exact string required by AC3.

```json
HTTP/1.1 400 Bad Request
Content-Type: application/problem+json

{
  "type":     "https://api.bank.example/problems/field-not-editable",
  "title":    "Field Not Editable",
  "status":   400,
  "detail":   "Field is not editable",
  "instance": "/api/v1/profile"
}
```

#### 400 Bad Request — E.164 phone number validation failure (AC4)

```json
HTTP/1.1 400 Bad Request
Content-Type: application/problem+json

{
  "type":     "https://api.bank.example/problems/validation-error",
  "title":    "Validation Failed",
  "status":   400,
  "detail":   "One or more fields are invalid.",
  "instance": "/api/v1/profile",
  "errors": [
    { "field": "phoneNumber", "message": "Phone number must be in E.164 format" }
  ]
}
```

#### 400 Bad Request — blank / oversized mutable field

```json
HTTP/1.1 400 Bad Request
Content-Type: application/problem+json

{
  "type":     "https://api.bank.example/problems/validation-error",
  "title":    "Validation Failed",
  "status":   400,
  "detail":   "One or more fields are invalid.",
  "instance": "/api/v1/profile",
  "errors": [
    { "field": "firstName", "message": "must not be blank" }
  ]
}
```

#### 401 Unauthorized (AC6) — missing, malformed, or expired ACCESS token

```json
HTTP/1.1 401 Unauthorized
Content-Type: application/problem+json

{
  "type":     "https://api.bank.example/problems/unauthorized",
  "title":    "Unauthorized",
  "status":   401,
  "detail":   "Authentication required. Please provide a valid access token.",
  "instance": "/api/v1/profile"
}
```

#### 401 Unauthorized — session invalidated after password reset (ADR-004 AC5)

```json
HTTP/1.1 401 Unauthorized
Content-Type: application/problem+json

{
  "type":     "https://api.bank.example/problems/unauthorized",
  "title":    "Unauthorized",
  "status":   401,
  "detail":   "Session invalidated. Please sign in again.",
  "instance": "/api/v1/profile"
}
```

### Error → AC mapping

| Status | Trigger | AC |
|--------|---------|----|
| 200 | Valid token; fields applied; updated profile returned | AC2, AC5 |
| 400 `field-not-editable` | `email` or `dateOfBirth` (or unknown field) in request body | AC3 |
| 400 `validation-error` (phoneNumber) | Invalid E.164 format | AC4 |
| 400 `validation-error` (firstName/lastName) | Blank or exceeds 100 chars | — |
| 401 `unauthorized` | Missing / malformed / expired / wrong-type token | AC6 |
| 401 `unauthorized` | `iat < password_changed_at` | ADR-004 AC5 |

---

## `JwtAuthenticationFilter` — Request Flow (US-005 / all protected endpoints)

```
Client Request
    │
    ▼
JwtAuthenticationFilter (OncePerRequestFilter)
    │
    ├─ [No Authorization header OR not "Bearer "] ──────────────────► 401 RFC 7807
    │
    ├─ JwtConfig.validateAccessToken(rawToken)
    │     ├─ HS256 signature ✓
    │     ├─ exp > now() ✓
    │     └─ type == "ACCESS" ✓
    │
    ├─ [JwtException / parse error] ────────────────────────────────► 401 RFC 7807
    │
    ├─ customerRepository.findById(UUID from sub)
    │     └─ [not found] ──────────────────────────────────────────► 401 RFC 7807
    │
    ├─ [passwordChangedAt != null AND iat < passwordChangedAt] ─────► 401 RFC 7807
    │                                                                  "Session invalidated"
    │
    └─ SecurityContextHolder.set(UsernamePasswordAuthenticationToken)
           │
           ▼
    DispatcherServlet → ProfileController
```

---

## Token Reference (cumulative — US-001 through US-005)

| Token | Issued by | Type | Expiry | `type` claim | Protected by |
|-------|-----------|------|--------|-------------|--------------|
| `sessionToken` | `POST /api/v1/auth/login` (US-002) | HS256 JWT | 5 min | `SESSION` | `JwtConfig.validateSessionToken()` |
| `accessToken` | `POST /api/v1/auth/verify-otp` (US-003) | HS256 JWT | 15 min | `ACCESS` | `JwtAuthenticationFilter` + `JwtConfig.validateAccessToken()` (US-005) |
| `refreshToken` | `POST /api/v1/auth/verify-otp` (US-003) | Opaque Base64URL | 7 days | — (opaque) | SHA-256 hash in `refresh_tokens` |

---

## Security Notes (US-005)

- The raw `accessToken` string must **never** appear in structured logs. Log only the
  customer UUID (available post-filter as the principal name).
- `JwtAuthenticationFilter` must write 401 responses directly to `HttpServletResponse`
  (not via MVC `@ExceptionHandler`) because it runs before the DispatcherServlet.
  Use `ObjectMapper` to serialize `ProblemDetail` with `Content-Type: application/problem+json`.
- The filter's `password_changed_at` DB read happens on every authenticated request.
  A future story may introduce Redis caching of `{ customerId → passwordChangedAt }` to
  avoid the per-request query — deferred until profiling identifies it as a bottleneck.
- Profile data (firstName, lastName, email, phoneNumber, dateOfBirth) constitutes PII.
  Encryption-at-rest and data-retention obligations must be addressed at the infrastructure
  level (out of scope for this story).

---

## Security Notes (US-003)

- `otp` codes must **never** be logged at INFO or above in production-profile code.
  `StubSmsService` may log them in `dev`/`test` profiles only.
- `sessionToken` must be validated for signature + expiry + `type="SESSION"` **before**
  any DB lookup is performed, to prevent timing oracle attacks on `otp_sessions`.
- `session_token_hash` (SHA-256 of raw JWT) is safe to include in structured logs;
  the raw `sessionToken` must not appear in logs.
- The `refreshToken` is a single-use opaque token. Rotation policy (revoke-on-use) should
  be enforced by the token-refresh endpoint (future story).
- Both endpoints should be protected by general IP-rate limiting (Bucket4j) in a
  follow-up hardening story.
