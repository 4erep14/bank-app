# Interaction States — US-002: Customer Login

Accessibility baseline for every state: keyboard-navigable, visible focus ring (≥2px, ≥3:1
contrast against background), text/icon contrast ≥4.5:1, errors conveyed by **icon + text +
color** (never color alone), inline/form errors wired via `aria-describedby` and announced with
`role="alert"`. Invalid fields set `aria-invalid="true"`. The transient `sessionToken` (AC2) is
never rendered to the DOM.

---

## LoginForm (whole form)

| State | Trigger | Visual / Behavior | A11y |
|-------|---------|-------------------|------|
| Default / Empty (first-run) | Page load | Both fields empty, labels visible, "Forgot password?" + "Create account" links shown, primary button enabled. No errors. | Logical tab order: Email → Password → 👁 toggle → Forgot password → Sign in → Create account. Email may be auto-focused on desktop; not on mobile (avoids keyboard jump). |
| Validating (in-field) | User blurs email | Email flips to Valid or a light format Error; password is not format-validated (server checks it). | Live region announces email format error if present. |
| Submitting | Valid submit | All inputs + links disabled/dimmed; button shows spinner + "Signing in…"; no double submit. | `aria-busy="true"` on form; focus retained on button. |
| Invalid credentials (AC3 / 401) | 401 response | Form re-enabled; **form-level** error banner "Invalid email or password. Please try again."; email preserved; password cleared and re-focused. Neither field marked as the specific culprit. | Banner `role="alert"`; focus moves to password field. |
| Approaching lockout (AC4 / 401) | 401 when within N of limit | As above, plus a warning line in the banner naming attempts remaining (or generic if API doesn't expose count). | Banner `role="alert"`; warning read as part of the alert. |
| Locked (AC4 / AC5 / 423) | 423 response | Form body replaced by `LockedAccountNotice`; inputs removed. | Move focus to locked-state H1; announce. |
| Success / Hand-off (AC2 / 200) | 200 `2FA_REQUIRED` | Form replaced by `VerificationHandoff` ("Verifying it's you…"); `sessionToken` stored in transient state; auto-route to US-003. | Move focus to handoff H1; `aria-busy="true"`; announce status. |
| Network/5xx error | Request fails | Form re-enabled; email preserved, password cleared; top banner "We couldn't sign you in right now. Please try again."; button back to default. Failure NOT counted toward lockout. | Banner `role="alert"`. |

---

## TextField — Email

| State | Visual | A11y |
|-------|--------|------|
| Default | Neutral border, label above, empty input | `<label for>` linked; `type="email"`, `autocomplete="email"`, `inputmode="email"` |
| Focus | Visible focus ring | — |
| Filled/Valid | Neutral border; subtle ✓ when format valid & non-empty | `aria-invalid="false"` |
| Error — blank (on submit) | Red border + `!` icon + "Enter your email address." below | `aria-invalid="true"`, `aria-describedby` → error id, `role="alert"` |
| Error — invalid format (on blur) | Red border + `!` + "Enter a valid email address, like name@example.com." | `aria-invalid="true"`, `role="alert"` |
| Preserved after 401 | Value retained; **not** marked invalid (anti-enumeration) | `aria-invalid="false"` |
| Disabled | Dimmed, non-interactive (during submit) | `disabled` |

> Note: email is given only a lightweight *format* check to prevent an obvious typo from wasting
> a lockout attempt. A wrong-but-valid email is never flagged at field level — only the generic
> form-level 401 message applies (AC3).

---

## PasswordField — Password

| State | Visual | A11y |
|-------|--------|------|
| Default | Neutral border; 👁 show/hide toggle | `type="password"`, `autocomplete="current-password"` |
| Focus | Visible focus ring | — |
| Filled | Neutral border; dots (or plaintext if revealed) | `aria-invalid="false"` |
| Error — blank (on submit) | Red border + `!` + "Enter your password." | `aria-invalid="true"`, `aria-describedby`, `role="alert"` |
| After 401 (AC3) | Field **cleared** and re-focused; no field-level "wrong password" message (generic form error only) | `aria-invalid="false"`; focus moved here |
| Show/Hide toggle | 👁 toggles plaintext/masked | Toggle is a `button` with `aria-pressed` + label "Show password" / "Hide password" |
| Disabled | Dimmed, non-interactive; toggle disabled | `disabled` |

> Security: the password is never auto-retained across an error or network failure; the user
> deliberately re-enters it. No complexity checklist here (that's registration, US-001) — login
> only verifies, it does not enforce policy.

---

## SubmitButton — "Sign in"

| State | Trigger | Visual | A11y |
|-------|---------|--------|------|
| Default | Form interactive | Solid primary fill, label "Sign in", full-width on mobile | Focusable, ≥44px target |
| Hover | Pointer over (desktop) | Darken fill ~8–10%; cursor pointer | — |
| Focus | Keyboard focus | Visible focus ring distinct from hover | — |
| Loading/Disabled | Submit in flight | Spinner + "Signing in…"; same width; not clickable | `aria-busy="true"`, `disabled` |
| Success | 200 received (AC2) | Button area replaced by `VerificationHandoff` state | Focus moves to handoff H1 |

> Button is **not** permanently disabled while fields are empty/invalid — pressing it triggers
> validation and guidance (avoids the "why won't this button work?" trap). It is disabled only
> during the submitting/loading state.

---

## FormErrorBanner

| State | Visual | A11y |
|-------|--------|------|
| Hidden | Not rendered | — |
| Invalid credentials (AC3) | Red bar + `!` icon: "Invalid email or password. Please try again." | `role="alert"`; `variant="error"` |
| Approaching lockout (AC4) | Amber bar + `!` icon: "Invalid email or password. For your security, your account will be locked after {n} more failed attempts." | `role="alert"`; `variant="warning"` |
| Server/network | Red bar + `!` icon: "We couldn't sign you in right now. Please try again." | `role="alert"`; `variant="error"` |

---

## VerificationHandoff (AC2 — transitional success)

| State | Visual | A11y |
|-------|--------|------|
| Default (active) | Centered spinner + H1 "Verifying it's you…" + body "We're sending a code to keep your account safe. Hang on a moment." | `role="status"` / `aria-busy="true"`; focus on H1; spinner icon `aria-hidden` |
| Auto-advance | After a brief moment, routes to US-003 OTP screen, passing the transient `sessionToken` | Route change announces new screen title |
| Fallback (route delay/failure) | "Continue to verification" button appears | Button focusable; descriptive label |

> The `sessionToken` is held in memory/transient state only and is **never** displayed or written
> to durable storage in this story. US-002 ends at "Proceeding to verification…".

---

## LockedAccountNotice (AC4 / AC5 — 423)

| State | Visual | A11y |
|-------|--------|------|
| Default | Lock icon (decorative) + H1 "Your account is locked" + explanation + recovery copy; Email/Password inputs removed | Focus moves to H1 on render; icon `aria-hidden` |
| Primary action | "Contact support" button (links to support channel) | `button`/link with clear label; keyboard-focusable; ≥44px |
| Secondary actions | "Forgot password?" and "Back to sign in" links | Focusable; "Back to sign in" returns to `/login` |
| Note | No credential inputs present — a locked account cannot be retried here (AC5); only an admin unlock (US-019) restores access | — |

---

## TrustBadge

| State | Visual | A11y |
|-------|--------|------|
| Default | 🔒 + "Your connection is encrypted and secure." | Decorative icon `aria-hidden`; text readable |

---

## Responsive behavior summary

| Breakpoint | Layout |
|------------|--------|
| Mobile (<768px) | Single column, full-width fields and button (≥44px touch targets); "Forgot password?" right-aligned under password. |
| Tablet/Desktop (≥768px) | Centered card (~400–460px); same single-column form; header shows "Create account" link. |

## Form validation rules

| Field | Client-side rule | Server-side authority |
|-------|------------------|------------------------|
| Email | Required; basic format check (`name@example.com`) to avoid wasting a lockout attempt | Credential validity decided by API (AC3) — never asserted client-side |
| Password | Required (non-empty) | Verified by API; no complexity rules enforced at login |

> All credential *correctness* decisions belong to the server (AC3/AC4/AC5). The client only
> prevents empty/obviously-malformed submissions and renders the server's outcome.
