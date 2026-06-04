# NorthBank Project Overview

## Project Description

NorthBank is a full-stack digital banking application for customer onboarding, secure login, bank account management, internal transfers, fraud detection, customer notifications, and platform administration.

The system is implemented as a modular monolith: one Spring Boot backend, one React frontend, and one PostgreSQL database. Backend packages are split by business capability, and frontend code is split by feature. This keeps local development simple while preserving clear boundaries for future service extraction.

Primary users:

- Customer: registers, signs in with two-factor authentication, manages profile data, opens accounts, views balances, transfers funds, reviews transactions, and reads fraud notifications.
- Bank Admin: manages customer bank accounts, views transactions across the platform, manages customer users, unlocks locked customers, deactivates customers, and reviews audit logs.
- Fraud Analyst: manages fraud rules, reviews fraud alerts, and approves or rejects blocked transactions.

## Demo Data

The backend seeds demo data on startup when `northbank.demo.seed-enabled=true`, which defaults to `true` through `NORTHBANK_DEMO_SEED_ENABLED`. The `test` profile disables seeding.

Admin account:

| Email | Password | Purpose |
| --- | --- | --- |
| `admin@northbank.test` | `AdminPass123!` | Bank Admin account for customer, account, transaction, and audit management |

Customer accounts:

| Email | Password | Status | Seeded balances |
| --- | --- | --- | --- |
| `maria.garcia@northbank.test` | `CustomerPass123!` | `ACTIVE` | Checking `$4,825.75`, Savings `$18,420.00` |
| `david.chen@northbank.test` | `CustomerPass123!` | `ACTIVE` | Checking `$2,380.40`, Savings `$8,200.00` |
| `sarah.patel@northbank.test` | `CustomerPass123!` | `LOCKED` | Frozen checking `$380.25`, Savings `$1,500.00` |
| `leo.martin@northbank.test` | `CustomerPass123!` | `INACTIVE` | Inactive checking `$94.12` |

## Repository Layout

```text
bank-app/
  backend/    Java 21, Spring Boot 3, PostgreSQL, Flyway, Spring Security
  frontend/   React 18, TypeScript, Vite, TanStack Query, Axios, Tailwind CSS
  docs/       Project documentation
  docker-compose.yml
```

Important backend packages:

- `customer`: registration, customer status, customer roles, admin customer operations.
- `auth`: login, OTP, password reset, refresh tokens, JWT support, SMS/email abstractions.
- `profile`: authenticated profile read/update.
- `account`: customer accounts and admin account lifecycle operations.
- `transaction`: transfers, transaction history, transaction details, admin transaction views.
- `fraud`: fraud rules, fraud alerts, analyst review, and blocked-transaction resolution.
- `notification`: customer notification inbox.
- `audit`: audit log entity, action types, audit service, and admin audit API.
- `config`: security, JWT filter, password encoder, and runtime configuration.
- `shared`: reusable security and exception helpers.

## Implemented User Flows

### EPIC-01: User Identity and Authentication

#### US-001: Customer Self-Registration

1. Customer opens `/register`.
2. Customer submits first name, last name, email, phone number, date of birth, and password.
3. Frontend validates the form.
4. Backend validates again, normalizes email, checks duplicate email, hashes the password with BCrypt, and inserts a `customers` row.
5. Customer starts as `PENDING_VERIFICATION`.
6. API returns the created customer ID and the frontend shows `/register/success`.

Decisions: plaintext passwords are never stored; BCrypt strength 12 is used; email uniqueness is enforced in service logic and with a database constraint.

#### US-002: Customer Login

1. Customer opens `/login` and submits email/password.
2. Backend normalizes email and validates credentials.
3. On success, failed-login state is reset.
4. Backend creates an OTP session and issues a short-lived `SESSION` JWT.
5. Frontend routes to `/verify-otp`.
6. Failed attempts increment a counter; five consecutive failures lock the customer.

Decisions: password login is only the first authentication step; lockout state is stored on the customer record; login success/failure is audited.

#### US-003: Two-Factor Authentication via SMS

1. Backend sends a six-digit OTP through the SMS abstraction.
2. Customer enters the code on `/verify-otp`.
3. Backend validates the session JWT, OTP hash, expiration, invalidation state, and attempt count.
4. Successful verification invalidates the OTP session.
5. Backend returns access and refresh tokens.
6. Frontend stores the access token and routes to `/dashboard`.
7. Customer can request OTP resend after the configured cooldown.

Decisions: raw OTP session tokens are hash-stored; OTP sessions expire and cannot be replayed; access tokens carry role information.

#### US-004: Password Reset

1. Customer opens `/forgot-password` and submits email.
2. Backend returns a neutral response to avoid account enumeration.
3. Reset token is generated and stored as a hash.
4. Customer opens `/reset-password?token=...` and submits a new password.
5. Backend validates the token, hashes the new password, updates `password_changed_at`, and invalidates the reset token.
6. Old access tokens issued before `password_changed_at` are rejected.
7. Frontend shows success or invalid-token screens.

Decisions: reset tokens are opaque and hash-stored; password policy is reused; token invalidation avoids a server-side access-token store.

#### US-005: View and Update Customer Profile

1. Authenticated customer opens `/profile`.
2. Frontend calls `GET /api/v1/profile`.
3. Backend resolves the customer from the access token.
4. Customer can update first name, last name, and phone number through `PATCH /api/v1/profile`.
5. Email and date of birth are immutable.

Decisions: profile endpoints require an access token; immutable fields are enforced in the service layer.

### EPIC-02: Account Management

#### US-006: Open Bank Account

1. Authenticated customer opens the dashboard and starts the open-account modal.
2. Customer chooses `CHECKING` or `SAVINGS`.
3. Frontend submits `POST /api/v1/accounts`.
4. Backend checks that the customer does not already own that account type.
5. Backend generates a unique account number and creates an active account with zero balance.
6. Frontend refreshes the account list.

Decisions: account numbers are server-generated; duplicate account type per customer is blocked; account creation is audited.

#### US-007: View Account List

1. Customer opens `/dashboard`.
2. Frontend calls `GET /api/v1/accounts`.
3. Backend returns only accounts owned by the authenticated customer.
4. Frontend displays type, account number, balance, status, and detail navigation.

Decisions: account list data is always scoped by authenticated customer ID; dashboard is the main customer landing page.

#### US-008: View Account Details

1. Customer selects an account from the dashboard.
2. Frontend routes to `/accounts/:accountId`.
3. Backend loads the account and verifies ownership.
4. Account details are returned only to the owner.
5. Unauthorized access returns 403; missing accounts return 404.

Decisions: ownership checks happen before sensitive data is returned; list and detail DTOs are separate.

#### US-009: Admin Manage Customer Bank Accounts

1. Admin opens `/admin/accounts`.
2. Frontend calls `GET /api/v1/admin/accounts`.
3. Admin filters by customer ID, account type, and account status.
4. Admin deactivates accounts with `PATCH /api/v1/admin/accounts/{id}/deactivate`.
5. Admin reactivates accounts with `PATCH /api/v1/admin/accounts/{id}/activate`.
6. Non-admin users receive 403.

Decisions: admin endpoints require `ROLE_ADMIN`; account activation/deactivation is audited; owner data is batch-loaded to avoid N+1 queries.

### EPIC-03: Transaction Management

#### US-010: Internal Fund Transfer

1. Customer opens `/transfer`.
2. Customer selects source and destination accounts, amount, and optional description.
3. Frontend submits `POST /api/v1/transactions/transfer`.
4. Backend rejects same-account transfers.
5. Backend locks both accounts using pessimistic write locking in deterministic UUID order.
6. Backend verifies ownership, active status, and sufficient funds.
7. Backend creates a transaction in `PENDING_EVALUATION`.
8. Fraud evaluation runs.
9. If allowed, balances are moved and transaction becomes `COMPLETED`.
10. If blocked, balances remain unchanged and transaction becomes `BLOCKED`.

Decisions: account locks prevent race conditions; deterministic lock ordering reduces deadlock risk; transfer submitted/completed/blocked events are audited.

#### US-011: View Transaction History

1. Customer opens `/transactions`.
2. Frontend calls `GET /api/v1/transactions` with optional filters.
3. Backend returns only the authenticated customer's transactions.
4. Customer can filter by account, type, status, and date range.

Decisions: customer queries are scoped by token identity; JPA specifications implement flexible filtering.

#### US-012: View Transaction Details

1. Customer opens `/transactions/:transactionId`.
2. Backend verifies that the transaction belongs to the authenticated customer.
3. Detail response includes transaction IDs, account references, amount, status, description, and timestamps.

Decisions: unauthorized transaction detail access is hidden as not found to reduce data leakage.

#### US-013: Admin View Transactions

1. Admin opens `/admin/transactions`.
2. Frontend calls `GET /api/v1/admin/transactions`.
3. Admin filters across all customer transactions.
4. Admin opens `/admin/transactions/:transactionId` for details.
5. Non-admin users receive 403.

Decisions: admin transaction visibility is implemented through separate admin endpoints.

### EPIC-04: Fraud Detection

#### US-014: Manage Fraud Rules

1. Fraud Analyst opens `/fraud/rules`.
2. Analyst creates rules with `POST /api/v1/fraud/rules`.
3. Supported condition types are `AMOUNT_EXCEEDS`, `TRANSACTION_FREQUENCY`, and `UNUSUAL_HOUR`.
4. Analyst updates rules with `PATCH /api/v1/fraud/rules/{id}`.
5. Analyst deletes rules with `DELETE /api/v1/fraud/rules/{id}`.

Decisions: rule deletion is soft deletion; rule names are unique; the last active rule cannot be deleted; rule changes are audited.

#### US-015: Auto Detect and Block Fraud

1. Transfer is submitted.
2. Fraud evaluation loads active rules by creation time.
3. First matching rule blocks the transaction.
4. Transaction is marked `BLOCKED`.
5. Fraud alert and customer notification are created.
6. Account balances are not moved.

Decisions: fraud evaluation is behind `FraudEvaluationPort`; blocked transfers remain stored for traceability.

#### US-016: Customer Fraud Notification

1. Fraud detection creates a `TRANSACTION_BLOCKED` notification.
2. Customer opens `/notifications`.
3. Frontend calls `GET /api/v1/notifications`.
4. Backend returns only notifications for the authenticated customer.

Decisions: notifications are customer-scoped and separated from transaction and fraud-alert state.

#### US-017: Analyst Review Alerts

1. Fraud Analyst opens `/fraud/alerts`.
2. Analyst filters and selects alerts.
3. Frontend calls `GET /api/v1/fraud/alerts`.
4. Analyst opens `/fraud/alerts/:alertId`.
5. Backend returns alert detail with related transaction data.

Decisions: alert summary and detail DTOs are separate; analyst endpoints require `ROLE_FRAUD_ANALYST`.

#### US-018: Analyst Unblock or Reject Transaction

1. Fraud Analyst opens a blocked alert.
2. Analyst approves or rejects the transaction.
3. Approval rechecks source funds and applies the transfer.
4. Rejection marks the transaction `REJECTED`.
5. Alert is marked reviewed.

Decisions: analyst decisions are audited; a blocked transaction cannot be resolved repeatedly.

### EPIC-05: Platform Administration

#### US-019: Admin Manage Customer User Accounts

1. Admin opens `/admin/customers`.
2. Frontend calls `GET /api/v1/admin/customers`.
3. Backend returns paginated customer records.
4. Admin filters by `ACTIVE`, `LOCKED`, or `INACTIVE`.
5. Admin deactivates customers with `PATCH /api/v1/admin/customers/{id}/deactivate`.
6. Deactivation sets status to `INACTIVE`, resets lockout state, and updates `password_changed_at`.
7. Admin unlocks locked customers with `PATCH /api/v1/admin/customers/{id}/unlock`.
8. Admin accounts cannot be deactivated by this flow.

Decisions: customer administration is separate from public registration; deactivation invalidates active tokens; admin actions are audited.

#### US-020: Admin View System Audit Log

1. Admin opens `/admin/audit-logs`.
2. Frontend calls `GET /api/v1/admin/audit-logs`.
3. Backend returns paginated audit entries.
4. Admin filters by actor ID, action type, date from, and date to.
5. PUT, PATCH, and DELETE attempts return 405.
6. Non-admin users receive 403.

Decisions: audit logs are append-only through the API; audit writes use a separate transaction; actor role, target entity, timestamp, and IP address are recorded.

## Frontend Route Map

Public/auth routes:

- `/` -> redirects to `/login`
- `/login`
- `/register`
- `/register/success`
- `/verify-otp`
- `/forgot-password`
- `/forgot-password/sent`
- `/reset-password`
- `/reset-password/success`
- `/reset-password/error`

Customer routes:

- `/dashboard`
- `/profile`
- `/accounts/:accountId`
- `/transfer`
- `/transactions`
- `/transactions/:transactionId`
- `/notifications`

Admin routes:

- `/admin/accounts`
- `/admin/transactions`
- `/admin/transactions/:transactionId`
- `/admin/customers`
- `/admin/audit-logs`

Fraud analyst routes:

- `/fraud/rules`
- `/fraud/alerts`
- `/fraud/alerts/:alertId`

## API Surface Summary

Public endpoints:

- `POST /api/v1/customers`
- `POST /api/v1/auth/login`
- `POST /api/v1/auth/verify-otp`
- `POST /api/v1/auth/resend-otp`
- `POST /api/v1/auth/forgot-password`
- `POST /api/v1/auth/reset-password`

Customer endpoints:

- `GET /api/v1/profile`
- `PATCH /api/v1/profile`
- `POST /api/v1/accounts`
- `GET /api/v1/accounts`
- `GET /api/v1/accounts/{id}`
- `POST /api/v1/transactions/transfer`
- `GET /api/v1/transactions`
- `GET /api/v1/transactions/{id}`
- `GET /api/v1/notifications`

Admin endpoints:

- `GET /api/v1/admin/accounts`
- `PATCH /api/v1/admin/accounts/{id}/deactivate`
- `PATCH /api/v1/admin/accounts/{id}/activate`
- `GET /api/v1/admin/transactions`
- `GET /api/v1/admin/transactions/{id}`
- `GET /api/v1/admin/customers`
- `PATCH /api/v1/admin/customers/{id}/deactivate`
- `PATCH /api/v1/admin/customers/{id}/unlock`
- `GET /api/v1/admin/audit-logs`

Fraud analyst endpoints:

- `GET /api/v1/fraud/rules`
- `POST /api/v1/fraud/rules`
- `PATCH /api/v1/fraud/rules/{id}`
- `DELETE /api/v1/fraud/rules/{id}`
- `GET /api/v1/fraud/alerts`
- `GET /api/v1/fraud/alerts/{id}`
- `POST /api/v1/fraud/alerts/{id}/approve`
- `POST /api/v1/fraud/alerts/{id}/reject`

Documentation endpoints:

- `GET /swagger-ui.html`
- `GET /api-docs`

## Technologies Used

Backend:

- Java 21
- Maven
- Spring Boot 3.3.5
- Spring MVC
- Spring Security
- Spring Data JPA and Hibernate
- Jakarta Bean Validation
- PostgreSQL JDBC driver
- Flyway
- JJWT 0.12.6
- BCrypt through Spring Security Crypto
- Lombok
- MapStruct 1.6.2
- springdoc-openapi 2.6.0
- ProblemDetail / RFC 7807-style error responses

Frontend:

- React 18.3.1
- TypeScript 5.5.3
- Vite 5.4.8
- React Router 6
- TanStack Query 5
- Axios
- React Hook Form
- Zod
- Tailwind CSS 3
- PostCSS and Autoprefixer
- Nginx for Docker runtime serving

Database and runtime:

- PostgreSQL 16
- Flyway SQL migrations
- Docker Compose
- Multi-stage Docker builds
- Nginx SPA fallback and `/api/*` proxy

Testing:

- JUnit 5
- Spring Boot Test
- Spring Security Test
- MockMvc
- Testcontainers PostgreSQL
- AssertJ and Mockito
- TypeScript compiler checks

## Backend Architecture

### Layering

Each feature generally follows this structure:

- Controller: HTTP contract, request validation, authentication principal, response status, and OpenAPI annotations.
- Service: business rules, transactional boundaries, orchestration, and audit recording.
- Repository: Spring Data JPA access and specifications.
- Domain model: JPA entities, enums, and domain events.
- DTOs: request and response models at the API boundary.
- Exception handlers: domain-specific errors mapped to `ProblemDetail` responses.

Services avoid exposing JPA entities directly to controllers.

### Security Architecture

Authentication is two-step:

1. `SESSION` JWT after password verification.
2. `ACCESS` JWT after OTP verification.

The JWT filter skips public registration, login, OTP, password-reset, Swagger, and API-doc paths. Protected endpoints require `Authorization: Bearer <token>`. The filter validates token type, signature, expiry, customer status, and `password_changed_at`.

Authorization rules:

- Customer APIs require authentication.
- Admin APIs require `ROLE_ADMIN`.
- Fraud analyst APIs require `ROLE_FRAUD_ANALYST`.
- Unknown endpoints are denied by default.
- CSRF is disabled and sessions are stateless.

### Persistence Architecture

Flyway migrations:

- `V1`: customers
- `V2`: login lockout columns
- `V3`: password reset
- `V4`: accounts
- `V5`: OTP and refresh tokens
- `V6`: transactions
- `V7`: fraud rules, fraud alerts, notifications
- `V8`: customer roles and inactive account status
- `V9`: platform administration and audit logs

Important tables:

- `customers`
- `accounts`
- `transactions`
- `otp_sessions`
- `refresh_tokens`
- `password_reset_tokens`
- `fraud_rules`
- `fraud_alerts`
- `notifications`
- `audit_logs`

Hibernate uses `ddl-auto=validate`, so Flyway owns schema changes. Database constraints protect uniqueness, valid statuses, positive amounts, different transfer accounts, and foreign-key relationships.

### Domain Model Summary

- Customer: identity, contact details, status, role, password hash, failed-login state, and token invalidation timestamp.
- Account: customer-owned `CHECKING` or `SAVINGS` account with account number, balance, and lifecycle status.
- Transaction: customer transfer between source and destination accounts with amount, status, description, and timestamps.
- Fraud rule: unique rule name, condition type, threshold, active flag, and soft-delete status.
- Fraud alert: transaction-linked alert with rule snapshot and review state.
- Notification: customer-facing blocked-transaction notification.
- Audit log: actor, role, action type, target entity, timestamp, and IP address.

### Transaction and Fraud Architecture

Transfers run in a transaction. Source and destination accounts are loaded with pessimistic write locks and sorted by UUID before locking. This protects balances from concurrent writes and lowers deadlock risk.

Fraud evaluation is behind `FraudEvaluationPort`. The current implementation is synchronous and rule-based. Supported checks are amount threshold, transaction frequency, and unusual-hour range. A blocked transfer creates a fraud alert and notification without moving funds.

### Audit Architecture

Audit logging uses `AuditLogService` and the `audit_logs` table. Audit writes use a separate transaction. The admin audit API is read-only; mutation methods return 405.

Captured actions include login success/failure, account open/activate/deactivate, transfer submit/block/complete, fraud rule create/update/delete, analyst unblock/reject, customer deactivation, and customer unlock.

## Frontend Architecture

Frontend code is feature-oriented. Routes are centralized in `frontend/src/app/router.tsx`. Feature API clients live near their screens; newer clients use `frontend/src/shared/api/client.ts`.

State and API communication:

- Axios reads `VITE_API_BASE_URL`, defaulting to `http://localhost:8080`.
- The Axios interceptor attaches the access token from local storage.
- TanStack Query manages server-state loading, cache, errors, and mutations.

Forms and UX:

- React Hook Form manages form state.
- Zod validates frontend schemas.
- Backend validation remains authoritative.
- Customer flows are task-focused.
- Admin and fraud screens use dense tables, filters, and explicit action controls.

## Testing Strategy

Backend:

- Unit tests cover service rules and edge cases.
- Integration tests use Spring Boot, MockMvc, Spring Security Test, and PostgreSQL Testcontainers.
- Test data is isolated through transactional rollback where applicable.
- Mockito uses the subclass mock maker in test resources.

Frontend:

- `npm run typecheck` runs `tsc --noEmit`.
- `npm run build` runs TypeScript compilation and Vite production build.

Useful commands:

```bash
cd backend
mvn test

cd ../frontend
npm install
npm run typecheck
npm run build
```

Integration tests that use Testcontainers require Docker access.

## Deployment and Local Development

Docker Compose services:

- PostgreSQL 16 on `5432`
- Backend on `8080`
- Frontend through Nginx on `3000`

Startup:

```bash
docker compose up --build
```

Useful URLs:

- Frontend: `http://localhost:3000`
- Backend: `http://localhost:8080`
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI docs: `http://localhost:8080/api-docs`

Required backend environment values:

- `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`
- `SECURITY_JWT_SECRET`

Optional frontend environment value:

- `VITE_API_BASE_URL`

Runtime decisions:

- Backend listens on port `8080`.
- JDBC time zone is UTC.
- Hibernate SQL logging is disabled by default.
- JSON responses omit null values.
- Stack traces and exception details are not exposed in default error responses.

## Architecture Decisions Summary

- Modular monolith over microservices for faster delivery and simple local development.
- Feature-oriented backend packages and frontend folders.
- PostgreSQL as the system of record.
- Flyway controls schema changes; Hibernate validates only.
- UUID identifiers for core aggregates.
- Database constraints reinforce service-layer validation.
- BCrypt protects customer passwords.
- Two-step authentication separates password verification from final access-token issuance.
- JWT access tokens keep the API stateless.
- OTP and reset tokens are hash-stored.
- `password_changed_at` invalidates old access tokens after password reset or admin deactivation.
- Role-based authorization is enforced by Spring Security.
- Admin, fraud analyst, and customer APIs are separated.
- Pessimistic account locking protects transfer consistency.
- Fraud evaluation uses a port so detection logic can evolve independently.
- Blocked transfers are persisted for traceability.
- Audit logs are append-only through API design and persisted for compliance.
- TanStack Query handles frontend server state.
- React Hook Form and Zod provide predictable form validation.
- Nginx serves the React SPA and proxies backend API traffic in Docker.
