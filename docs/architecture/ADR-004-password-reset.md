## ADR-004: Password Reset

**Story:** US-004
**Status:** Accepted

---

### Context

US-004 lets a customer who has forgotten their password regain access without contacting support. The flow is two public REST calls:

1. **Request** — customer submits email; if it matches, a reset token is generated, stored with 1-hour expiry, and (stub) emailed. Response is **always** `200` (anti-enumeration, AC1/AC2).
2. **Confirm** — customer submits token + new password; on success the password is updated, token consumed, and **all active sessions invalidated** (AC3–AC5, AC7).

This ADR builds on the US-001 baseline:
- `customers` table and BCrypt encoder already exist (V1).
- `@Password` annotation + `PasswordValidator` already exist — **reused as-is**.
- US-002 (parallel) owns Flyway **V2**. This story owns Flyway **V3**.
- These are **public endpoints** — no authentication required.

**Cross-story dependency:** AC5 (session invalidation) requires the JWT filter (US-002) to honor a `password_changed_at` cutoff. US-004 provides the data; US-002 must enforce it.

---

### Decision

Two synchronous public REST endpoints under `/api/v1/auth`, backed by **PostgreSQL** via Spring Data JPA.

1. **Token storage = new `password_reset_tokens` table.** Store only the **SHA-256 hash** of the raw token. Single-use via `used` flag + expiry.
2. **Token generation = `SecureRandom` 32 bytes → Base64URL → 43-char token.** Verification re-hashes incoming token with SHA-256 and looks up by hash.
3. **Session invalidation = `password_changed_at` cutoff on `customers`.** Any JWT with `iat < password_changed_at` is rejected by the US-002 JWT filter.
4. **Email = `EmailService` interface + `StubEmailService`** that logs the reset link via SLF4J. `@Primary` for `dev`/`test` profiles.
5. **Anti-enumeration = uniform `200` response** regardless of whether email exists.
6. **Errors = RFC 7807 Problem Details** via existing global `@RestControllerAdvice`.

---

### Persistence (V3 migration)

**New table `password_reset_tokens`:**

| Column | Type | Constraints | Notes |
|--------|------|-------------|-------|
| `id` | `UUID` | `PRIMARY KEY` | |
| `customer_id` | `UUID` | `NOT NULL`, FK → `customers(id)` ON DELETE CASCADE | |
| `token_hash` | `VARCHAR(64)` | `NOT NULL`, `UNIQUE` | SHA-256 hex (64 chars) |
| `expires_at` | `TIMESTAMPTZ` | `NOT NULL` | `created_at + 1 hour` |
| `used` | `BOOLEAN` | `NOT NULL`, default `FALSE` | Single-use flag |
| `created_at` | `TIMESTAMPTZ` | `NOT NULL`, default `now()` | Audit |

**Add column to `customers`:**

| Column | Type | Constraints | Notes |
|--------|------|-------------|-------|
| `password_changed_at` | `TIMESTAMPTZ` | `NULL` | Session-invalidation cutoff (AC5) |

Migration file: `V3__password_reset.sql`

---

### Communication (REST only — public endpoints)

- **Pattern:** REST (synchronous). No messaging in this story.
- Email delivery abstracted behind `EmailService` (stub in dev/test).

---

### API Endpoints

| Method | Path | Request | Response |
|--------|------|---------|----------|
| POST | `/api/v1/auth/forgot-password` | `{ "email": string }` | `200 OK` — always (AC1) |
| POST | `/api/v1/auth/reset-password` | `{ "token": string, "newPassword": string }` | `200 OK` (AC4); `400` invalid token (AC6/AC7); `400` weak password (AC3) |

**`POST /api/v1/auth/forgot-password` — always 200:**
```json
{ "message": "If an account exists for that email, a password reset link has been sent." }
```

**`POST /api/v1/auth/reset-password` — 200 success:**
```json
{ "message": "Your password has been reset. Please sign in with your new password." }
```

**400 invalid/expired/used token (AC6/AC7):**
```json
{
  "type": "https://api.bank.example/problems/invalid-reset-token",
  "title": "Invalid Reset Token",
  "status": 400,
  "detail": "Invalid or expired reset token"
}
```

---

### Reset Token Design

**Generation:**
1. Look up customer by normalized email.
2. If found: generate 32-byte `SecureRandom` → Base64URL (43 chars) raw token. SHA-256 → `token_hash`. Insert row (`expires_at = now() + 1h`, `used = false`). Call `EmailService.sendPasswordResetEmail(email, link)`.
3. If not found: do nothing.
4. Always return same `200` body.

**Verification:**
1. `incomingHash = SHA-256(request.token)`
2. Look up by `token_hash`. Reject if: no row, `used = true`, or `expires_at < now()` → `400 "Invalid or expired reset token"`.
3. On valid: update `password_hash` (BCrypt), set `password_changed_at = now()`, set `used = true` — all in one `@Transactional` call.

---

### Session Invalidation Design (AC5)

Add `customers.password_changed_at` (V3). On successful reset, set to `now()`. US-002 JWT filter checks: if `iat < password_changed_at` → reject `401`. `NULL` = no restriction.

---

### Email Stub Design

```java
public interface EmailService {
    void sendPasswordResetEmail(String email, String resetLink);
}
```

`StubEmailService` logs via SLF4J at `INFO`. Annotated `@Primary` and `@Profile({"dev","test"})`.

---

### Consequences

**Positive:**
- DB leak does not expose usable tokens (hash-at-rest).
- Anti-enumeration uniform responses (AC1).
- Stateless global logout via one timestamp (AC5).
- Reuses BCrypt encoder, `@Password` validator, RFC 7807 handler.

**Negative / Trade-offs:**
- AC5 enforcement depends on US-002 JWT filter honoring `password_changed_at`.
- Expired/used tokens accumulate — periodic cleanup recommended.

### Alternatives Considered

- **Store raw token** — rejected: DB leak would expose live reset tokens.
- **BCrypt-hash the token** — rejected: salted slow hash breaks indexed lookup; SHA-256 correct for high-entropy input.
- **JWT as reset token** — rejected: AC7 (single-use) forces server-side state anyway.
- **Server-side session blacklist for AC5** — rejected: `password_changed_at` achieves global logout statelessly.
- **Distinct errors for expired vs used vs invalid** — rejected: leaks token state to attackers.

---

### Architect → Dev Handoff Checklist

- [x] ADR written and **Accepted** for US-004
- [x] Persistence strategy decided (PostgreSQL / SQL)
- [x] DB schema defined (V3 in `data-model-us004.md`)
- [x] API endpoints listed with request/response shapes (200/400)
- [x] Anti-enumeration contract specified (AC1)
- [x] Token design specified: `SecureRandom` 32B → Base64URL 43-char; SHA-256 hash at rest; 1h TTL; single-use
- [x] Session-invalidation design: `password_changed_at` cutoff (AC5)
- [x] Email stub design specified
- [x] Password policy reuse confirmed: `@Password` + `PasswordValidator` + BCrypt from US-001
- [x] Error handling: RFC 7807; `InvalidResetTokenException` → 400
- [ ] **US-002 dependency confirmed:** JWT filter enforces `iat >= password_changed_at` (AC5 runtime)