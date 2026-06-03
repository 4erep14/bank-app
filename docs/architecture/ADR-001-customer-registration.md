## ADR-001: Customer Self-Registration

**Story:** US-001
**Status:** Accepted

---

### Context

US-001 introduces the first persisted aggregate of the banking platform: the **Customer**. A new (anonymous) customer must be able to self-register by submitting personal details (first name, last name, email, phone number, date of birth, password) and, on success, receive a newly created customer `id`. The created customer must start in `PENDING_VERIFICATION` (email verification is a later story, out of scope).

This ADR establishes the foundational decisions downstream stories build on: the persistence technology and `customers` schema; the synchronous REST contract; how passwords are protected at rest; where validation is enforced (client + server); and cross-cutting error handling (RFC 7807) for validation (400) and email-uniqueness (409). This is a regulated (banking) domain — data is structured, relational, and requires strong consistency. There are no existing ADRs to conflict with.

### Decision

Implement registration as a single **synchronous REST** endpoint `POST /api/v1/customers`, backed by **PostgreSQL** via Spring Data JPA, inside a **modular monolith**. No messaging or external integrations in this story.

1. **Persistence = PostgreSQL (SQL).** Structured relational identity data needing ACID + a hard uniqueness constraint on email.
2. **Communication = REST only.** Immediate request/reply (new `id` + validation feedback). No async in scope.
3. **Password security = BCrypt** (`BCryptPasswordEncoder`, strength 12) applied **before** persistence. Plaintext never stored or logged.
4. **Validation = defense in depth.** Client-side for UX; authoritative server-side via Bean Validation + custom `@Password` + E.164 pattern.
5. **Uniqueness enforced twice.** DB `UNIQUE` on `email` (race-safe source of truth) + pre-save service check returning friendly `409`.
6. **Errors = RFC 7807 Problem Details** via a global `@RestControllerAdvice`.

### Persistence

**Choice: PostgreSQL (SQL).**

| Criterion | Assessment for US-001 |
|-----------|-----------------------|
| Structured, relational data | ✅ Fixed, well-defined record |
| ACID transactions required | ✅ Banking domain; atomic registration |
| Unique constraint on email | ✅ Race-safe via UNIQUE index |
| Flexible/dynamic schema | ❌ Stable schema |
| Document-style nested data | ❌ None in scope |
| High write throughput / time-series | ❌ Low frequency |

**Table:** `customers` (single table).

| Column | Type | Constraints | Notes |
|--------|------|-------------|-------|
| `id` | `UUID` | `PRIMARY KEY`, default `gen_random_uuid()` | Returned on 201 (AC6) |
| `first_name` | `VARCHAR(100)` | `NOT NULL` | |
| `last_name` | `VARCHAR(100)` | `NOT NULL` | |
| `email` | `VARCHAR(255)` | `NOT NULL`, `UNIQUE` | Lower-cased; uniqueness (AC2) |
| `phone_number` | `VARCHAR(20)` | `NOT NULL` | E.164 string (AC4) |
| `date_of_birth` | `DATE` | `NOT NULL` | Must be in the past |
| `password_hash` | `VARCHAR(72)` | `NOT NULL` | BCrypt hash only |
| `status` | `VARCHAR(30)` | `NOT NULL`, default `'PENDING_VERIFICATION'` | `CustomerStatus` enum (AC5) |
| `created_at` | `TIMESTAMPTZ` | `NOT NULL`, default `now()` | Audit |
| `updated_at` | `TIMESTAMPTZ` | `NOT NULL`, default `now()` | Audit |

**Indexes / constraints:**
- `PRIMARY KEY (id)`
- `CONSTRAINT uq_customers_email UNIQUE (email)` → backing unique B-tree index (also serves lookups).
- `CONSTRAINT chk_customers_status CHECK (status IN ('PENDING_VERIFICATION','ACTIVE','SUSPENDED','CLOSED'))`.

> `password_hash` sized 72: BCrypt output is 60 chars; never holds plaintext.

**`CustomerStatus` enum** (`@Enumerated(EnumType.STRING)`): `PENDING_VERIFICATION` (initial, AC5), `ACTIVE`, `SUSPENDED`, `CLOSED`. Only `PENDING_VERIFICATION` set by this story.

**Flyway migration:** `V1__create_customers_table.sql`

```sql
-- V1__create_customers_table.sql
-- Story: US-001 — Customer Self-Registration
CREATE TABLE customers (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    first_name     VARCHAR(100) NOT NULL,
    last_name      VARCHAR(100) NOT NULL,
    email          VARCHAR(255) NOT NULL,
    phone_number   VARCHAR(20)  NOT NULL,
    date_of_birth  DATE         NOT NULL,
    password_hash  VARCHAR(72)  NOT NULL,
    status         VARCHAR(30)  NOT NULL DEFAULT 'PENDING_VERIFICATION',
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_customers_email UNIQUE (email),
    CONSTRAINT chk_customers_status
        CHECK (status IN ('PENDING_VERIFICATION', 'ACTIVE', 'SUSPENDED', 'CLOSED'))
);
```

### Communication

- **Pattern:** REST (synchronous). **No** RabbitMQ / Kafka.
- **Exchange/Topic:** None.
- **Contract:** Plain HTTP request/response (see API Endpoints + `api-contracts.md`).
- **Rationale:** Result must return immediately; no downstream consumer in scope. Email verification will get its own ADR (likely async).

### API Endpoints

| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/v1/customers` | Register a new customer; returns `201` with the new `id` |

**Auth:** Public (anonymous). Spring Security `permitAll()` for this path; deny by default elsewhere.

**Request body (`application/json`):**

| Field | Type | Server-side validation |
|-------|------|------------------------|
| `firstName` | string | `@NotBlank`, ≤100 |
| `lastName` | string | `@NotBlank`, ≤100 |
| `email` | string | `@NotBlank`, `@Email`, ≤255; normalized lower-case (AC2) |
| `phoneNumber` | string | `@NotBlank`, E.164 `^\+[1-9]\d{1,14}$` (AC4) |
| `dateOfBirth` | string `yyyy-MM-dd` | `@NotNull`, `@Past` |
| `password` | string | `@NotBlank` + `@Password`: ≥8, ≥1 upper, ≥1 lower, ≥1 digit, ≥1 special (AC3); write-only |

**201 Created:** `{ "id": "<uuid>" }` (AC6), plus `Location: /api/v1/customers/{id}`.

**Errors (RFC 7807 `application/problem+json`):** 400 (field-level `errors[]`), 409 (`"Email already registered"`), 500 (generic).

**OpenAPI:** `@Operation` + `@ApiResponses` (201/400/409/500); `password` write-only and excluded from response schemas.

### Cross-Cutting Concerns

| Concern | Approach |
|---------|----------|
| Authentication | Public endpoint; `permitAll()`, deny by default elsewhere |
| Authorization | None for registration |
| Password security | BCrypt (strength 12) in service before save; plaintext never persisted/logged |
| Error handling | Global `@RestControllerAdvice` → RFC 7807 `ProblemDetail`; handles `MethodArgumentNotValidException` (400), `EmailAlreadyRegisteredException` (409), `DataIntegrityViolationException` on `uq_customers_email` (defensive 409), catch-all (500) |
| Validation | Client-side (UX) + server-side (authoritative) |
| Uniqueness race safety | DB `UNIQUE` is source of truth; service pre-check returns friendly 409 |
| Logging | SLF4J structured + correlation IDs; never log `password`/`password_hash` |
| Rate limiting | Recommended (Bucket4j) — deferred to follow-up story |
| Idempotency | Not required; natural via email uniqueness (duplicates → 409) |

**Server-side validation rules per field:** as table above (AC2/AC3/AC4 enforced server-side).

### Consequences

**Positive:** Strong consistency + race-safe email uniqueness; simple, testable sync contract; secure BCrypt baseline reused by future identity stories; consistent RFC 7807 errors for clean frontend mapping; enum/schema leaves room for activation stories.

**Negative / Trade-offs:** Redundant pre-check + DB constraint (justified); future verification needs a follow-up async ADR; rate limiting deferred leaves public endpoint exposed; PII obligations (encryption-at-rest, retention) must be handled at infra level.

### Alternatives Considered

- **MongoDB (NoSQL)** — rejected: structured relational identity needs ACID + hard unique constraint; no benefit, complicates consistency.
- **Async registration (RabbitMQ/Kafka)** — rejected: caller needs `id`/validation immediately; no consumer in scope. Revisit for verification story.
- **Plaintext/reversible password storage** — rejected: violates security baseline. BCrypt chosen (salted, adaptive) over plain SHA-256; Argon2 noted as future option.
- **App-only uniqueness check (no DB constraint)** — rejected: check-then-act race; DB `UNIQUE` is authoritative.
- **Returning full customer on 201** — rejected: AC6 needs only `id`; minimize PII exposure.

### Architect → Dev Handoff Checklist

- [x] ADR written and **Accepted** for US-001
- [x] Persistence strategy decided (PostgreSQL / SQL)
- [x] DB schema defined (DDL + Flyway `V1__create_customers_table.sql`)
- [x] API endpoint listed (method, path, description)
- [x] Request/response shapes specified (incl. 400/409/500)
- [x] Messaging topology — N/A
- [x] External integrations — N/A
- [x] Error-handling strategy defined (RFC 7807; 400 + 409 + 500)
- [x] Security defined (public endpoint, BCrypt, write-only password)
- [x] Non-functional notes captured (rate limiting deferred, PII flagged)
