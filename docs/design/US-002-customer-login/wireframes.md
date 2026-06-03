# Wireframes — US-002: Customer Login

Components referenced (PascalCase): `LoginPage`, `LoginForm`, `TextField`, `PasswordField`,
`FormErrorBanner`, `SubmitButton`, `VerificationHandoff`, `LockedAccountNotice`, `TrustBadge`.

Legend: `[____]` input · `(•)` focus ring · `~spinner~` loading · `✓` valid · `!` error ·
`👁` show-password toggle.

---

## 1. Login Form — Desktop (centered card)

```
+--------------------------------------------------------------------------+
| HEADER:  🔒 NorthBank                          [ Create account ]        |
+--------------------------------------------------------------------------+
|                                                                          |
|                 +--------------------------------------------+           |
|                 |  Sign in to NorthBank                       |  <- H1   |
|                 |  Enter your details to continue.            |  <- sub  |
|                 |                                             |          |
|                 |  Email address                              |          |
|                 |  [_______________________________________] |          |
|                 |                                             |          |
|                 |  Password                              👁    |          |
|                 |  [_______________________________________] |          |
|                 |                            Forgot password? |  <- 2°   |
|                 |                                             |          |
|                 |  [             Sign in            ]         |  <- 1°   |
|                 |                                             |          |
|                 |  New to NorthBank?  Create an account       |  <- 2°   |
|                 |                                             |          |
|                 |  🔒 Your connection is encrypted and secure.| TrustBadge|
|                 +--------------------------------------------+           |
|                                                                          |
+--------------------------------------------------------------------------+
| FOOTER:  © 2026 NorthBank · Privacy · Terms                              |
+--------------------------------------------------------------------------+
```

- Card max-width ~400–460px, centered. Single column even on desktop (only two fields).
- One primary action ("Sign in"); "Forgot password?" and "Create an account" are secondary links.
- "Forgot password?" sits directly under the password field, right-aligned — found exactly where failure happens.

---

## 2. Login Form — Mobile (single column, stacked)

```
+----------------------------------+
| 🔒 NorthBank   [ Create account ]|
+----------------------------------+
|                                  |
|  Sign in to NorthBank            |  H1
|  Enter your details to continue. |  sub
|                                  |
|  Email address                   |
|  [____________________________]  |
|                                  |
|  Password                   👁   |
|  [____________________________]  |
|                 Forgot password? |  2° (right-aligned)
|                                  |
|  [          Sign in          ]   |  full-width 1°
|                                  |
|  New to NorthBank?               |
|  Create an account               |  2°
|                                  |
|  🔒 Your connection is encrypted.|  TrustBadge
+----------------------------------+
| © 2026 NorthBank · Privacy       |
+----------------------------------+
```

- Single column; full-width primary button (≥44px tall touch target).
- Labels persist above inputs (never placeholder-only).
- Email field uses email keyboard; password supports manager autofill (`current-password`).

---

## 3. Invalid Credentials Error State (AC3 — 401)

```
+----------------------------------+
|  Sign in to NorthBank            |
|                                  |
|  ! Invalid email or password.    |  <- FormErrorBanner (role="alert")
|    Please try again.             |     single, generic, non-field-specific
|                                  |
|  Email address                   |
|  [ maria.p@northmail.com _____]  |  <- preserved (NOT flagged as the culprit)
|                                  |
|  Password                   👁   |
|  [____________________________]  |  <- CLEARED + re-focused
|                 Forgot password? |  <- recovery path emphasised
|                                  |
|  [          Sign in          ]   |
|                                  |
|  New to NorthBank? Create account|
+----------------------------------+
```

- One form-level message only — neither field shows a "this one was wrong" marker (anti-enumeration, AC3).
- Email preserved; password cleared and focused so retry is one action.
- Message text fixed by AC3: "Invalid email or password" (with a gentle "Please try again.").

---

## 3b. Approaching Lockout Warning (AC4 — pre-lock)

```
+----------------------------------+
|  Sign in to NorthBank            |
|                                  |
|  ! Invalid email or password.    |  <- FormErrorBanner (role="alert")
|    For your security, your        |
|    account will be locked after  |
|    2 more failed attempts.       |  <- attempts-remaining warning (AC4)
|                                  |
|  Email address                   |
|  [ maria.p@northmail.com _____]  |
|                                  |
|  Password                   👁   |
|  [____________________________]  |  <- cleared
|                 Forgot password? |  <- safer route, emphasised
|                                  |
|  [          Sign in          ]   |
+----------------------------------+
```

- The count ("2 more") is shown only if the API exposes remaining attempts; otherwise a
  generic warning is used (see ux-flow open question). Number is illustrative.

---

## 4. Submitting State (form disabled, button spinner)

```
+----------------------------------+
|  Sign in to NorthBank            |
|                                  |
|  Email address                   |
|  [ maria.p@northmail.com _____]  (disabled, dimmed)
|                                  |
|  Password                        |
|  [ ••••••••••• ______________]  (disabled, dimmed)
|                 Forgot password? (disabled)
|                                  |
|  [   ~spinner~  Signing in…   ]   |  <- SubmitButton: spinner + label,
|                                  |     aria-busy="true", disabled
|  New to NorthBank? Create account (disabled)
+----------------------------------+
```

- Entire form non-interactive to prevent double submission (`aria-busy` on form).
- Button keeps its width (no layout shift); label changes to "Signing in…".

---

## 5. Verification Hand-off State (AC2 — 200 `2FA_REQUIRED`)

```
+----------------------------------+
| 🔒 NorthBank                     |
+----------------------------------+
|                                  |
|           ~spinner~              |  <- progress indicator (aria-busy)
|                                  |
|      Verifying it's you…         |  <- H1 / live status
|                                  |
|  We're sending a code to keep    |
|  your account safe. Hang on a    |  <- sets expectation for US-003
|  moment.                         |
|                                  |
+----------------------------------+
| © 2026 NorthBank                 |
+----------------------------------+
```

- Replaces the form on success. `sessionToken` is held in transient state — NEVER displayed.
- Auto-advances to the US-003 verification (OTP) screen. *This is where US-002 ends.*
- If the hand-off itself fails to route, a fallback "Continue to verification" button appears (no dead-end).

---

## 6. Locked Account State (AC4 / AC5 — 423)

```
+----------------------------------+
| 🔒 NorthBank                     |
+----------------------------------+
|                                  |
|            ( 🔒 )                |  <- lock icon (decorative, aria-hidden)
|                                  |
|     Your account is locked       |  <- H1
|                                  |
|  For your security, we locked    |
|  your account after too many     |  <- explains AC4 in human terms
|  failed sign-in attempts.        |
|                                  |
|  To unlock it, please contact    |
|  NorthBank support. Our team     |  <- recovery (admin unlock, US-019)
|  will verify you and restore     |
|  access.                         |
|                                  |
|  [      Contact support      ]   |  <- primary action (no dead-end)
|                                  |
|  Forgot password?  ·  Back to    |
|  sign in                         |  <- secondary links
+----------------------------------+
| © 2026 NorthBank                 |
+----------------------------------+
```

- The Email/Password inputs are removed so a locked user cannot keep hammering the endpoint.
- Even correct credentials cannot proceed from here (AC5) — only an admin unlock restores access.
- "Back to sign in" lets the user return once they believe the account is unlocked.

---

## Component map

| Component | Purpose | Key props | Events |
|-----------|---------|-----------|--------|
| `LoginPage` | Route container for `/login`; orchestrates form / handoff / locked states | — | — |
| `LoginForm` | Hosts email + password fields, validation, submit | `onSubmit(values)`, `isSubmitting`, `formError`, `attemptsRemaining?` | `onSubmit`, `onFieldBlur` |
| `TextField` | Reusable labelled text input (shared w/ US-001) | `label`, `name`, `value`, `error`, `autocomplete`, `disabled` | `onChange`, `onBlur` |
| `PasswordField` | Password input + show/hide toggle (shared w/ US-001) | `label`, `value`, `error`, `disabled`, `isVisible`, `autocomplete="current-password"` | `onChange`, `onBlur`, `onToggleVisibility` |
| `FormErrorBanner` | Form-level error/warning banner (shared w/ US-001) | `message`, `variant: 'error' \| 'warning'` | — |
| `SubmitButton` | Primary action w/ loading state (shared w/ US-001) | `label`, `loadingLabel`, `isLoading`, `disabled` | `onClick` |
| `VerificationHandoff` | Transitional success state → routes to US-003 | `onContinue` | `onContinue` |
| `LockedAccountNotice` | Locked-account explanation + recovery | `supportHref`, `onBackToSignIn` | `onContactSupport`, `onBackToSignIn` |
| `TrustBadge` | Encryption reassurance line (shared w/ US-001) | `text` | — |

> Reuse note: `TextField`, `PasswordField`, `FormErrorBanner`, `SubmitButton`, and `TrustBadge`
> are carried over from US-001 — recognition over recall and consistent vocabulary.
