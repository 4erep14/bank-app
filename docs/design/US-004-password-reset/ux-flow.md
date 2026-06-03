# UX Flow — US-004: Password Reset

> Covers AC1–AC7. The journey spans **two screens** bridged by an **out-of-band email step**.
> Email delivery is stubbed this iteration, but the UX is designed as if real.

---

## High-level User Journey

```
[Login screen (US-002)]
   │  taps "Forgot password?"
   ▼
SCREEN 1 ──────────────► SCREEN 2 (Confirmation) ──► (user leaves to email)
ForgotPasswordForm        "Check your email"
 enter email → Send                                  taps link in email
   │  POST /forgot-password (always 200, AC1)          │  carries token
   │                                                    ▼
   └─ AC2: if email exists → token (1h expiry) +   SCREEN 3
      stubbed email sent. If not → nothing sent,   ResetPasswordForm
      but UI is IDENTICAL (anti-enumeration).        enter + confirm new password
                                                       │  POST /reset-password
                                  ┌────────────────────┼─────────────────────┐
                                  ▼                    ▼                     ▼
                          AC4 valid token +      AC3 password fails    AC6/AC7 token
                          compliant pw →         complexity →          invalid/expired/used →
                          SCREEN 4               inline errors,        SCREEN 5
                          "Password reset"       stay on SCREEN 3      "Link expired"
                          + AC5 all sessions                            (recovery)
                          invalidated                                       │
                                  │                                         │ "Request a new link"
                                  ▼                                         ▼
                          [Continue to sign in]                      back to SCREEN 1
```

---

## Screens / Views Involved

| # | Screen / Component | Route | Purpose |
|---|--------------------|-------|---------|
| 1 | **ForgotPasswordForm** | `/forgot-password` | Capture the email and trigger the reset request (AC1, AC2) |
| 2 | **ForgotPasswordConfirmation** | `/forgot-password/sent` (or in-place state) | Neutral "check your email" message; anti-enumeration (AC1) |
| 3 | **ResetPasswordForm** | `/reset-password?token=…` | Set a new compliant password using the token (AC3, AC4) |
| 4 | **ResetPasswordSuccess** | `/reset-password/success` | Confirm reset + explain session invalidation (AC4, AC5) |
| 5 | **ResetTokenError** | `/reset-password` (invalid/expired/used token state) | Recover from a bad token (AC6, AC7) |

> The login screen's **"Forgot password?"** link (US-002) is the only entry point into Screen 1.

---

## Step-by-step Flow

### Step 0 — Entry (from US-002 Login)
- User on the login screen taps **"Forgot password?"** → navigates to **ForgotPasswordForm** (`/forgot-password`).

### Step 1 — Request reset (ForgotPasswordForm) — *AC1, AC2*
1. User sees one field: **Email address**, helper text, and a primary **Send reset link** button.
2. Inline validation: empty or malformed email → inline error, submission blocked (error prevention).
3. On valid format, user taps **Send reset link**:
   - Button enters **loading** state (`Sending…`, `aria-busy`).
   - App calls `POST /api/v1/auth/forgot-password` with `{ email }`.
   - **API always returns 200** regardless of whether the email exists (**AC1**).
   - Backend (AC2): *if* email exists → generates a single-use reset token, stores it with a
     **1-hour expiry**, and sends the reset link (stubbed). *If not* → does nothing.
4. **UI behaviour is identical in both cases** → navigate to **ForgotPasswordConfirmation**.

### Step 2 — Confirmation (ForgotPasswordConfirmation) — *AC1*
- Neutral message: *"If that email is registered, we've sent a link to reset your password."*
- Explicitly **does not** confirm whether the address exists (anti-enumeration).
- Tells the user to check their inbox/spam and that the link **expires in 1 hour**.
- Recovery affordances (no dead-end):
  - **Resend email** (re-issues the request; still returns to this neutral state).
  - **Use a different email** → back to Step 1 with the field cleared.
  - **Back to sign in** → US-002 login.
- *(Stubbed iteration note: no real email arrives; QA/dev use the token from logs/DB to reach Step 3.)*

### Step 3 — Open the email link (out-of-band)
- User opens the email and taps **Reset your password** → opens **ResetPasswordForm** at
  `/reset-password?token=<token>`. The token travels in the URL; the user never types it.
- On load, the app may pre-validate the token presence; an **absent/garbled token** → Screen 5.

### Step 4 — Set new password (ResetPasswordForm) — *AC3, AC4*
1. Two fields: **New password** (with live **PasswordChecklist**) and **Confirm new password**.
2. Live checklist (reused from US-001) ticks each rule as typed: **8+ chars, uppercase,
   lowercase, number, special character** (**AC3** — same rules as registration).
3. Confirm-password must match (typo guard).
4. User taps **Save new password**:
   - Button → **loading** (`Saving…`).
   - App calls `POST /api/v1/auth/reset-password` with `{ token, newPassword }`.
   - **AC4:** valid + unexpired token + compliant password → password updated, **200** →
     navigate to **ResetPasswordSuccess**.
   - **AC5:** on success the backend invalidates **all active sessions** (JWT/refresh tokens) for
     that customer; the success screen explains this.
5. **Branches:**
   - **AC3 failure** (password not compliant): inline field errors + checklist shows unmet rules;
     user stays on Screen 3, no navigation.
   - **AC6 / AC7** (token expired, invalid, or already used — single-use): API returns **400
     `"Invalid or expired reset token"`** → navigate to **ResetTokenError** (Screen 5).

### Step 5a — Success (ResetPasswordSuccess) — *AC4, AC5*
- Heading: *"Your password has been reset."*
- Body explains **AC5**: *"For your security, we've signed you out of all devices. Sign in with
  your new password to continue."*
- Primary CTA: **Continue to sign in** → US-002 login (email prefilled if available).

### Step 5b — Token error / recovery (ResetTokenError) — *AC6, AC7*
- Triggered by an expired token, an invalid token, or **reusing an already-consumed single-use
  token** (AC7). All produce the same human-readable screen.
- Heading: *"This reset link has expired"* with body explaining links are single-use and last 1 hour.
- Primary recovery CTA: **Request a new link** → back to Step 1 (ForgotPasswordForm).
- Secondary: **Back to sign in**; tertiary **Contact support** for repeat failures.

---

## Decision Points & Recovery Paths

| Situation | AC | What the user sees | Recovery |
|-----------|----|--------------------|----------|
| Email field empty/malformed | — (prevention) | Inline error under field | Correct and resubmit |
| Email not registered | AC1 | **Identical** neutral confirmation (no leak) | "Use a different email" / "Resend" |
| Email registered | AC2 | Same neutral confirmation; link sent (stubbed) | Open email link |
| Token missing/garbled in URL | AC6 | ResetTokenError screen | "Request a new link" |
| New password fails complexity | AC3 | Inline errors + checklist unmet rules; stays on form | Fix per checklist |
| Passwords don't match | — (prevention) | Inline error on confirm field | Re-enter |
| Token expired (>1h) | AC6 | ResetTokenError: "link expired" | "Request a new link" |
| Token already used | AC7 | ResetTokenError (same screen) | "Request a new link" |
| Valid token + compliant pw | AC4 | Success screen | "Continue to sign in" |
| All sessions invalidated | AC5 | Success copy explains sign-out everywhere | Sign in again |
| Network / 5xx on either submit | — | Inline error banner, data preserved, button restored | Retry |

---

## Navigation Summary

- **Forward (happy path):** Login → ForgotPasswordForm → Confirmation → (email) → ResetPasswordForm → Success → Login.
- **Back/escape (every screen):** "Back to sign in" → US-002 login.
- **Recovery loop:** ResetTokenError → "Request a new link" → ForgotPasswordForm (restart cleanly).
