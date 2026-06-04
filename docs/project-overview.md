# NorthBank Project Overview

## Project Description

NorthBank is a full stack digital banking application that supports customer onboarding, secure authentication, account management, internal transfers, fraud operations, customer notifications, and platform administration.

The project is organized around product epics and user stories. Each epic delivers a vertical slice across backend APIs, persistence, security, frontend screens, and tests. The current application models three primary user groups:

- Customer: registers, signs in with two-factor authentication, manages a profile, opens and views accounts, transfers funds, reviews transaction history, and receives notifications.
- Bank Admin: manages customer bank accounts, views all transactions, manages customer user accounts, unlocks locked customers, deactivates customers, and reviews audit logs.
- Fraud Analyst: manages fraud rules, reviews fraud alerts, and resolves blocked transactions.

The system is intentionally built as a modular monolith: one Spring Boot backend and one React frontend, with package boundaries by business capability. This keeps the project understandable for a small team while preserving clear seams for future extraction if the domain grows.

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

## Implemented Epics and User Flows

### EPIC-01: User Identity and Authentication

#### US-001: Customer Self-Registration

Flow:

1. A new customer opens the registration screen.
2. The customer enters first name, last name, email, phone number, date of birth, and password.
3. The frontend validates form structure and password rules.
4. The backend validates the request again, normalizes the email address, checks duplicate email, hashes the password with BCrypt, and stores a new `customers` row.
5. The customer starts in `PENDING_VERIFICATION`.
6. The API returns only the created customer ID.

Key decisions:

- Passwords are never stored or logged in plaintext.
- Email uniqueness is enforced both in service logic and with a database unique constraint.
- Customer creation is the root aggregate for later stories.

#### US-002: Customer Login

Flow:

1. A returning customer submits email and password.
2. The backend normalizes email and checks credentials using BCrypt.
3. If credentials are valid, the failed-login counter is reset.
4. The backend issues a short-lived `SESSION` JWT and creates an OTP session for US-003.
5. The frontend transitions the customer to OTP verification.
6. Failed login attempts increment a counter.
7. After five consecutive failures, the customer status becomes `LOCKED`.

Key decisions:

- Login is step one of a two-factor flow, not a full authenticated session.
- Lockout state lives on the `customers` table for transactional consistency.
- Login success and failure are captured in the audit log.

#### US-003: Two-Factor Authentication via SMS

Flow:

1. After password verification, the customer receives an OTP through the SMS service abstraction.
2. The customer enters the six-digit code.
3. The backend validates the `SESSION` token, OTP session hash, expiration, invalidation flag, and failed-attempt count.
4. On success, the OTP session is invalidated to prevent replay.
5. The backend issues an access token and refresh token.
6. The frontend stores the access token and routes to the dashboard.
7. The customer can request an OTP resend after the configured cooldown.

Key decisions:

- The raw session token is not stored; a SHA-256 hash is persisted.
- OTP sessions expire and are invalidated after repeated failures.
- Access tokens include role information so admin and analyst endpoints can use Spring Security role checks.

#### US-004: Password Reset

Flow:

1. The customer requests a password reset using an email address.
2. The backend returns a neutral success response to prevent account enumeration.
3. A reset token is generated and stored as a hash.
4. The customer submits the reset token and new password.
5. The backend validates the token, hashes the new password, updates `password_changed_at`, and invalidates the reset token.
6. Existing access tokens issued before `password_changed_at` are rejected by the JWT filter.

Key decisions:

- Reset tokens are opaque and hash-stored.
- Token invalidation is based on `password_changed_at`.
- Password validation reuses the registration password policy.

#### US-005: View and Update Customer Profile

Flow:

1. An authenticated customer opens the profile screen.
2. The frontend calls `GET /api/v1/profile`.
3. The backend resolves the customer ID from the access token.
4. The customer can update first name, last name, and phone number.
5. Email and date of birth remain immutable.

Key decisions:

- Profile endpoints are protected by the shared JWT filter.
- Immutable fields are guarded at the service layer.
- Phone format validation reuses the registration validator.

### EPIC-02: Account Management

#### US-006: Open Bank Account

Flow:

1. An authenticated customer chooses an account type.
2. The backend verifies the customer does not already own that account type.
3. The backend generates a unique account number and creates the account with an initial active status.
4. The new account is returned to the frontend.

Key decisions:

- Account numbers are generated server-side.
- Duplicate account type per customer is blocked.
- Account creation is audited as `ACCOUNT_OPENED`.

#### US-007: View Account List

Flow:

1. The authenticated customer opens the dashboard.
2. The frontend calls the account list endpoint.
3. The backend returns only accounts owned by the authenticated customer.
4. The frontend displays balances, account types, account numbers, and statuses.

Key decisions:

- Account list data is scoped by authenticated customer ID.
- The dashboard acts as the operational landing page after OTP verification.

#### US-008: View Account Details

Flow:

1. The customer selects one account from the account list.
2. The frontend routes to the account detail page.
3. The backend loads the account and verifies ownership.
4. Unauthorized access to another customer's account returns 403.
5. Missing accounts return 404.

Key decisions:

- Ownership checks happen before returning sensitive account details.
- Account detail and account list use separate response DTOs.

#### US-009: Admin Manage Customer Bank Accounts

Flow:

1. A Bank Admin opens the admin account management page.
2. The frontend calls `GET /api/v1/admin/accounts`.
3. The admin can filter accounts by customer ID, account type, and account status.
4. The admin can deactivate an active account.
5. The admin can reactivate an inactive account.
6. Non-admin users receive 403.

Key decisions:

- Admin account endpoints require `ROLE_ADMIN`.
- Account lifecycle actions are audited as `ACCOUNT_DEACTIVATED` and `ACCOUNT_ACTIVATED`.
- Admin account listing batch-loads customer owners to avoid N+1 reads.

### EPIC-03: Transaction Management

#### US-010: Internal Fund Transfer

Flow:

1. A customer starts a transfer between two owned accounts.
2. The backend verifies source and destination accounts are different.
3. The backend locks both accounts in deterministic ID order.
4. The backend verifies both accounts belong to the customer and are active.
5. The backend verifies sufficient funds.
6. A transaction is created with `PENDING_EVALUATION`.
7. Fraud evaluation is invoked.
8. If fraud does not block the transfer, balances are updated and the transaction becomes `COMPLETED`.
9. If fraud blocks the transfer, balances are unchanged and the transaction becomes `BLOCKED`.

Key decisions:

- Row-level locking avoids concurrent transfer races.
- Account IDs are sorted before locking to reduce deadlock risk.
- Transfer submission, blocked transfers, and completed transfers are audited.

#### US-011: View Transaction History

Flow:

1. A customer opens transaction history.
2. The frontend calls the transaction list endpoint with optional filters.
3. The backend returns only transactions owned by the authenticated customer.
4. The customer can filter by account, type, status, and date range.

Key decisions:

- Customer transaction queries are always scoped by authenticated customer ID.
- Filtering is implemented with JPA specifications.

#### US-012: View Transaction Details

Flow:

1. A customer opens a transaction detail page.
2. The backend verifies that the transaction belongs to the authenticated customer.
3. The detail response includes transaction metadata, account references, amount, status, and timestamps.

Key decisions:

- Unauthorized or missing transaction access is intentionally returned as not found to avoid data leakage.

#### US-013: Admin View Transactions

Flow:

1. A Bank Admin opens the transaction overview.
2. The frontend calls `GET /api/v1/admin/transactions`.
3. The admin can filter across all customer transactions.
4. The admin can open transaction detail pages.
5. Non-admin users receive 403.

Key decisions:

- Admin transaction endpoints are separate from customer endpoints.
- The same transaction DTOs are reused where appropriate.

### EPIC-04: Fraud Detection

#### US-014: Manage Fraud Rules

Flow:

1. A Fraud Analyst opens the fraud rules page.
2. The analyst creates rules such as amount thresholds, transaction frequency rules, or unusual-hour rules.
3. The analyst can update active state or threshold values.
4. The analyst can delete rules through soft deletion.

Key decisions:

- Fraud rule deletion uses status rather than physical removal.
- The system prevents deleting the last active rule.
- Fraud rule create, update, and delete actions are audited.

#### US-015: Auto Detect and Block Fraud

Flow:

1. A transfer is submitted.
2. Fraud evaluation checks the transaction against active rules.
3. If a rule matches, the transaction is marked `BLOCKED`.
4. A fraud alert is created for analyst review.
5. Account balances are not moved for blocked transactions.

Key decisions:

- Fraud evaluation is modeled behind a port so detection rules can evolve without rewriting transaction orchestration.
- Blocked transfers are kept as transactions for traceability.

#### US-016: Customer Fraud Notification

Flow:

1. When fraud detection creates a relevant customer notification, it is stored in the notification table.
2. The authenticated customer opens the notification inbox.
3. The backend returns only notifications for the authenticated customer.

Key decisions:

- Notifications are customer-scoped.
- Notification state is separated from transaction and fraud alert state.

#### US-017: Analyst Review Alerts

Flow:

1. A Fraud Analyst opens the alert queue.
2. The analyst filters and selects alerts.
3. The backend returns alert detail with the related transaction data.

Key decisions:

- Alert summary and alert detail use separate DTOs.
- Analyst endpoints require `ROLE_FRAUD_ANALYST`.

#### US-018: Analyst Unblock or Reject Transaction

Flow:

1. A Fraud Analyst opens a blocked alert.
2. The analyst approves or rejects the blocked transaction.
3. On approval, the backend verifies sufficient funds and applies the transfer.
4. On rejection, the transaction becomes `REJECTED`.
5. The alert is marked reviewed.

Key decisions:

- Approval rechecks source funds at the time of analyst action.
- Analyst decisions are audited as `TRANSACTION_UNBLOCKED` or `TRANSACTION_REJECTED`.

### EPIC-05: Platform Administration

#### US-019: Admin Manage Customer User Accounts

Flow:

1. A Bank Admin opens the customer administration page.
2. The frontend calls `GET /api/v1/admin/customers`.
3. The backend returns paginated customer records with ID, full name, email, phone, status, and creation timestamp.
4. The admin can filter customers by `ACTIVE`, `LOCKED`, or `INACTIVE`.
5. The admin can deactivate a customer.
6. Deactivation sets customer status to `INACTIVE`, resets lockout state, and updates `password_changed_at`.
7. Existing access tokens for that customer are rejected by the JWT filter.
8. The admin can unlock a locked customer, setting status back to `ACTIVE`.
9. Attempts to deactivate another admin account return 403 with `Cannot deactivate an admin account`.
10. Non-admin users receive 403.

Key decisions:

- Customer administration is separated from public customer registration.
- Deactivation invalidates active sessions without needing a server-side access-token store.
- Customer deactivation and unlock actions are audited.

#### US-020: Admin View System Audit Log

Flow:

1. A Bank Admin opens the audit log page.
2. The frontend calls `GET /api/v1/admin/audit-logs`.
3. The backend returns paginated audit entries with ID, actor ID, actor role, action type, target entity type, target entity ID, timestamp, and IP address.
4. The admin can filter by actor ID, action type, date from, and date to.
5. Attempts to mutate audit logs with PUT, PATCH, or DELETE return 405.
6. Non-admin users receive 403.

Key decisions:

- Audit logs are append-only at the API level.
- Audit writes run in their own transaction so a successfully completed action has a durable compliance record.
- Request IP is captured from `X-Forwarded-For` when present, otherwise from the request remote address.

## Backend Architecture

### Runtime

- Java 21
- Spring Boot 3.3.5
- Spring MVC for REST APIs
- Spring Security for authentication and authorization
- Spring Data JPA and Hibernate for persistence
- PostgreSQL 16
- Flyway for schema migrations
- JJWT 0.12.x for session and access JWTs
- Lombok for boilerplate reduction
- MapStruct for DTO mapping where useful
- springdoc-openapi for Swagger and OpenAPI documentation

### Package Structure

Backend code is organized by business capability:

- `customer`: registration, customer entity, customer repository, admin customer management
- `auth`: login, OTP, password reset, token persistence, SMS and email abstractions
- `profile`: authenticated profile read and update
- `account`: customer bank accounts and admin account management
- `transaction`: transfers, transaction history, transaction details, admin transaction views
- `fraud`: fraud rules, fraud alerts, analyst decisions
- `notification`: customer notification inbox
- `audit`: audit action model, audit log entity, audit service, admin audit API
- `config`: security, JWT, password encoder, test infrastructure
- `shared`: shared security and exception support

### Layering

Each feature generally follows this structure:

- Controller: HTTP contract, request parameters, authentication principal, response status.
- Service: business rules, transactional boundaries, orchestration, audit recording.
- Repository: persistence access through Spring Data JPA.
- Domain model: JPA entity and enums.
- DTOs: request and response records used at the API boundary.
- Exception handler: RFC 7807-style `ProblemDetail` responses for domain errors.

### Security Architecture

Authentication uses a two-step flow:

1. `SESSION` JWT after password verification.
2. `ACCESS` JWT after OTP verification.

The `JwtAuthenticationFilter`:

- Skips public registration, login, OTP, password-reset, Swagger, and API docs paths.
- Requires `Authorization: Bearer <token>` on protected endpoints.
- Validates token signature, expiry, and token type.
- Resolves the customer from the token subject.
- Rejects missing or inactive customers.
- Rejects tokens issued before `password_changed_at`.
- Adds Spring Security authorities from token role claims and customer role fallback.

Authorization rules:

- Customer APIs require authentication.
- Admin APIs require `ROLE_ADMIN`.
- Fraud analyst APIs require `ROLE_FRAUD_ANALYST`.
- Unknown endpoints are denied by default.

### Persistence Architecture

The database schema is versioned through Flyway migrations:

- `V1`: customers
- `V2`: login lockout columns
- `V3`: password reset
- `V4`: accounts
- `V5`: OTP and refresh tokens
- `V6`: transactions
- `V7`: fraud detection
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

JPA uses `ddl-auto=validate`, so the database schema is controlled by Flyway rather than Hibernate auto-generation.

### Audit Architecture

Audit logging is implemented through an internal `AuditLogService` and `audit_logs` table.

Captured action types:

- `LOGIN_SUCCESS`
- `LOGIN_FAILURE`
- `LOGOUT`
- `ACCOUNT_OPENED`
- `ACCOUNT_DEACTIVATED`
- `ACCOUNT_ACTIVATED`
- `TRANSFER_SUBMITTED`
- `TRANSFER_BLOCKED`
- `TRANSFER_COMPLETED`
- `FRAUD_RULE_CREATED`
- `FRAUD_RULE_UPDATED`
- `FRAUD_RULE_DELETED`
- `TRANSACTION_UNBLOCKED`
- `TRANSACTION_REJECTED`
- `CUSTOMER_DEACTIVATED`
- `CUSTOMER_UNLOCKED`

Audit entries include:

- Actor ID
- Actor role
- Action type
- Target entity type
- Target entity ID
- Timestamp
- IP address

Audit logs are exposed through read-only admin APIs. PUT, PATCH, and DELETE routes intentionally return 405.

## Frontend Architecture

### Runtime

- React 18
- TypeScript
- Vite
- React Router
- TanStack Query
- Axios
- React Hook Form
- Zod
- Tailwind CSS

### Frontend Structure

The frontend is organized by feature:

- `registration`
- `auth`
- `profile`
- `dashboard`
- `accounts`
- `transactions`
- `fraud`
- `notifications`
- `admin`
- `shared`

Routing is centralized in `frontend/src/app/router.tsx`.

API clients live near the feature that owns the UI. The newer feature clients use the shared Axios instance in `frontend/src/shared/api/client.ts`, which attaches the access token from local storage.

### UX and UI Decisions

- Customer journeys are simple and task-focused: registration, login, OTP, dashboard, profile, accounts, transfers, transactions, and notifications.
- Admin and fraud tools use dense tables, compact filters, and clear action controls because they are operational workflows.
- Protected role-specific pages are routed directly; backend security remains the source of truth for authorization.
- Error states are shown inline with retry controls where appropriate.
- Loading states use skeleton-like placeholder blocks to avoid layout jumps.

## API Surface Summary

Public endpoints:

- `POST /api/v1/customers`
- `POST /api/v1/auth/login`
- `POST /api/v1/auth/verify-otp`
- `POST /api/v1/auth/resend-otp`
- `POST /api/v1/auth/forgot-password`
- `POST /api/v1/auth/reset-password`

Authenticated customer endpoints:

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

## Testing Strategy

Backend:

- Unit tests use JUnit 5, AssertJ, and Mockito.
- Integration tests use Spring Boot, MockMvc, PostgreSQL Testcontainers, and the test profile.
- `IntegrationTestBase` starts one PostgreSQL container per test JVM.
- Test data is isolated through transactional rollback.
- Mockito is configured with the subclass mock maker in test resources to avoid local JDK self-attach issues with the inline mock maker.

Frontend:

- TypeScript compilation is used as the current static safety check.
- TanStack Query boundaries keep data loading and mutation state explicit.

Verified commands used during EPIC-005:

```bash
env JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-21.jdk/Contents/Home mvn test -DskipTests
node node_modules/typescript/bin/tsc --noEmit
env JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-21.jdk/Contents/Home mvn test -Dtest=AuthServiceTest,CustomerServiceTest,ProfileServiceTest,OtpServiceTest
```

Local limitation:

```bash
env JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-21.jdk/Contents/Home mvn test -Dtest=AdminCustomerControllerIT,AdminAuditLogControllerIT
```

This integration-test command requires Docker because the project uses Testcontainers. It cannot run in environments where `/var/run/docker.sock` is unavailable.

## Deployment and Local Development

The project includes a Docker Compose setup for the complete local stack:

- PostgreSQL on port `5432`
- Backend on port `8080`
- Frontend on port `3000`

Typical local startup:

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

## Architecture Decisions Summary

- Modular monolith over microservices to keep delivery fast and local development simple.
- Feature-oriented packages to preserve ownership boundaries.
- PostgreSQL as the system of record.
- Flyway controls all schema changes.
- JPA specifications support flexible filtering without duplicating query methods.
- JWT access tokens keep the API stateless.
- OTP and reset tokens are hash-stored to reduce token leakage risk.
- `password_changed_at` invalidates old access tokens after password reset or admin deactivation.
- Role-based endpoint protection is enforced in Spring Security and reinforced with `@PreAuthorize`.
- Admin and customer endpoints are separated to avoid accidental overexposure.
- Audit logging is append-only through public API design and persisted for compliance.
- Transaction account locking uses deterministic account ordering to reduce deadlock risk.
- Fraud evaluation is behind a port so future fraud logic can evolve independently from transfer orchestration.
- Frontend state fetching uses TanStack Query for cache, loading, error, and mutation states.
- Form validation uses React Hook Form and Zod for predictable user feedback.
