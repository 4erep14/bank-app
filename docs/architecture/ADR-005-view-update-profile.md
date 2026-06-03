## ADR-005: View & Update Customer Profile

**Story:** US-005
**Status:** Accepted

---

### Context

US-005 introduces the first **authenticated, self-service data operation** in NorthBank: a logged-in customer must be able to view their own profile and update a restricted subset of their personal details (first name, last name, phone number). Email and date of birth are immutable after registration.

This ADR builds on four accepted predecessors:

| ADR | Relevant baseline |
|-----|-------------------|
| ADR-001 (Accepted) | `customers` table (V1) with all required columns already present; BCrypt encoder; RFC 7807 `GlobalExceptionHandler`; `@PhoneNumber` / E.164 constraint; package root `com.northbank.registration` |
| ADR-002 (Accepted) | `jjwt` 0.12.x library; HS256 HMAC-SHA256 signing key `security.jwt.secret`; `type="SESSION"` JWT (5-min); `type="ACCESS"` JWT (15-min); Spring Security STATELESS + CSRF disabled; `JwtConfig.validateSessionToken()` exists — **`validateAccessToken()` is required by this story** |
| ADR-003 (Accepted) | `otp_sessions` and `refresh_tokens` tables (V4); access token issued with `sub`=customer UUID, `type="ACCESS"`, `iat`/`exp` standard claims |
| ADR-004 (Accepted) | `customers.password_changed_at` column (V3); session-invalidation rule: any JWT with `iat < password_changed_at` must be rejected |

**What this ADR decides:**

1. How `GET /api/v1/profile` and `PATCH /api/v1/profile` are authenticated (AC6).
2. Which fields are readable (AC1), which are updatable (AC2), and how immutable fields are enforced (AC3).
3. E.164 phone validation strategy (AC4).
4. Persistence — whether a new migration is required (answer: **no**).
5. Communication pattern.
6. Spring Security filter chain changes needed to protect the new endpoints.
7. Package structure for the new `profile` domain.

**Conflict check vs existing ADRs:**

- **ADR-001:** No conflict. No `customers` schema changes; `@PhoneNumber` annotation is reused as-is.
- **ADR-002:** **Additive dependency only.** `JwtConfig` must be extended with a `validateAccessToken()` method. The existing `validateSessionToken()` method, the security config class, and all prior endpoints are untouched. Spring Security `SecurityConfig` is modified additively: public paths keep `permitAll()`; profile paths gain `authenticated()`.
- **ADR-003:** No conflict. Access token structure (`type="ACCESS"`, `sub`=UUID) is consumed here without modification.
- **ADR-004:** **Completes the AC5 dependency.** ADR-004's Handoff Checklist item — _"JWT filter enforces `iat >= password_changed_at`"_ — is implemented by the `JwtAuthenticationFilter` introduced in this story. This is explicitly the runtime closure of that open item.

> ⚠️ **ADR-002/ADR-004 open item closed here:** `JwtAuthenticationFilter` must enforce `iat ≥ password_changed_at` (ADR-004 AC5). The `customers.password_changed_at` column already exists (V3). The filter introduced in this story completes that enforcement contract.

---

### Decision

Implement the profile feature as two **synchronous REST** endpoints (`GET /api/v1/profile`, `PATCH /api/v1/profile`) that read from and write to the existing PostgreSQL `customers` table. No new Flyway migration, no messaging, no external integrations.

**Decision summary — eight points:**

#### 1. JWT Authentication Filter (`JwtAuthenticationFilter`)

A new `JwtAuthenticationFilter` extending `OncePerRequestFilter` is added to the Spring Security filter chain **before** `UsernamePasswordAuthenticationFilter`. It guards every endpoint that requires `authenticated()` in the security config.

**Filter algorithm (ordered steps):**

```
1. Extract header:
     - Read "Authorization" header from HttpServletRequest.
     - If missing or does not start with "Bearer " → send RFC 7807 401 and return immediately.

2. Parse & validate as ACCESS token:
     - Strip "Bearer " prefix to get raw JWT compact string.
     - Call JwtConfig.validateAccessToken(rawToken):
         a. Verify HS256 signature against security.jwt.secret.
         b. Verify token is not expired (exp > now()).
         c. Verify claim type == "ACCESS".
         d. Extract sub (customer UUID string).
     - On any JwtException / SignatureException / ExpiredJwtException → send RFC 7807 401.

3. Enforce password_changed_at invalidation (ADR-004 AC5):
     - Obtain the token's iat (issued-at) epoch second from JWT claims.
     - Load customer from DB (by UUID from sub). If not found → send RFC 7807 401.
     - If customer.passwordChangedAt != null AND Instant.ofEpochSecond(iat).isBefore(customer.passwordChangedAt)
         → send RFC 7807 401 with detail "Session invalidated. Please sign in again."

4. Set SecurityContext:
     - Build UsernamePasswordAuthenticationToken(customerUUID, null, emptyList()).
     - Set SecurityContextHolder.getContext().setAuthentication(token).
     - Call filterChain.doFilter(request, response).
```

The `sub` claim (customer UUID string) is available downstream in controllers via `@AuthenticationPrincipal` (or `SecurityContextHolder`) as the principal name.

#### 2. No New DB Migration

**All required columns already exist on `customers` (V1/V3):**

| Column | Source migration | Used by |
|--------|-----------------|---------|
| `id` | V1 | Primary key / JWT `sub` |
| `first_name` | V1 | GET response (AC1); PATCH update (AC2) |
| `last_name` | V1 | GET response (AC1); PATCH update (AC2) |
| `email` | V1 | GET response (AC1); read-only (AC3) |
| `phone_number` | V1 | GET response (AC1); PATCH update with E.164 (AC2/AC4) |
| `date_of_birth` | V1 | GET response (AC1); read-only (AC3) |
| `password_changed_at` | V3 | Filter iat-check (ADR-004 AC5) |

**No Flyway migration is needed for this story.** The next available migration number remains `V5` for a future story.

#### 3. No Messaging

Read + partial-update of structured identity data is a simple synchronous request/reply interaction. No eventual consistency, no decoupled consumers, no event sourcing requirements. REST only.

#### 4. API Endpoints

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| `GET` | `/api/v1/profile` | ACCESS JWT required | Return authenticated customer's profile (AC1) |
| `PATCH` | `/api/v1/profile` | ACCESS JWT required | Partially update firstName, lastName, phoneNumber (AC2) |

See the **API Endpoints (Detailed)** section below for full request/response shapes.

#### 5. `UpdateProfileRequest` DTO — Field Rules

The PATCH request body DTO carries only the three mutable fields. The `email` and `dateOfBirth` fields **must not appear in the DTO class at all** — enforcing the "unknown field → 400" rule at deserialization time via Jackson configuration. The detailed enforcement strategy is:

- The DTO class declares only `firstName`, `lastName`, `phoneNumber` (all optional / nullable).
- `ObjectMapper` is configured with `DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES = true` **or** the DTO is annotated with `@JsonIgnoreProperties(ignoreUnknown = false)`.
- If a client sends `email` or `dateOfBirth` in the request body, Jackson raises `HttpMessageNotReadableException` → the `GlobalExceptionHandler` must translate this to `400` with detail `"Field is not editable"`.
  - Alternatively, a dedicated `@JsonAnySetter` method on the DTO can capture unexpected properties and throw `FieldNotEditableException` mapped to `400` — giving a richer per-field error message.
- The preferred approach is the **`@JsonAnySetter` sentinel** (see API Endpoints section) because it allows a specific, targeted `"Field is not editable"` message rather than the generic unknown-property error.

All three declared fields are individually optional (a PATCH may update one, two, or all three). Only non-null fields in the request body are applied to the entity. A PATCH body with zero known fields and no unknown fields is accepted with `200` and returns the unchanged profile (idempotent no-op).

#### 6. E.164 Validation Reuse

The `@PhoneNumber` constraint annotation and its `PhoneNumberValidator` (introduced in US-001 and located in `com.northbank.registration`) are reused directly on the `phoneNumber` field of `UpdateProfileRequest`. No new validator is created. Invalid format → `400` with `errors[{ "field": "phoneNumber", "message": "Phone number must be in E.164 format" }]` via the existing `GlobalExceptionHandler`.

#### 7. Package Structure

A new `profile` sub-package is added under the existing `com.northbank.registration` root:

```
com.northbank.registration.profile/
  ProfileController       — @RestController, maps GET + PATCH /api/v1/profile
  ProfileService          — @Service @Transactional, orchestrates reads and updates
  ProfileResponse         — Response DTO (firstName, lastName, email, phoneNumber, dateOfBirth)
  UpdateProfileRequest    — Request DTO (firstName?, lastName?, phoneNumber?); rejects unknown fields
```

The `CustomerRepository` (already defined in `com.northbank.registration`) is injected into `ProfileService` — no new repository interface is required for the `customers` table.

#### 8. Spring Security Config Update

`SecurityConfig` is updated additively. **No existing `permitAll()` path is removed.**

```
Public (permitAll):
  POST  /api/v1/customers            (US-001 — registration)
  POST  /api/v1/auth/login           (US-002 — login)
  POST  /api/v1/auth/forgot-password (US-004 — password reset request)
  POST  /api/v1/auth/reset-password  (US-004 — password reset confirm)
  POST  /api/v1/auth/verify-otp      (US-003 — OTP verification)
  POST  /api/v1/auth/resend-otp      (US-003 — OTP resend)

Authenticated (require valid ACCESS JWT via JwtAuthenticationFilter):
  GET   /api/v1/profile              (US-005)
  PATCH /api/v1/profile              (US-005)

Default:
  All other paths → deny (401)
```

`JwtAuthenticationFilter` is registered in the filter chain with `addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)`. The filter internally skips public paths (no `Authorization` header present → passes through without setting context, relying on `.anyRequest().authenticated()` to reject).

---

### Persistence

**Choice: No new migration — existing PostgreSQL `customers` table (SQL, V1/V3) satisfies all requirements.**

Per the decision guide:

| Criterion | Assessment for US-005 |
|-----------|-----------------------|
| Structured, relational data | ✅ Fixed schema, well-defined customer record |
| ACID transactions required | ✅ Atomic partial update of customer row |
| Flexible / dynamic schema | ❌ Not needed |
| Document-style nested data | ❌ None in scope |
| High write throughput | ❌ Low-frequency self-service operation |

**Why no migration is needed:** every field that AC1 requires the GET to return (`firstName`, `lastName`, `email`, `phoneNumber`, `dateOfBirth`) and every field that AC2 permits PATCH to update (`firstName`, `lastName`, `phoneNumber`) already exists on `customers` from V1. The `password_changed_at` column required by the filter's iat-check was added in V3. No new column, index, or constraint is introduced.

**ERD (unchanged — reference):**

```
TABLE customers  (V1 + additive V2 + V3 columns; no V5 change for this story)
  id                   UUID          PRIMARY KEY
  first_name           VARCHAR(100)  NOT NULL
  last_name            VARCHAR(100)  NOT NULL
  email                VARCHAR(255)  NOT NULL UNIQUE
  phone_number         VARCHAR(20)   NOT NULL
  date_of_birth        DATE          NOT NULL
  password_hash        VARCHAR(72)   NOT NULL
  status               VARCHAR(30)   NOT NULL
  failed_login_attempts INT          NOT NULL DEFAULT 0      [V2]
  locked_at            TIMESTAMP     nullable                 [V2]
  password_changed_at  TIMESTAMPTZ   nullable                 [V3]  ← used by filter (AC6/ADR-004)
  created_at           TIMESTAMPTZ   NOT NULL
  updated_at           TIMESTAMPTZ   NOT NULL
```

---

### Communication

- **Pattern:** REST (synchronous). **No** RabbitMQ / Kafka.
- **Exchange / Topic:** None.
- **Rationale:** Both operations require immediate, consistent responses (the updated profile must be readable on the very next `GET` — AC5). Profile update is low-frequency, single-row; async processing would add latency and complexity with zero benefit.
- **Rejected alternatives:** See *Alternatives Considered*.

---

### API Endpoints (Detailed)

#### `GET /api/v1/profile`

Retrieve the authenticated customer's profile.

- **Auth:** `Authorization: Bearer <accessToken>` — ACCESS JWT required (AC6).
- **Content-Type (response):** `application/json`

**Request:** No body, no query parameters.

**200 OK (AC1):**

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

| Field | Type | Notes |
|-------|------|-------|
| `firstName` | string | From `customers.first_name` |
| `lastName` | string | From `customers.last_name` |
| `email` | string | From `customers.email` (read-only — AC3) |
| `phoneNumber` | string | From `customers.phone_number` (E.164) |
| `dateOfBirth` | string (`yyyy-MM-dd`) | From `customers.date_of_birth` (read-only — AC3) |

**401 Unauthorized (AC6) — missing or invalid ACCESS token:**

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

**401 Unauthorized (ADR-004 AC5) — token issued before password reset:**

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

---

#### `PATCH /api/v1/profile`

Partially update mutable profile fields.

- **Auth:** `Authorization: Bearer <accessToken>` — ACCESS JWT required (AC6).
- **Content-Type (request):** `application/json`
- **Idempotency:** A PATCH with the same values applied twice returns the same result — safe to retry.

**Request body — `UpdateProfileRequest`:**

All fields are individually optional. At least one known field should be present (empty-body PATCH is accepted as a no-op but callers should include at least one field by convention).

| Field | Type | Required | Validation |
|-------|------|----------|------------|
| `firstName` | string | no | If present: `@NotBlank`, ≤100 chars |
| `lastName` | string | no | If present: `@NotBlank`, ≤100 chars |
| `phoneNumber` | string | no | If present: `@PhoneNumber` E.164 `^\+[1-9]\d{1,14}$` (AC4) |

**Forbidden fields — AC3 enforcement:**

If `email` or `dateOfBirth` appear in the JSON body, the `@JsonAnySetter` sentinel method on the DTO captures them and throws `FieldNotEditableException`, which the `GlobalExceptionHandler` maps to:

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

> AC3 exact wording: `"Field is not editable"` — the `detail` value must match this string precisely so frontend and AQA tests can assert on it.

**Example request (updating all three mutable fields):**

```json
PATCH /api/v1/profile
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiI5ZjFjMmU3YS0...
Content-Type: application/json

{
  "firstName":   "Ada",
  "lastName":    "Byron",
  "phoneNumber": "+14155559999"
}
```

**Example request (updating only phone number):**

```json
PATCH /api/v1/profile
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
Content-Type: application/json

{
  "phoneNumber": "+441234567890"
}
```

**200 OK (AC2, AC5) — returns the full updated profile:**

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

The response shape is identical to `GET /api/v1/profile` — allowing the frontend to merge the updated state directly into its store.

**400 Bad Request — forbidden field present (AC3):**

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

**400 Bad Request — E.164 validation failure (AC4):**

```json
HTTP/1.1 400 Bad Request
Content-Type: application/problem+json

{
  "type":   "https://api.bank.example/problems/validation-error",
  "title":  "Validation Failed",
  "status": 400,
  "detail": "One or more fields are invalid.",
  "instance": "/api/v1/profile",
  "errors": [
    { "field": "phoneNumber", "message": "Phone number must be in E.164 format" }
  ]
}
```

**401 Unauthorized (AC6) — missing or invalid ACCESS token:** (same shape as GET 401 above)

---

### `JwtAuthenticationFilter` Design

**Class:** `com.northbank.registration.config.JwtAuthenticationFilter`  
**Extends:** `OncePerRequestFilter`  
**Injection:** `JwtConfig`, `CustomerRepository`  
**Registered:** `SecurityConfig.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)`

**Step-by-step algorithm:**

```
STEP 1 — Extract bearer token
  header = request.getHeader("Authorization")
  IF header == null OR NOT header.startsWith("Bearer ") THEN
    → write RFC 7807 401 ProblemDetail("Authentication required. Please provide a valid access token.")
    → return (do not continue filter chain)

  rawToken = header.substring(7)  // strip "Bearer "

STEP 2 — Validate as ACCESS token
  TRY:
    claims = JwtConfig.validateAccessToken(rawToken)
    // validateAccessToken() internally checks:
    //   a. HS256 signature (security.jwt.secret)
    //   b. exp > Instant.now()
    //   c. claims.get("type") == "ACCESS"
    //   d. returns Claims object on success
    customerUUID = UUID.fromString(claims.getSubject())
  CATCH (JwtException | IllegalArgumentException):
    → write RFC 7807 401 ProblemDetail("Invalid or expired access token.")
    → return

STEP 3 — Enforce password_changed_at cutoff (ADR-004 AC5)
  iat = claims.getIssuedAt()  // java.util.Date from JWT
  customer = customerRepository.findById(customerUUID)
  IF customer is empty THEN
    → write RFC 7807 401 ProblemDetail("Authentication required. Please provide a valid access token.")
    → return
  IF customer.passwordChangedAt != null
      AND iat.toInstant().isBefore(customer.passwordChangedAt) THEN
    → write RFC 7807 401 ProblemDetail("Session invalidated. Please sign in again.")
    → return

STEP 4 — Populate SecurityContext
  auth = new UsernamePasswordAuthenticationToken(
           customerUUID.toString(),   // principal (String UUID)
           null,                      // credentials
           Collections.emptyList()    // authorities (no roles in this story)
         )
  auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request))
  SecurityContextHolder.getContext().setAuthentication(auth)
  filterChain.doFilter(request, response)   // proceed
```

**`JwtConfig.validateAccessToken()` — new method spec:**

```
validateAccessToken(String rawToken): Claims
  - Uses the same secretKey bean (security.jwt.secret → HMAC-SHA256 SecretKey) as validateSessionToken()
  - Calls jjwt Jwts.parserBuilder().setSigningKey(secretKey).build().parseClaimsJws(rawToken)
  - Throws JwtException on any signature/expiry/parse failure
  - AFTER successful parse: extracts claim "type"; if != "ACCESS" throws JwtException("Token type mismatch")
  - Returns Claims on success
```

**401 error write helper (used in steps 1–3):**

The filter must write a JSON body directly to `HttpServletResponse` without going through the Spring MVC dispatcher (since the filter runs before the servlet). Use `response.setStatus(401)`, `response.setContentType("application/problem+json")`, and write the `ProblemDetail` JSON via `ObjectMapper`.

---

### `UpdateProfileRequest` DTO Field Rules (Summary)

| Field | Type | Allowed in PATCH | Validation if present |
|-------|------|------------------|-----------------------|
| `firstName` | `String` (nullable) | ✅ | `@NotBlank`, `@Size(max=100)` |
| `lastName` | `String` (nullable) | ✅ | `@NotBlank`, `@Size(max=100)` |
| `phoneNumber` | `String` (nullable) | ✅ | `@PhoneNumber` (E.164) from US-001 |
| `email` | — | ❌ FORBIDDEN | `@JsonAnySetter` sentinel → `FieldNotEditableException` → 400 `"Field is not editable"` |
| `dateOfBirth` | — | ❌ FORBIDDEN | Same sentinel |
| Any other unknown field | — | ❌ | Same sentinel |

**Partial-update semantics:** `ProfileService.updateProfile()` applies only non-null fields from the DTO to the `Customer` entity. Null (absent) fields leave the corresponding column unchanged. The full `@Validated` group annotation must be applied at the controller layer: `@Validated @RequestBody UpdateProfileRequest request`.

---

### E.164 Validation Reuse

The `@PhoneNumber` constraint annotation (package `com.northbank.registration`, created in US-001 for the registration request DTO) is applied on `UpdateProfileRequest.phoneNumber` without modification. The `PhoneNumberValidator` class remains the single source of truth for E.164 pattern enforcement (`^\+[1-9]\d{1,14}$`) across both the registration and profile-update flows. No new validator class, no regex duplication.

---

### Cross-Cutting Concerns

| Concern | Approach |
|---------|----------|
| Authentication | `JwtAuthenticationFilter` validates `type="ACCESS"` JWT on every protected request; rejects with RFC 7807 401 on any failure (AC6) |
| Authorization | Identity-scoped: customer can only read/update their own profile. The filter sets principal = customer UUID; `ProfileService` fetches by that UUID — no cross-customer access is possible |
| Error Handling | Existing `GlobalExceptionHandler` (`@RestControllerAdvice`) — RFC 7807 `ProblemDetail`; two new exception types: `FieldNotEditableException` (400) and re-use of `MethodArgumentNotValidException` (400 with `errors[]`); filter writes 401 directly via `HttpServletResponse` |
| Logging | SLF4J structured JSON; never log the raw `accessToken`, never log `phoneNumber` at DEBUG in production profile |
| Rate Limiting | Not in scope for this story — deferred to hardening story (consistent with ADR-001/ADR-003 deferrals) |
| Caching | Not applied — profile is low-read-frequency; consistency (AC5) mandates direct DB reads after PATCH |
| Idempotency | PATCH is naturally idempotent (same values → same state); no idempotency key required |
| Session Invalidation | `JwtAuthenticationFilter` enforces `iat ≥ password_changed_at` — closes the ADR-004 open checklist item |

---

### Consequences

**Positive:**
- Closes the long-standing ADR-004 open item: `password_changed_at` invalidation is now enforced at runtime by `JwtAuthenticationFilter`, protecting all current and future authenticated endpoints.
- Zero schema migration cost — all profile data already exists in `customers`.
- `@PhoneNumber` reuse avoids duplicating E.164 validation logic; single source of truth.
- `JwtAuthenticationFilter` is general-purpose: all future authenticated endpoints (`/api/v1/accounts`, `/api/v1/transfers`, etc.) are protected automatically by the same filter without additional Spring Security configuration.
- Response shape of `PATCH` matches `GET` — frontend can use one TypeScript type for both.
- Read-only field enforcement at DTO deserialization level (before service) prevents accidental or malicious mutation of email/date-of-birth at the earliest possible point.

**Negative / Trade-offs:**
- The filter loads the `customers` row on every authenticated request (to check `password_changed_at`). This is a DB read per call. For high-traffic scenarios, Redis caching of `{ customerId → passwordChangedAt }` could mitigate this — deferred to a future performance story.
- `OncePerRequestFilter` approach means 401 responses from the filter bypass the MVC error rendering path; the filter must manually serialise `ProblemDetail` JSON, which is slightly duplicative with `GlobalExceptionHandler`. This is a known Spring Security integration constraint — acceptable for this scope.
- `PATCH` semantics (null = no-update) mean a client cannot explicitly clear `firstName` to an empty string — consistent with `@NotBlank` validation which would reject empty anyway.

### Alternatives Considered

- **`@JsonIgnoreProperties(ignoreUnknown = false)` instead of `@JsonAnySetter` sentinel** — rejected for the field-not-editable requirement: `FAIL_ON_UNKNOWN_PROPERTIES` throws `HttpMessageNotReadableException` with a generic message, making it impossible to return the exact `"Field is not editable"` string required by AC3. The `@JsonAnySetter` sentinel provides per-field control.
- **Dedicated `CustomerProfileRepository` (new interface)** — rejected: the existing `CustomerRepository` already targets the `customers` table; a second repository on the same JPA entity adds noise without benefit.
- **Separate `profiles` table** — rejected: no new data is introduced; profile fields are already 1:1 with `customers`. A separate table would require a join on every GET with no normalisation benefit.
- **Async profile update via RabbitMQ/Kafka** — rejected: AC5 requires that a `GET` immediately after a `PATCH` returns the updated values; eventual consistency would break this requirement. Synchronous REST is the correct pattern per the communication decision guide.
- **PUT instead of PATCH** — rejected: PUT semantics require sending the full resource representation; requiring the client to supply `email` and `dateOfBirth` in every update request contradicts AC3 (read-only fields). PATCH with partial semantics is the correct choice.
- **Caching `GET /api/v1/profile` with Redis** — rejected for this story: AC5 mandates consistency between PATCH and subsequent GET; cache invalidation logic adds complexity without a demonstrated latency requirement. Deferred.

---

### Architect → Dev Handoff Checklist

- ✅ ADR written and **Accepted** for US-005
- ✅ Conflict check performed against ADR-001, ADR-002, ADR-003, ADR-004 — no conflicts; one additive dependency (ADR-002 `JwtConfig.validateAccessToken()`) and one open-item closure (ADR-004 `password_changed_at` enforcement)
- ✅ Persistence strategy decided: **no new migration** — all required columns exist in `customers` (V1/V3); next available migration remains V5
- ✅ DB schema confirmed (existing `customers` table ERD reproduced with column-source annotations)
- ✅ API endpoints listed with HTTP method, path, auth requirement, and description
- ✅ Request payload shape specified for `PATCH /api/v1/profile` (field table + forbidden-field rule + partial-update semantics)
- ✅ Response payload shape specified for both endpoints (identical `ProfileResponse` shape)
- ✅ All error response shapes specified (400 validation, 400 field-not-editable, 401 unauthenticated, 401 session-invalidated)
- ✅ `JwtAuthenticationFilter` algorithm fully specified (4-step: extract → validate → iat-check → set context)
- ✅ `JwtConfig.validateAccessToken()` method contract specified
- ✅ `UpdateProfileRequest` DTO rules specified (allowed fields, forbidden fields, `@JsonAnySetter` sentinel approach, partial-update semantics)
- ✅ E.164 validation strategy: reuse `@PhoneNumber` from US-001 — no new validator
- ✅ Package structure specified: `com.northbank.registration.profile.{ProfileController, ProfileService, ProfileResponse, UpdateProfileRequest}`
- ✅ Spring Security config changes specified (additive; existing `permitAll()` paths preserved; profile paths gain `authenticated()`)
- ✅ Messaging topology: N/A — REST only
- ✅ External integrations: N/A
- ✅ Error handling strategy defined (RFC 7807; two new exception types: `FieldNotEditableException`, `InvalidAccessTokenException`; filter writes 401 directly)
- ✅ Cross-cutting concerns addressed (auth, authorization, logging, rate-limiting deferral, idempotency, caching deferral)
- ✅ ADR-004 open handoff item closed: `JwtAuthenticationFilter` enforces `iat ≥ password_changed_at` at runtime
