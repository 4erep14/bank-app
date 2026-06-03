# UX Flow — US-003: Two-Factor Authentication via SMS

> AC coverage:
> - **AC1** — OTP entry screen shown after US-002 password check (arrives with `sessionToken`)
> - **AC2** — expired OTP returns 401 "Invalid or expired OTP"; UI shows error + resend path
> - **AC3** — valid OTP → 200 + JWT access token + refresh token → stored → navigate to dashboard
> - **AC4** — invalid OTP → 401 "Invalid or expired OTP" → inline error + attempts remaining
> - **AC5** — 3 consecutive invalid OTP attempts → session invalidated → redirect to `/login`
>              with message "Too many attempts. Please sign in again."

---

## Screens / Views Involved

- **Entry point — US-002 `VerificationHandoff`:** The auto-route from US-002 when the password is
  accepted (200 `2FA_REQUIRED`). The `sessionToken` is held in `sessionStorage`. *This story
  starts here — US-003 owns the screen from this point forward.*

- **Screen A — `OtpVerificationPage` (`/verify-otp`):**
  The primary and only screen in this story. A single-task, full-focus page containing:
  - Contextual header confirming where the code was sent (masked phone number).
  - `OtpInputGroup` — 6 individual digit boxes with auto-advance and auto-submit.
  - `ResendCodeControl` — countdown timer + "Resend code" link/button.
  - `FormErrorBanner` — shared component from US-002; shows invalid/expired errors with
    remaining-attempts count.
  - `SubmitButton` ("Verify") — visible for manual submit; auto-triggered on fill.
  - "Back to sign in" text link — escape hatch to exit the flow.
  - `TrustBadge` — shared from US-002.

- **In-page transient states on `OtpVerificationPage`:**
  - **Submitting:** all inputs disabled, spinner, "Verifying…".
  - **Error — invalid/expired (AC2/AC4):** error banner, attempts remaining, boxes cleared.
  - **Near-lockout warning (AC4):** same as error but amber warning banner with `1 attempt remaining`.
  - **Success (AC3):** brief green confirmation → auto-navigate to `/dashboard`.
  - **Resend in-flight:** resend button disabled + spinner; confirmation on success.

- **Exit — `/dashboard`:** Post-AC3 success destination. Tokens stored in `localStorage`
  (access token) and an httpOnly cookie (refresh token — handled by the API). US-003 ends here.

- **Exit — `/login` with flash message (AC5):** On session invalidation after 3 failures.
  The `sessionToken` is cleared from `sessionStorage`. The `/login` page renders a transient
  flash/banner: "Too many attempts. Please sign in again."

- **Guard — redirect to `/login` on direct access:** If the user navigates to `/verify-otp`
  without a valid `sessionToken` in `sessionStorage`, immediately redirect to `/login`. No
  error is shown — the login page's default state handles the scenario naturally.

---

## User Journey (full map)

```
[Arrive at /verify-otp via US-002 VerificationHandoff]
  (sessionToken stored in sessionStorage)              <-- AC1
        |
        v
[OtpVerificationPage — Default State]
  "We sent a 6-digit code to +*** *** **89"
  [ _ ][ _ ][ _ ][ _ ][ _ ][ _ ]   OtpInputGroup
  Resend in 0:59  (ResendCodeControl — cooldown)
  [        Verify        ]  SubmitButton
  Back to sign in
        |
        v
[User enters 6 digits (keyboard / SMS autofill / paste)]
  → auto-advance box-to-box
  → 6th digit filled → auto-submit (300ms debounce)   <-- or manual "Verify" press
        |
        v
[OtpInputGroup disabled + "Verifying…" spinner]        <-- visible status
        |
        v
   { API call: POST /api/v1/auth/verify-otp }
   { Header: Authorization: Bearer <sessionToken> }
   { Body: { "otp": "123456" } }
        |
        +──── 200 OK ─────────────────────────────────────────────────────────────────────>
        |     { "accessToken": "...", "refreshToken": "..." }              <-- AC3
        |     → Store accessToken in localStorage
        |     → API sets refreshToken as httpOnly cookie
        |     → Show brief success state ("Identity verified! ✓")
        |     → Navigate to /dashboard
        |
        +──── 401 "Invalid or expired OTP" (attempt 1 or 2) ─────────────────────────────>
        |     → Re-enable OtpInputGroup (cleared) + focus on box 1        <-- AC4
        |     → FormErrorBanner (error): "Incorrect or expired code.
        |       Check your SMS and try again. You have X attempts remaining."
        |     → If resend cooldown has expired: "Resend code" active
        |
        +──── 401 "Invalid or expired OTP" (attempt 3 → session invalidated) ────────────>
        |     → Clear sessionToken from sessionStorage                     <-- AC5
        |     → Navigate to /login
        |     → Flash banner on /login: "Too many attempts. Please sign in again."
        |
        +──── 5xx / Network failure ──────────────────────────────────────────────────────>
              → Re-enable OtpInputGroup (NOT cleared — user's effort preserved)
              → FormErrorBanner (error): "We couldn't verify your code right now. Please try again."
              → Failure NOT counted toward the 3-attempt limit
```

---

## Happy Path — valid OTP (AC1 → AC3)

```
[OtpVerificationPage loads with masked phone number visible]
   → User glances at SMS: code is "4 8 2 9 1 7"
   → (Ideal case: OS/browser autofill suggests the code directly — one tap fills all 6)
   → Or: User types digits; focus auto-advances with each keystroke:
       Box 1: "4" → focus moves to box 2
       Box 2: "8" → focus moves to box 3
       Box 3: "2" → focus moves to box 4
       Box 4: "9" → focus moves to box 5
       Box 5: "1" → focus moves to box 6
       Box 6: "7" → auto-submit fires after 300ms debounce
         |
         v
   [All 6 boxes disabled; SubmitButton: spinner + "Verifying…"]
         |
         v
   POST /api/v1/auth/verify-otp → 200 OK                               <-- AC3
   { "accessToken": "eyJ…", "refreshToken": "eyJ…" }
         |
         v
   → accessToken stored in localStorage
   → refreshToken in httpOnly cookie (set by API)
   → OtpInputGroup shows green checkmark overlay briefly
   → "Identity verified! Redirecting…" (aria-live="polite")
         |
         v
   Navigate to /dashboard
   *** US-003 ends here — user is fully authenticated ***
```

**Steps to success:** Land → 6 keystrokes (or 1 autofill tap) → auto-submit → brief success → dashboard. This is the absolute minimum viable authentication step count.

---

## Invalid OTP Path — first or second failure (AC4)

```
[User enters 6 digits (wrong code) → auto-submit]
         |
         v
   POST /api/v1/auth/verify-otp → 401
   { "message": "Invalid or expired OTP" }                              <-- AC4
         |
         v
   → OtpInputGroup re-enabled; all boxes CLEARED; focus on box 1
   → FormErrorBanner (error, role="alert"):
       "Incorrect or expired code. Check your SMS and try again.
        You have 2 attempts remaining."
   → SubmitButton back to "Verify" (enabled, ready)
   → If ResendCodeControl cooldown has elapsed: link becomes active
         |
         v
   [User reads the SMS again more carefully, types the correct code]
         |
         v
   → Proceeds to Happy Path → success
```

**Recovery:** Re-read SMS → retype → success. If SMS not received: resend code (after cooldown).

---

## Expired OTP Path (AC2) — visual treatment

> The API returns the same `401 + "Invalid or expired OTP"` for both expired and invalid codes.
> The UI cannot distinguish between AC2 and AC4 from the response alone. The error message is
> designed to cover both and direct the user to either retype OR request a new code.

```
[User took > 5 minutes to enter the code (tab-switched, got distracted)]
         |
         v
   POST /api/v1/auth/verify-otp → 401
   { "message": "Invalid or expired OTP" }                              <-- AC2
         |
         v
   → Same visual treatment as AC4 (above)
   → FormErrorBanner: "Incorrect or expired code. Check your SMS and try again.
      You have X attempts remaining."
   → If ResendCodeControl cooldown has elapsed (very likely after 5+ min):
       "Resend code" link is active — the primary recovery path for an expired code
```

**Recovery:** Tap "Resend code" → get a fresh code → enter it within 5 minutes.

---

## Near-Lockout Warning Path (AC4/AC5 — 2nd failure, 1 attempt remaining)

```
[User has already failed once; submits a second wrong code]
         |
         v
   POST /api/v1/auth/verify-otp → 401 (2nd failure, 1 remaining)       <-- AC4
         |
         v
   → OtpInputGroup cleared; focus on box 1
   → FormErrorBanner (WARNING variant — amber, not red):
       "⚠ Incorrect or expired code. This is your last attempt — after one more
        incorrect entry, you'll need to sign in again from the beginning.
        Consider requesting a new code below."
   → ResendCodeControl made more visually prominent (bold / underlined link)
   → SubmitButton enabled ("Verify")
```

**Recovery options surfaced clearly:**
1. Carefully re-read the SMS and enter the code (last chance).
2. Request a new code (strongly recommended by the copy) — resets the attempt counter (*flagged to Architect: confirm whether resend resets the attempt counter*).

---

## Session Invalidation Path (AC5 — 3rd consecutive failure)

```
[User submits a third consecutive wrong code]
         |
         v
   POST /api/v1/auth/verify-otp → 401 (3rd failure → session invalidated)  <-- AC5
   (API invalidates the session server-side)
         |
         v
   → Clear sessionToken from sessionStorage
   → Set a one-time flash message in sessionStorage:
       "Too many attempts. Please sign in again."                        <-- AC5 exact message
   → Navigate immediately to /login
         |
         v
   [/login page renders]
   → FormErrorBanner at top of LoginForm (role="alert", variant="warning"):
       "Too many attempts. Please sign in again."
   → Flash message cleared from sessionStorage after first render
   → User must complete the full US-002 login flow again from scratch
```

**No dead-end:** `/login` is a fully functional page with the standard recovery options
(Forgot password, Create account).

---

## Resend Code Path (happy resend)

```
[OtpVerificationPage: ResendCodeControl cooldown has elapsed]
   → "Resend code" link is active (previously disabled with countdown)
         |
[User taps "Resend code"]
         |
         v
   → ResendCodeControl: spinner + "Sending…" (link disabled during request)
         |
         v
   POST /api/v1/auth/resend-otp → 200 OK                                <-- stub / stubbed SMS
         |
         v
   → Cooldown resets to 60 seconds ("Resend in 0:59")
   → Inline success message (role="status"):
       "A new code has been sent to +*** *** **89."
   → OtpInputGroup: all boxes cleared; focus on box 1
   → Existing code in OtpInputGroup is wiped (previous code is now invalid)
```

---

## Resend Code Path — API failure

```
[User taps "Resend code" → network error or 5xx]
         |
         v
   → ResendCodeControl link re-enabled (no cooldown set — the resend didn't succeed)
   → Inline error message below the control (role="alert"):
       "We couldn't send a new code. Please try again."
   → Original boxes preserved (user can still try the original code if unexpired)
```

---

## "Back to Sign In" Exit Path (user-initiated abandonment)

```
[User clicks "Back to sign in"]
         |
         v
   → Clear sessionToken from sessionStorage
   → Navigate to /login
   → No error banner; /login shows its clean default state
   → User may sign in again from scratch
```

---

## Direct-URL Guard (no sessionToken)

```
[User navigates directly to /verify-otp without a sessionToken]
         |
         v
   → Route guard detects missing/invalid sessionToken in sessionStorage
   → Immediately redirect to /login (no error message — default login state is appropriate)
```

---

## Network / Server Failure Path (resilience — not an AC)

```
[OTP submitted → request fails: timeout / 5xx]
         |
         v
   → OtpInputGroup re-enabled; boxes PRESERVED (user's input kept — not a security secret
     in the same way as a password, and retaining it reduces friction on a transient error)
   → FormErrorBanner (error, role="alert"):
       "We couldn't verify your code right now. Please try again."
   → SubmitButton returns to "Verify" (enabled)
   → Attempt counter NOT incremented (server-side: only true 401s count toward AC5)
```

---

## Notes / Open Questions raised to Architect

1. **Attempt counter (AC5):** Confirm whether the `verify-otp` 401 response body includes
   `remainingAttempts: number`. If yes, the UI uses it directly. If not, the UI tracks attempts
   in `sessionStorage`. Preferred: API includes `remainingAttempts`.
2. **Session invalidation signal (AC5):** When the 3rd attempt is made, does the API return a
   standard 401 (UI redirects based on count = 0) or a distinct signal such as
   `{ "sessionInvalidated": true }` in the body? The latter is more robust.
3. **Resend OTP endpoint:** Confirm `POST /api/v1/auth/resend-otp` path, request/response shape,
   and whether a successful resend resets the `remainingAttempts` counter.
4. **Session expiry vs OTP expiry (AC2):** Does the `sessionToken` itself expire at 5 minutes
   alongside the OTP, or does the session stay alive for longer? If the session can expire
   independently, the UI must handle an additional error case: `sessionToken` expired → redirect
   to `/login` with message "Your session has expired. Please sign in again."
5. **Masked phone from API:** Confirm the API returns the masked number string. The UI must not
   mask client-side (it doesn't have access to the full number post-login).
6. **Token storage (AC3):** Confirm that the `accessToken` goes to `localStorage` and the
   `refreshToken` is set as an httpOnly cookie by the server. If both are in the response body,
   the UI must handle `refreshToken` storage — flag to Frontend Dev.
