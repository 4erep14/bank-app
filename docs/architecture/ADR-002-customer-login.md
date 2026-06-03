## ADR-002: Customer Login

**Story:** US-002
**Status:** Accepted

---

### Context

US-002 introduces the **first authentication step** of the platform. A registered customer submits email + password to `POST /api/v1/auth/login`. This story does **not** complete login — it is the first leg of a two-factor flow. On valid credentials the system returns `2FA_REQUIRED` plus a short-lived **sessionToken** that US-003 (OTP verification) will exchange for a full access token. The story must also defend against credential-stuffing/brute-force via account lockout after 5 consecutive failed attempts.

This builds directly on the US-001 baseline and **must remain compatible** with it:

- `customers` table (V1) already exists with `password_hash` (BCrypt) and a `status` column constrained by `chk_customers_status`.
- `CustomerStatus` enum currently has `PENDING_VERIFICATION, ACTIVE, SUSPENDED, CLOSED`.
- Spring Security is configured stateless-ready: `permitAll()` for `POST /api/v1/customers`, deny by default, **no JWT yet**.
- Package root is `com.northbank.registration`; next Flyway migration is `V2__*.sql`.

This ADR therefore decides: (1) how the temporary sessionToken is represented and correlated with US-003; (2) where lockout state lives; (3) how passwords are verified; (4) the JWT library + claim design; (5) how Spring Security is wired while keeping the REST pattern; and (6) the new endpoint contract.

**Conflict check vs ADR-001:** No conflicts. This ADR *extends* the `customers` table (additive V2 migration) and *adds* a new `LOCKED` status value to the existing CHECK constraint — it does not alter US-001 columns, the email uniqueness rule, or the BCrypt baseline. ADR-001's RFC 7807 global error handling is reused.

---

### Decision

Implement login as a single **synchronous REST** endpoint `POST /api/v1/auth/login`, backed by the existing **PostgreSQL** `customers` table (extended via V2), inside the existing modular monolith. No messaging or external integrations in this story.

1. **sessionToken = short-lived signed JWT (stateless).** 5-minute expiry, custom claim `type=SESSION`, subject = customer `id`. **Not** persisted — verified by signature + expiry only.
2. **Lockout state = persisted on `customers`.** Add `failed_login_attempts INT NOT NULL DEFAULT 0` and `locked_at TIMESTAMP` (nullable) via `V2`. Add `LOCKED` to `chk_customers_status`.
3. **Password verification = BCrypt `PasswordEncoder.matches()`.** Reuse the US-001 `BCryptPasswordEncoder` bean. **No Spring Security form login** — credentials are checked explicitly inside the service from the REST controller.
4. **JWT library = `io.jsonwebtoken:jjwt` (jjwt 0.12.x).** `jjwt-api` + runtime `jjwt-impl` + `jjwt-jackson`. HMAC-SHA256 signing with a secret from configuration/env.
5. **Spring Security stays stateless.** Provide a `DaoAuthenticationProvider`-backed `AuthenticationManager` bean, but **invoke authentication from a plain `@Service`** (`AuthService`) called by `AuthController`. `SecurityConfig` remains `SessionCreationPolicy.STATELESS`, CSRF disabled, `permitAll()` added for `POST /api/v1/auth/login`.
6. **Errors = RFC 7807 Problem Details** via the existing global `@RestControllerAdvice`.

---

### Persistence (V2 migration changes)

**Choice: extend the existing PostgreSQL `customers` table.** No new table — lockout counters are 1:1 with a customer, low-cardinality, and must be read/written atomically with the same row during a login attempt.

**New columns on `customers`:**

| Column | Type | Constraints | Notes |
|--------|------|-------------|-------|
| `failed_login_attempts` | `INT` | `NOT NULL`, default `0` | Reset to `0` on success; incremented on each failure (AC4) |
| `locked_at` | `TIMESTAMP` | nullable | Set when the account transitions to `LOCKED`; null otherwise |

**CHECK constraint update:** `chk_customers_status` is dropped and recreated to add `'LOCKED'`:

```
status IN ('PENDING_VERIFICATION','ACTIVE','SUSPENDED','CLOSED','LOCKED')
```

**`CustomerStatus` enum** gains a new value `LOCKED`. Migration file: `V2__add_login_lockout_columns.sql`.

---

### Communication (REST only)

- **Pattern:** REST (synchronous). **No** RabbitMQ / Kafka.
- **Rationale:** The caller needs an immediate `2FA_REQUIRED` decision + sessionToken to proceed to US-003.

---

### API Endpoints

| Method | Path | Request | Response |
|--------|------|---------|----------|
| POST | `/api/v1/auth/login` | `{ "email": string, "password": string }` | `200` `{ "status": "2FA_REQUIRED", "sessionToken": "<jwt>" }` · `401` problem · `423` problem |

**Auth:** Public (anonymous). Add `permitAll()` for `POST /api/v1/auth/login`.

**200 OK (AC2):**
```json
{ "status": "2FA_REQUIRED", "sessionToken": "<short-lived-jwt>" }
```

**401 Unauthorized (AC3)** — RFC 7807, `detail`: `"Invalid email or password"`.

**423 Locked (AC4, AC5)** — RFC 7807, `detail`: `"Account locked due to too many failed login attempts"`.

---

### sessionToken Design

| Aspect | Value |
|--------|-------|
| Format | Signed JWT (JWS), HMAC-SHA256 (`HS256`) |
| Expiry (`exp`) | **5 minutes** from issue |
| `sub` | customer `id` (UUID) |
| Custom claim | **`type` = `"SESSION"`** |
| Signing key | HMAC secret from config/env (`security.jwt.secret`) |
| Storage | **None** — stateless |

---

### Account Lockout Design

**Algorithm (executed atomically within one `@Transactional` service method):**

1. Look up customer by normalized email. **Not found →** return `401`.
2. If `status = LOCKED` → return `423` immediately (AC5).
3. Verify password:
   - **Match:** Reset `failed_login_attempts = 0`. Return `200` (AC2).
   - **No match:** Increment `failed_login_attempts`. If `>= 5` → set `status = LOCKED`, `locked_at = now()`, return `423` (AC4). Else → return `401` (AC3).

---

### Consequences

**Positive:**
- Additive, backward-compatible V2 migration.
- Stateless sessionToken needs no new infra/table.
- Atomic single-row lockout — strong consistency in a banking context.
- Establishes the JWT foundation (`jjwt`, `type` claim convention) that US-003 will extend.

**Negative / Trade-offs:**
- Stateless sessionToken cannot be revoked before 5-min `exp`.
- No auto-unlock/self-service unlock in this story.
- No IP-based throttling yet.

### Alternatives Considered

- **DB/Redis-stored opaque sessionToken** — rejected: unnecessary for a 5-minute single-use handshake.
- **Spring Security form login** — rejected: imposes non-REST flow; keep explicit REST endpoint.
- **Separate `account_lockout` table** — rejected: 1:1 with customer, adds a join without benefit.
- **Long-lived sessionToken** — rejected: enlarges risk window for a token that only authorizes the 2FA step.

---

### Architect → Dev Handoff Checklist

- [x] ADR written and **Accepted** for US-002
- [x] Conflicts with existing ADRs checked (ADR-001) — none
- [x] Persistence strategy decided (extend PostgreSQL `customers`; SQL)
- [x] DB schema defined (V2 DDL in `data-model-us002.md`)
- [x] API endpoint listed with request/response shapes (200/400/401/423)
- [x] sessionToken design specified (stateless JWT, 5-min, `type=SESSION`)
- [x] Account lockout design specified (5-attempt threshold, atomic algorithm)
- [x] JWT library decided (`io.jsonwebtoken:jjwt` 0.12.x, HS256)
- [x] Spring Security wiring decided (stateless; `permitAll` for login path)
- [x] Error-handling strategy defined (RFC 7807; reuse global advice)
- [x] **US-004 dependency:** JWT filter MUST enforce `iat >= password_changed_at` (AC5 of US-004)