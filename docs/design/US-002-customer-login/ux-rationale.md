# UX Rationale — US-002: Customer Login

> Banking platform · Security-critical · Returning, unauthenticated user
> Satisfies: AC1, AC2, AC3, AC4, AC5

---

## User & Job-to-be-Done

- **Primary persona:** *Maria, the Returning Customer* — she already has a NorthBank account (created in US-001) and now wants back in. She is a **returning** user: familiar with the email/password pattern and impatient with friction, but a banking login carries a higher emotional charge than most apps. She is often **rushed** (checking a balance before a payment, on a phone, possibly on the move) and becomes **anxious or defensive** the moment something goes wrong — a rejected password on a *bank* feels alarming, not merely annoying.
- **JTBD:** *When* I want to access my money and account, *I want to* prove it's me with my email and password quickly and confidently, *so that* I can move on to verification and reach my dashboard.
- **Context of use:**
  - **Device:** Mobile-first (assume ~65% mobile, often one-handed). Scales to tablet/desktop.
  - **Environment:** Anywhere — commute, shop queue, home. Variable network. Password managers in play.
  - **Urgency:** High. This is a gateway, not a destination. Every extra second is felt.
  - **Frequency:** Repeated (many times per customer over the relationship). Muscle memory matters — layout must stay stable and predictable.
  - **Emotional state:** Trust-seeking but low-patience. On failure, she needs calm, clear, non-accusatory guidance, *not* security jargon.

---

## Mental Model & Information Architecture

- **Vocabulary:** Plain terms — "Email address", "Password", "Sign in". Never surface system states like `2FA_REQUIRED`, `sessionToken`, `LOCKED`, or HTTP codes (401/423). The user thinks "I'm signing in, then it'll text me a code", not "the API returned 200 with status 2FA_REQUIRED".
- **Where it lives:** Public, unauthenticated route `/login`. Reached from the marketing site, the header "Sign in" link, the registration success screen ("Continue to sign in"), and deep links that bounce unauthenticated users here. Onward exit is the 2FA screen (US-003); lateral exits are Register (US-001) and Forgot password (US-004).
- **Information priority:**
  - **Primary:** The two credential fields and the single primary action ("Sign in").
  - **Secondary:** "Forgot password?" recovery link; "New here? Create an account" link.
  - **Tertiary:** Trust reassurance (lock icon, encryption note), footer legal links.
- **Login ≠ access.** A core IA decision: a successful login does **not** land on a dashboard. It hands off to verification (US-003). The design must make this *expected* ("we'll send you a code") so the 2FA step feels like a continuation, not an interruption.

---

## Key Decisions

| Decision | Why (user benefit) | Alternative rejected |
|----------|--------------------|----------------------|
| **Two-field single-page form** (email + password) | Fastest possible path for a frequent, high-urgency task; nothing to read or decide. | Multi-step "email first, then password" — adds a screen and a round-trip for no benefit at this scale. |
| **Generic error for invalid credentials** — never reveal which part was wrong (AC3) | Security best practice: prevents account enumeration; *and* it's honestly simpler for the user — one clear message, one action (try again / reset). | "Email not found" vs "Wrong password" — leaks which emails are registered and helps attackers; rejected on security grounds. |
| **Show-password toggle** | Lets a rushed mobile user verify a complex password instead of failing blindly — reduces failed attempts (which matter doubly here because of the 5-attempt lockout, AC4). | Masked-only — increases typos and pushes the user toward lockout. |
| **Forgot-password link given prominence next to the password field** | The natural recovery for the #1 cause of login failure; placing it *before* failure happens is error-prevention, and it's the escape hatch as the user nears lockout (AC4/AC5). | Hiding it until after an error — makes the user hunt for help at the most stressful moment. |
| **Attempts-remaining warning before lockout** (shown after a failed attempt once the user is close to the limit) | Turns the invisible "5 strikes" rule (AC4) into a visible, fair warning so the user can slow down or reset — recognition over recall, and it softens the shock of a lockout. | Silent counting then a sudden lock — feels arbitrary and punitive on a bank. |
| **Dedicated locked-account state** (AC5) with a clear, non-blaming explanation and an explicit unlock path | A locked account is a dead-end unless we say *why* and *what to do next* (contact support; unlocked by a Bank Admin). | Reusing the generic "invalid credentials" message — traps the user in a loop of failing attempts with no way out. |
| **Transitional "verification" hand-off state** after success (AC2) instead of a silent redirect | Confirms the login worked *and* sets expectations for the 2FA step, so the next screen is anticipated, not jarring. | Instant silent redirect — the user isn't sure what happened or why they're now being asked for a code. |
| **One primary action per screen** ("Sign in") | Clear hierarchy for a habitual action; secondary links de-emphasized. | Equal-weight "Sign in"/"Create account" buttons — splits attention on a returning-user screen. |
| **Do not disable "Sign in" while fields are empty/invalid** | Lets the user press and receive guidance rather than wondering why the button is dead. | Permanently disabled button — the classic "why won't this work?" trap. |
| **No `sessionToken` ever shown to the user** (AC2) | It's a machine correlation token for the OTP step only; surfacing it is meaningless and worrying. Stored in memory/transient state, not displayed. | Echoing token/status text — confusing, leaks internals. |

---

## Friction Removed / Cognitive Load

- **Only two fields.** Exactly what AC1 requires — nothing speculative (no "remember me", SSO, or biometrics; explicitly out of scope).
- **Autofill-friendly:** `type="email"` + `autocomplete="email"` and `autocomplete="current-password"` so password managers and browsers fill both fields in one tap — the ideal returning-user experience.
- **Smart input types:** email keyboard on the email field; `inputmode` set appropriately for mobile.
- **Show-password toggle** removes the "retype the whole thing" penalty on a mistyped character.
- **Forgot-password always in view**, so recovery never requires hunting.
- **Stable layout across states** — error and warning messages reserve space so the form doesn't jump, preserving muscle memory for a repeated task.
- **No dead-ends:** invalid credentials → retry or reset; lockout → clear support/unlock path; network failure → retry with data preserved (email kept, password cleared for safety).

---

## Trust, Feedback & Safety

- **Visible system status:** the submit button shows a spinner + "Signing in…" while the request is in flight; the form is disabled to prevent double submission.
- **Error prevention first:** lightweight format check on email (so an obvious typo doesn't burn a lockout attempt), show-password toggle, and the pre-lockout warning all reduce avoidable failures.
- **Calm, non-accusatory failure copy:** "Invalid email or password" is rendered as a single, neutral, form-level message (mapped from AC3's 401) — it never blames the user or names which field failed.
- **Lockout is explained, not just enforced (AC4/AC5):** the locked state tells the user *what happened*, *why*, and *exactly how to regain access* (the account is unlocked by a Bank Admin / contact support), so a security control doesn't read as a malfunction.
- **Password hygiene on failure/return:** on an error or network failure the email is preserved but the password field is cleared — the user re-enters the secret deliberately, and a shoulder-surfer doesn't see a retained password.
- **No silent failures and no silent successes:** success is acknowledged ("Verifying it's you…") before the hand-off to US-003.

---

## Heuristic Check (Nielsen's 10)

| Heuristic | How the design satisfies it |
|-----------|------------------------------|
| **1. Visibility of system status** | Spinner + "Signing in…" on submit; transitional "Verifying it's you…" state on success (AC2); attempts-remaining warning before lockout (AC4). |
| **2. Match the real world** | Human language ("Sign in", "We'll send a code"); no `2FA_REQUIRED`, `LOCKED`, `sessionToken`, or HTTP codes shown. |
| **3. User control & freedom** | "Forgot password?" recovery always available; links to Register; data (email) preserved on error; lockout screen offers a clear next action. |
| **4. Consistency & standards** | Same form/field/button vocabulary as US-001; one primary button; standard credential layout users expect. |
| **5. Error prevention** | Email format check, show-password toggle, and pre-lockout warning prevent avoidable failed attempts that could trigger AC4. |
| **6. Recognition over recall** | Persistent labels (not placeholder-only); forgot-password visible up front; attempts remaining stated explicitly rather than left to memory. |
| **7. Flexibility & efficiency** | Password-manager/autofill support, Enter-to-submit, keyboard-navigable, minimal fields for frequent use. |
| **8. Aesthetic & minimalist design** | Two fields plus essential links; no clutter on a habitual screen. |
| **9. Help users recover from errors** | Generic-but-actionable 401 message paired with "Forgot password?"; the 423 lockout state spells out the recovery path (AC5). |
| **10. Help & documentation** | Inline trust note and lockout explanation remove the need for external help in the common cases; support contact surfaced on lockout. |

### Deliberate trade-offs

- **Generic invalid-credentials message (no field-level error).** We deliberately trade *specificity* (which would normally aid recovery, heuristic #9) for **security** — not revealing whether the email exists. We compensate by pairing the message with the "Forgot password?" path so the user still has a clear way forward. **(Confirmed against AC3 — message text is fixed by the AC.)**
- **Showing "attempts remaining" before lockout.** This reveals part of the security control (the 5-attempt threshold, AC4). Trade-off accepted because fairness and shock-avoidance on a *bank* outweigh the minor information disclosure; an attacker gains little (the limit is not secret-by-design). Threshold for *when* to start warning (e.g. after the 3rd failure) is a **product/security decision flagged to BA/Architect** — see UX Flow note.
- **Lockout reason and unlock path are surfaced.** Some security teams prefer opacity. We surface a calm explanation plus "contact support" because an unexplained lock on a bank account drives panicked calls and erodes trust; the *mechanism* (admin unlock, US-019) is described in user terms, not system terms. **(Raised to BA.)**
- **No "remember me" / persistent session.** Out of scope per the story; also a sensitive default on a banking app. Layout reserves no space for it.
