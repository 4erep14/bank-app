# Interaction States — US-004: Password Reset

Accessibility baseline for every state (same as US-001): keyboard-navigable, visible focus ring
(≥2px, ≥3:1 contrast vs. background), text/icon contrast ≥4.5:1, errors conveyed by **icon + text
+ colour** (never colour alone), inline errors wired via `aria-describedby` and announced with
`role="alert"`. Invalid fields set `aria-invalid="true"`. Primary buttons are ≥44px targets.

---

## Component Specs (reused & new)

### Reused from US-001 (no change to contract)
- **PasswordField** — masked input + show/hide toggle (`button`, `aria-pressed`, label "Show/Hide password").
- **PasswordChecklist** — live list of the 5 complexity rules; `aria-live="polite"`.
- **SubmitButton** — primary action with default/loading states.
- **FormErrorBanner** — top-of-form `role="alert"` banner for server/network failures.
- **TrustBadge** — `🔒` + reassurance text; icon `aria-hidden`.

### New — Component Spec: `ForgotPasswordForm`
**Purpose:** Capture email and trigger `POST /forgot-password`. **Used in:** US-004.
**Props:** `initialEmail?: string`. **Events:** `onSubmit(email)`, `onBackToSignIn()`.
**States:** Default, Validating, Submitting, Server/Network error, Success(→Confirmation).

### New — Component Spec: `ForgotPasswordConfirmation`
**Purpose:** Neutral anti-enumeration confirmation. **Used in:** US-004.
**Props:** none (must NOT receive/echo the email). **Events:** `onResend()`, `onUseDifferentEmail()`, `onBackToSignIn()`.
**States:** Default, Resending, Resent (subtle confirmation).

### New — Component Spec: `ResetPasswordForm`
**Purpose:** Set a new compliant password using a token. **Used in:** US-004.
**Props:** `token: string`. **Events:** `onSubmit(token, newPassword)`, `onBackToSignIn()`.
**States:** Default, Typing/Progress, Validating, Submitting, Error(AC3/mismatch), TokenError(→Screen5), Success(→Screen4).

### New — Component Spec: `ResetTokenError`
**Purpose:** Recovery screen for invalid/expired/used token. **Used in:** US-004.
**Props:** none. **Events:** `onRequestNewLink()`, `onBackToSignIn()`, `onContactSupport()`.
**States:** Default only.

---

## ForgotPasswordForm (whole form) — AC1, AC2

| State | Trigger | Visual / Behavior | A11y |
|-------|---------|-------------------|------|
| Default / first-run | Page load | Single email field, helper visible, primary button enabled, "Back to sign in" link. No errors. | Logical tab order; email field focusable (not auto-focused on mobile). |
| Validating (in-field) | Blur email | Flips to Valid (subtle ✓) or Error (inline). | Live region announces error if present. |
| Submitting | Valid submit | Field + links disabled/dimmed; button → spinner + "Sending…"; no double submit. | `aria-busy="true"` on form; focus stays on button. |
| Success | 200 (always, AC1) | Navigate to ForgotPasswordConfirmation — **identical for existing & non-existing email**. | Move focus to Confirmation H1. |
| Network/5xx error | request fails | Form re-enabled, email preserved; FormErrorBanner "Something went wrong… Please try again."; button restored. | Banner `role="alert"`. |

> **AC1 enforcement:** the component has **no "email not found" branch**. It cannot render any
> state that differs based on email existence.

## TextField — Email (ForgotPasswordForm)

| State | Visual | A11y |
|-------|--------|------|
| Default | Neutral border, label above, helper below | `type="email"`, `autocomplete="email"`, `inputmode="email"`, helper via `aria-describedby` |
| Focus | Visible focus ring | — |
| Filled/Valid | Neutral border + ✓ when format valid | `aria-invalid="false"` |
| Error — blank | Red border + `!` + "Enter your email address." | `role="alert"` |
| Error — invalid format | Red border + `!` + "Enter a valid email address, like name@example.com." | `role="alert"` |
| Disabled | Dimmed, non-interactive (during submit) | `disabled` |

## SubmitButton — "Send reset link"

| State | Visual | A11y |
|-------|--------|------|
| Default | Solid primary fill, full-width on mobile, label "Send reset link" | Focusable, ≥44px |
| Hover | Darken ~8–10% (desktop) | — |
| Focus | Visible focus ring distinct from hover | — |
| Loading | Spinner + "Sending…"; not clickable | `aria-busy="true"`, `disabled` |

> Button is **not** permanently disabled on invalid input — pressing it triggers validation guidance
> (avoids the "why won't this work?" trap). Disabled only during submit.

---

## ForgotPasswordConfirmation — AC1

| State | Trigger | Visual / Behavior | A11y |
|-------|---------|-------------------|------|
| Default | Arrived from submit | ✉ icon, H1 "Check your email", neutral body + "expires in 1 hour" + anti-enumeration helper; Resend button; "Use a different email" / "Back to sign in" links. **No email address shown.** | Focus to H1; body in normal reading order. |
| Resending | Tap "Resend email" | Button → "Sending…"; calls `POST /forgot-password` again. | `aria-busy` on button. |
| Resent | Resend succeeds (always 200) | Subtle inline confirmation: "Sent again — check your inbox." (still no enumeration). | `aria-live="polite"` announcement. |
| Network/5xx error | Resend fails | FormErrorBanner "Something went wrong… Please try again." | Banner `role="alert"`. |

---

## ResetPasswordForm (whole form) — AC3, AC4

| State | Trigger | Visual / Behavior | A11y |
|-------|---------|-------------------|------|
| Default | Page load with token in URL | Both password fields empty; PasswordChecklist shows all rules neutral (○); primary button enabled. | Tab order: New pw → Confirm pw → Save → Back link. |
| Token pre-check fail | Token absent/garbled on load | Render ResetTokenError (Screen 5) instead of the form. | Focus to error H1. |
| Typing / Progress | User types new password | Each met rule flips ○ → ✓; unmet shows ✗ (icon + text + colour). | Checklist updates `aria-live="polite"`. |
| Validating | Blur fields | Confirm-password checked against new password. | Errors announced. |
| Submitting | Valid submit | Inputs + links disabled; button → spinner + "Saving…"; no double submit. | `aria-busy="true"` on form. |
| Error — complexity (AC3) | 400 / client rule fail | Inline error naming missing rule(s); checklist shows ✗ on failing rules; data preserved; focus to first errored field. | Each error `role="alert"`; `aria-invalid="true"`. |
| Error — mismatch | Confirm ≠ new | Inline error on Confirm field "Passwords don't match. Re-enter to confirm." | `role="alert"`. |
| Error — token invalid/expired/used (AC6/AC7) | 400 "Invalid or expired reset token" | Navigate to ResetTokenError (Screen 5). | Focus to error H1. |
| Network/5xx error | request fails | FormErrorBanner; data preserved; button restored. | Banner `role="alert"`. |
| Success | 200 (AC4) | Navigate to ResetPasswordSuccess (AC5 messaging). | Focus to success H1. |

## PasswordField — New password (AC3)

| State | Visual | A11y |
|-------|--------|------|
| Default | Neutral border; 👁 show/hide; PasswordChecklist below, all rules ○ | `type="password"`, `autocomplete="new-password"`, checklist via `aria-describedby` |
| Focus | Visible focus ring; checklist live as user types | `aria-live="polite"` |
| Typing/Progress | Met rule ○→✓ (green); unmet ✗ (red) — icon + text, not colour only | — |
| Filled/Valid | All 5 rules ✓; neutral border + field ✓ | `aria-invalid="false"` |
| Error — blank | Red border + `!` + "Enter a new password." | `role="alert"` |
| Error — too short (<8) | Red border + `!` + "Password must be at least 8 characters." | `role="alert"` |
| Error — missing complexity (AC3) | Red border + `!` + dynamic message naming missing rule(s); checklist ✗ on failing rules | `role="alert"` |
| Show/Hide toggle | 👁 toggles plaintext/masked | `button` with `aria-pressed` + "Show password"/"Hide password" |
| Disabled | Dimmed; toggle disabled | `disabled` |

## PasswordField — Confirm new password

| State | Visual | A11y |
|-------|--------|------|
| Default | Neutral border; 👁 show/hide; no checklist | `type="password"`, `autocomplete="new-password"` |
| Focus | Visible focus ring | — |
| Filled/Valid | Neutral border + ✓ when it matches new password | `aria-invalid="false"` |
| Error — blank | Red border + `!` + "Re-enter your new password to confirm." | `role="alert"` |
| Error — mismatch | Red border + `!` + "Passwords don't match. Re-enter to confirm." | `role="alert"` |
| Disabled | Dimmed; toggle disabled | `disabled` |

## SubmitButton — "Save new password"

| State | Visual | A11y |
|-------|--------|------|
| Default | Solid primary fill, full-width on mobile, label "Save new password" | Focusable, ≥44px |
| Hover | Darken ~8–10% (desktop) | — |
| Focus | Visible focus ring distinct from hover | — |
| Loading | Spinner + "Saving…"; not clickable | `aria-busy="true"`, `disabled` |

---

## ResetPasswordSuccess — AC4, AC5

| State | Visual / Behavior | A11y |
|-------|-------------------|------|
| Default | ✓ icon, H1 "Your password has been reset", body + AC5 sign-out-everywhere note; primary "Continue to sign in". | Focus to H1 on mount; announce confirmation. |

## ResetTokenError — AC6, AC7

| State | Visual / Behavior | A11y |
|-------|-------------------|------|
| Default | ⚠ icon, H1 "This reset link has expired", human-readable explanation (single-use + 1-hour); primary "Request a new link"; "Back to sign in" / "Contact support" links. | Focus to H1; primary CTA reachable by keyboard. |

> Raw API string `"Invalid or expired reset token"` is mapped to friendly copy — never shown verbatim.

---

## FormErrorBanner (shared)

| State | Visual | A11y |
|-------|--------|------|
| Hidden | Not rendered | — |
| Field-summary | Amber/red bar + `!`: "Please fix the highlighted fields below." | `role="alert"` |
| Server/network | Red bar + `!`: "Something went wrong on our end. Please try again." | `role="alert"` |

## Empty / first-run note
The request screen **is** the first-run state for this flow (one empty email field with helper).
The set-new-password screen's first-run is both fields empty with the neutral (○) checklist — the
checklist itself acts as guidance so the user always knows what "done" looks like.
