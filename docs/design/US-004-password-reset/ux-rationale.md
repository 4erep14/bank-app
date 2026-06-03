# UX Rationale — US-004: Password Reset

> Brand placeholder: **NorthBank**. Reuses patterns and component vocabulary established
> in US-001 (PasswordField, PasswordChecklist, SubmitButton, FormErrorBanner, TrustBadge)
> and the auth screens of US-002 (login). Designed mobile-first, WCAG 2.1 AA.

---

## User & Job-to-be-Done

- **Primary persona:** *Returning customer, locked out.* A NorthBank account holder who has
  forgotten their password and cannot sign in. Proficiency: **returning user**, but in this
  moment operating under stress — they may be mid-task (paying a bill, checking a balance) and
  the lockout is a hard blocker.
- **JTBD:** *When I've forgotten my password and can't sign in, I want to securely reset it
  using only my email, so I can get back into my account quickly without phoning support.*
- **Context of use:** Mobile-first (most likely on a phone, possibly the same device that
  receives the reset email). Emotional state: **anxious, impatient, slightly suspicious**
  (security-sensitive banking context). Frequency: rare per user, but high-stakes each time.
  Often interrupted — the user must leave the app to open their email and come back.

---

## Mental Model & Information Architecture

- The user's mental model is the universal **"forgot password → check email → set new password"**
  pattern. We deliberately mirror it so there is nothing new to learn (recognition over recall).
- The journey is **two screens separated by an out-of-band step (email)**:
  1. **Request reset** (enter email) → confirmation that *if* the address is registered, a link is on its way.
  2. **Set new password** (reached by clicking the emailed link, which carries the token).
- **Entry points:** the **"Forgot password?"** link on the US-002 login screen is the canonical
  entry. The set-new-password screen is reachable **only** via the tokenised email link.
- **Primary vs. secondary information:**
  - Request screen — *primary:* the single email field + submit. *Secondary:* "Back to sign in".
  - Confirmation screen — *primary:* reassurance + "what to do next". *Secondary:* resend / try another email.
  - Set-new-password screen — *primary:* new password field + live checklist + submit.
- **Getting back:** every screen offers a route back to **Sign in**; no screen is a dead-end.

---

## Key Decisions

| Decision | Why (user benefit) | Alternative rejected |
|----------|--------------------|----------------------|
| **Two distinct screens + email step** (not a single modal) | Matches the universal mental model; the email hop is unavoidable because the token is delivered out-of-band (AC2) | Inline OTP-style code entry — adds a field and a new pattern; not in scope |
| **Anti-enumeration confirmation copy** — same neutral "If that email is registered, we've sent a link" for *every* request | Security requirement (AC1): the UI must never reveal whether an email exists. Protects customers from account-probing | A literal "Email sent to x@y.com" — leaks account existence; rejected outright |
| **Always show success state regardless of email existence** | Enforces AC1 at the UX layer; the API returns 200 either way, so the UI must not branch on a "not found" | Showing "We couldn't find that email" — directly violates AC1 |
| **Single email field on request screen** | Lowest cognitive load for a stressed user; nothing else is needed to start the flow | Asking for email + DOB/last-4 — extra friction, not required by ACs |
| **Reuse the live `PasswordChecklist`** from US-001 on the set-new-password screen | Consistency + error prevention: the user sees complexity rules met in real time before submitting (AC3) | Validate only on submit — fails error-prevention heuristic; more round trips |
| **Confirm new password with a second "Confirm password" field** | Prevents the user locking themselves out again via a typo in a masked field | Single password field — one typo = another lockout; poor for a recovery flow |
| **Dedicated full-screen error state for invalid/expired/used token** | The token failure (AC6/AC7) happens *before* any form is usable, so it needs its own screen with a clear recovery CTA, not an inline field error | Inline error on the password form — confusing, since the form was never valid to begin with |
| **"Request a new link" CTA on the token-error screen** | Turns a dead-end (expired/used token) into a one-tap recovery back to the request screen | Generic "Error" with no action — traps the user |
| **Invalidate-all-sessions communicated on success** | Builds trust in a banking context: user is told other sessions were signed out (AC5) so a "you've been logged out elsewhere" surprise is explained up front | Silent invalidation — correct technically, but erodes trust if unexplained |
| **1-hour expiry surfaced as friendly guidance, not a countdown** | Sets expectations without inducing timer anxiety; if it lapses, the error screen recovers them | Live countdown timer — adds pressure and engineering cost for little benefit |

---

## Friction Removed / Cognitive Load

- **One required field** to start (email). Nothing the system can't justify.
- **No password rules to memorise** — the live checklist shows them and ticks them off (AC3).
- **No manual token handling** — the token rides in the email link's query string; the user
  never sees, copies, or types it.
- **Email autofill** via `type="email"` + `autocomplete="email"`; new password uses
  `autocomplete="new-password"` so password managers offer to generate/store.
- **Resend without retyping** — the confirmation screen offers "Resend email" and "Use a
  different email" so an interrupted user isn't forced to restart from memory.

---

## Trust, Feedback & Safety

- **Visible status at every step:** submit buttons show a loading label (`Sending…`,
  `Saving…`); the confirmation and success screens explicitly state what happened and what's next.
- **Error prevention first:** live password checklist + confirm-password field + inline email
  format validation stop most errors before submission.
- **Anti-enumeration is a safety feature, surfaced honestly:** the confirmation copy explains
  *why* we don't confirm the address ("To protect your account, we don't reveal whether an email
  is registered.") — turning a potential confusion into a trust signal.
- **Reversibility / no traps:** an expired or already-used token (AC6/AC7) routes to a recovery
  screen with a one-tap "Request a new link". Sign-in is always reachable.
- **Reassurance on session invalidation (AC5):** success copy tells the user every device was
  signed out and that they should now sign in with the new password — framed as protection.

---

## Heuristic Check (Nielsen)

- **Visibility of system status** — loading labels on both submits; explicit confirmation and
  success screens; token-error screen states the problem plainly.
- **Match to the real world** — plain language ("Forgot your password?", "We've sent you a link");
  no "token", "JWT", "401", or "expiry timestamp" surfaced to the user.
- **User control & freedom** — "Back to sign in" on every screen; "Use a different email" and
  "Request a new link" provide escape hatches; no dead-ends.
- **Consistency & standards** — reuses US-001 PasswordField/PasswordChecklist/SubmitButton and
  the universal forgot-password pattern; same tone and error grammar.
- **Error prevention** — live checklist, confirm-password, inline email validation, single-use
  token guidance.
- **Recognition over recall** — rules shown on screen, not remembered; the email link carries the
  token so nothing must be transcribed.
- **Flexibility & efficiency** — password-manager autofill; email-app deep link works on the same
  device or a different one.
- **Aesthetic & minimalist design** — one primary action per screen; request screen is a single field.
- **Help users recover from errors** — token-error screen names the cause in human terms and
  offers the corrective action; password errors say exactly which rule to satisfy.
- **Help & documentation** — inline helper text and a "Contact support" fallback link on the
  token-error screen for users who repeatedly fail.

### Deliberate trade-offs
- **AC1 vs. "helpfulness" heuristic:** Normally we'd tell a user "that email isn't registered" to
  help them. We **deliberately refuse** to, because anti-enumeration (AC1) outranks that
  convenience in a banking context. We compensate with a clear explanation and a "use a different
  email" path.
- **No live expiry countdown:** we trade precise status visibility for reduced anxiety, relying on
  the recovery screen to handle a lapsed token gracefully.
