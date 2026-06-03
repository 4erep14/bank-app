# UX Rationale — US-003: Two-Factor Authentication via SMS

> Banking platform · Security-critical · Returning, mid-flow user (post-password-check)
> Satisfies: AC1, AC2, AC3, AC4, AC5

---

## User & Job-to-be-Done

- **Primary persona:** *Maria, the Returning Customer* — she has already passed the US-002 password
  check and is in the middle of signing in. She is a **returning** user who is familiar with
  "enter the code we texted you" from other apps (banking, email recovery, e-commerce). She did
  not choose to come to this screen — she was routed here automatically as a security checkpoint.
- **JTBD:** *When* I have just entered my password and NorthBank is verifying my identity, *I want
  to* enter the 6-digit code from my SMS as quickly as possible, *so that* I can reach my account
  and complete whatever task prompted me to sign in.
- **Context of use:**
  - **Device:** Mobile-first (phone where she will also receive the SMS; one-handed use expected).
  - **Environment:** On the move, possibly in a noisy or distracted environment. She will context-
    switch from the banking app → SMS app → banking app to retrieve the code.
  - **Urgency:** High. This is the last step before her dashboard. Any friction compounds the
    frustration of the preceding password step.
  - **Frequency:** Every login session — a highly repeated task. Speed and predictability matter.
  - **Emotional state:** Mildly impatient (she already authenticated once), and security-aware
    (she understands why this step exists). On failure, the emotional register tips toward anxious
    — a wrong code on a *bank account* feels alarming, not merely inconvenient.

---

## Mental Model & Information Architecture

- **Vocabulary:** Users think "code" or "text message", not "OTP", "TOTP", or "one-time password".
  Labels say "Enter the code we sent" and "We texted you a 6-digit code" — never "Submit OTP".
- **Masked phone number:** The user must be shown *which device* to look on. We display the
  partially masked number (+*** *** **89) so she can confirm she is looking at the right phone
  without exposing her full number in a potentially shoulder-surfed environment. This is standard
  banking convention she already expects.
- **Where it lives:** Route `/verify-otp` — a protected, session-gated page. Accessed only via the
  US-002 hand-off with a valid `sessionToken` in `sessionStorage`. Direct access without a
  `sessionToken` redirects to `/login`. There is no navigation sidebar or header links — this is a
  single-focus authentication tunnel.
- **Information priority:**
  - **Primary:** The 6-digit input group and the single "Verify" action (or auto-submit when full).
  - **Secondary:** Where the code was sent (masked number); expiry reminder; "Resend code" after
    cooldown.
  - **Tertiary:** "Back to sign in" escape hatch; trust badge; footer.
- **No distractions:** No links to the marketing site, no "New here?" prompt. The user is mid-flow;
  every element must serve the task of entering the code.

---

## Key Decisions

| Decision | Why (user benefit) | Alternative rejected |
|----------|--------------------|----------------------|
| **6 individual digit boxes** instead of a single 6-character field | Clear affordance: user sees exactly 6 slots and knows how many digits to enter. Auto-advances focus on each key press — zero deliberate cursor management. Shows completion at a glance. Supports OS SMS autofill and paste cleanly. Feels native to the banking / 2FA mental model most users already have. | Single text input — works but less visually clear about expected length; cursor placement confusion on mobile; does not provide auto-advance UX. |
| **Auto-submit when all 6 boxes are filled** (with a 300 ms debounce) | Reduces the step count by one for the happy path — the vast majority of users fill all 6 and expect nothing else to do. The debounce prevents a mistype in the final box from causing an unwanted submit. | Requiring explicit "Verify" press — adds a redundant step for every single user on every single login. |
| **Manual "Verify" button retained visibly** | Users who paste, use autofill, or navigate back to correct a digit may land in a "all 6 filled but I didn't trigger auto-submit" state. The button provides a clear, always-available escape hatch. | Button hidden after auto-submit triggers — creates confusion if the auto-submit fires and the user can't see the feedback clearly. |
| **"Resend code" with a 60-second cooldown** (not immediately available) | Prevents SMS flooding / abuse and forces a brief wait before a new code is requested. 60 s is long enough for SMS delivery on a slow network but short enough that an impatient user can wait. Countdown text keeps the user informed and prevents repeated tapping. | Instant resend — allows abuse and sends multiple competing codes, which confuses users who cannot tell which code is active. |
| **Attempts remaining shown after each failure** (AC4/AC5) | Banking users deserve to know how close they are to session invalidation so they can slow down, check the code again, or request a new one before they are locked out. Recognition over recall; fairness. | Silent failure count — suddenly being thrown back to the login page with no warning is alarming on a bank and damages trust. |
| **Single combined error message for invalid AND expired OTP** | The API returns HTTP 401 with `"Invalid or expired OTP"` for both AC2 (expired) and AC4 (invalid). We cannot distinguish them at the UI level. The error message covers both cases *and* directs the user to "Request a new code" — which resolves both scenarios. | Separate messages for invalid vs expired — requires the API to return different messages or codes; not specified in AC; would complicate the contract unnecessarily. |
| **Static "This code expires in 5 minutes" text** rather than a live countdown | A live countdown adds anxiety and cognitive load for the common case where the user enters the code in < 30 seconds. The static reminder covers the rare user who is slow or distracted without stressing everyone else. | Live countdown timer — increases anxiety; misleads if the SMS is delayed (the timer starts when the UI loads, not when the user actually receives the SMS). |
| **Full-screen focus mode** (no main-nav, no sidebar) | This is a security checkpoint. Removing navigational chrome removes distractions, prevents accidental navigation away from the session, and communicates that "this is the only thing to do right now." | Standard app navigation visible — invites the user to navigate away, abandoning the session, which then requires a full re-login. |
| **"Back to sign in" escape hatch** | The user must always have a way out (user control & freedom). If they cannot find their phone, the SMS hasn't arrived, or they entered the wrong phone number during registration, they need a graceful exit that clears the session. | No escape hatch — traps the user on a page they cannot complete, which is a critical usability failure (violates heuristic #3). |
| **`sessionToken` never rendered to the DOM** | The `sessionToken` from US-002 is a machine-correlation token; showing it is meaningless to the user and is a potential security concern. It lives in `sessionStorage` and is passed as an HTTP header. | Showing the session token — confusing, unnecessary, potentially a phishing vector. |

---

## Friction Removed / Cognitive Load

- **Auto-advance between digit boxes:** user never touches the cursor between digits.
- **Auto-submit on fill:** removes the "what do I press now?" question for the happy path.
- **OS / browser SMS autofill (OTP autocomplete):** on supported devices the OS suggests the code
  directly from the received SMS. The input group's `autocomplete="one-time-code"` attribute enables
  this. This is the fastest possible path: receive SMS → tap the suggestion → code fills → submits.
- **Paste anywhere → fills all 6:** if the user copies the code from the SMS app and pastes it into
  any of the 6 boxes, the input logic distributes the digits and auto-submits.
- **Masked number shown:** removes the mental question "which phone should I check?".
- **No unnecessary fields:** there is exactly one thing to do — enter 6 digits.
- **Resend in view at all times** (cooldown or active): the user never has to search for the
  recovery action if the SMS doesn't arrive.

---

## Trust, Feedback & Safety

- **Every action has immediate, visible feedback:**
  - Digit entry → box fills and focus moves to next (or submits) instantly.
  - Submit → all boxes disabled + "Verifying…" spinner with `aria-busy`.
  - Success → brief green checkmark + "Identity verified!" → immediate redirect (AC3).
  - Error → red outline on all boxes + error banner (role="alert") + boxes cleared + focus reset
    to first box (AC4); attempts remaining shown.
  - Session invalidated → immediate redirect to `/login` with `sessionStorage` cleared and a
    persistent banner message "Too many attempts. Please sign in again." (AC5).
  - Resend submitted → spinner → confirmation "A new code has been sent to +*** *** **89." +
    60-second cooldown reset.
- **Error prevention first:**
  - Digit boxes accept only numeric characters; non-numeric key presses are silently ignored.
  - Paste is sanitized: only the first 6 numeric characters are used; whitespace and dashes (common
    in SMS codes like "123 456") are stripped.
  - Auto-advance prevents the wrong-box entry confusion.
- **After an error, all 6 boxes are cleared** and focus returns to the first box — the user
  re-enters the full code deliberately. This prevents a scenario where a misread "6" sitting in
  box 3 is not noticed and submitted again.
- **Session invalidation (AC5) is immediate and deterministic:** the UI does not allow a 4th
  attempt; on the 3rd 401 the session is cleared and the user is routed back to `/login` with the
  AC5-specified message.
- **No silent failures:** every outcome (good or bad) is communicated clearly before any navigation.

---

## Heuristic Check (Nielsen's 10)

| Heuristic | How the design satisfies it |
|-----------|-----------------------------|
| **1. Visibility of system status** | Spinner + "Verifying…" on submit; resend countdown running; "A new code has been sent" confirmation after resend; success indicator before dashboard redirect. |
| **2. Match the real world** | Language: "code", "text message", "we sent you a code". Masked phone format matches the SMS/banking convention. No "OTP", "token", "session", or HTTP codes shown. |
| **3. User control & freedom** | "Back to sign in" always available to exit the flow; user can request a new code after 60 s; resend is not hidden behind an error state — it's always visible. |
| **4. Consistency & standards** | Same `FormErrorBanner`, `SubmitButton` (now "Verify"), `TrustBadge`, and page shell as US-002 — learned patterns carry over. Error variants (red error, amber warning) match US-002's vocabulary. |
| **5. Error prevention** | Digit boxes are numeric-only; paste is sanitized; auto-advance prevents misplacement; static expiry warning sets expectation before an expiry error occurs. |
| **6. Recognition over recall** | 6 individual boxes make the 6-digit constraint unmistakable. Masked phone number shown so the user doesn't have to remember which number is registered. Attempts remaining stated as a number, not left to memory. |
| **7. Flexibility & efficiency** | SMS autofill (autocomplete="one-time-code") reduces the task to a single tap on supported devices. Paste support. Auto-submit on fill. Keyboard-navigable for power/accessibility users. |
| **8. Aesthetic & minimalist design** | Single task, single input group, single primary action. No chrome or navigation that doesn't serve the task. |
| **9. Help users recover from errors** | Error message covers both invalid and expired codes, and immediately points to "Request a new code" as the fix. Attempts remaining count is shown so the user can make an informed choice (try again vs. resend). Session-invalidation message on `/login` explains why. |
| **10. Help & documentation** | The page explains *where* the code was sent (masked number), *how long* it lasts (5 min helper), and *what to do* if it doesn't arrive (resend). No external help page needed for the common cases. |

### Deliberate trade-offs

- **Auto-submit on fill.** This is a speed-vs-safety trade-off. Auto-submit is the right choice
  here because: (a) the user is filling a fixed-length known field; (b) errors are recoverable
  (the user just re-enters); (c) the debounce prevents accidental single-key submission. The
  retained manual "Verify" button means no user is forced to rely on auto-submit.
- **Combined invalid/expired error message.** We cannot distinguish these at the UI level without
  an API contract change. The combined message ("incorrect or has expired") is honest, accurate,
  and actionable — it covers both scenarios and directs the user to the resolution (request a new
  code). *Flagged to Architect: if the API can return separate codes for AC2 vs AC4, the UI can
  render more specific messages.*
- **Static expiry reminder instead of live countdown.** We trade real-time accuracy for reduced
  anxiety. In 99% of sessions the code is entered within 60 seconds; showing a countdown running
  toward zero would pressure users and cause unnecessary errors. *If a future requirement adds a
  longer OTP lifetime (> 5 min), reconsider whether a live countdown becomes appropriate.*
- **No "remember this device" option.** Explicitly out of scope per US-003. Layout reserves no
  space for it (no `<div>` placeholder or commented-out UI) to prevent scope creep.

---

## Open Questions Raised to BA / Architect

1. **Attempt counter source of truth (AC5):** Does the API return a specific response body field
   (e.g. `attemptsRemaining`) on a 401, or does the UI track attempts client-side? Client-side
   counting is fragile (lost on page refresh). Preferred: API returns `remainingAttempts` in the
   401 body. *Fallback: UI tracks in `sessionStorage`; redirects on count reaching 0.*
2. **Third-attempt response (AC5):** When the 3rd invalid attempt is made, does the API:
   (a) return the standard 401 (UI counts to 3 and redirects), or
   (b) return a distinct status/message (e.g., 401 + a `sessionInvalidated: true` flag)?
   Option (b) is more robust. *Design assumes option (b) if available; falls back to client counter.*
3. **Resend code endpoint:** What is the API contract for requesting a new OTP? Assumed to be
   `POST /api/v1/auth/resend-otp` with the `sessionToken`. Confirm path and rate-limit behaviour.
4. **Session expiry on OTP expiry (AC2):** Does the session itself expire at 5 minutes, or only
   the OTP? If the session also expires, the "request a new code" flow must first check whether
   the session is still valid (and redirect to `/login` with an appropriate message if not).
5. **Masked phone format:** Confirm the exact masking pattern returned by the API (+*** *** **89).
   Design assumes the API supplies the masked string; the UI does NOT mask client-side.
