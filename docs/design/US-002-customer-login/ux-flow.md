# UX Flow — US-002: Customer Login

> AC coverage: AC1 (submit email + password to `POST /api/v1/auth/login`),
> AC2 (200 → `2FA_REQUIRED` + `sessionToken`, hand off to verification),
> AC3 (401 → generic "Invalid email or password"),
> AC4 (5 failed attempts → 423 lockout), AC5 (LOCKED cannot log in; admin unlock).

---

## Screens / Views Involved

- **Screen A — LoginPage (`/login`):** The single-page sign-in form. Hosts the email + password fields (AC1), the primary "Sign in" action, inline/form-level error handling (AC3, AC4), and recovery links (forgot password, register).
- **Transitional state — VerificationHandoff:** Not a separate route; an in-place success state shown after a 200 `2FA_REQUIRED` response (AC2). Confirms the credentials were accepted and that a code is being sent, then routes to the US-003 OTP screen. **The flow for this story ends at "Proceeding to verification…" — the OTP entry screen is US-003's responsibility.**
- **Locked state — LockedAccountNotice:** An in-page state (replaces the form body) shown on a 423 response (AC4) or when a known-locked account attempts login (AC5). Explains the lock and the unlock path.
- **Entry points:** Header "Sign in" link, marketing site, RegistrationSuccess ("Continue to sign in"), and redirected deep links for unauthenticated users.
- **Exit points:** US-003 verification (primary onward path, AC2), `/forgot-password` (US-004 recovery), `/register` (US-001), support contact (lockout recovery, AC5).

---

## User Journey (high level)

```
[Arrive at /login]
        |
        v
[See empty LoginForm: Email, Password, "Sign in", "Forgot password?"]
        |
        v
[Enter email + password]                                              <-- AC1
        |
        v
[Press "Sign in"]
        |
        v
[Form disables -> button shows "Signing in…" spinner]                 <-- visible status
        |
        v
   { API call POST /api/v1/auth/login }                               <-- AC1
        |
        +--> 200 { status:"2FA_REQUIRED", sessionToken } --> [VerificationHandoff: "Verifying it's you…"]
        |                                                     -> route to US-003 OTP screen   <-- AC2
        |
        +--> 401 "Invalid email or password" ----> [Form-level error + cleared password]      <-- AC3
        |          (after enough failures, show "attempts remaining" warning)                  <-- AC4 (pre-lock)
        |
        +--> 423 "Account locked due to too many failed attempts" --> [LockedAccountNotice]    <-- AC4
        |
        +--> (account already LOCKED) 423 -----------------------> [LockedAccountNotice]        <-- AC5
        |
        +--> 5xx / Network failure --------------> [Form-level banner + "Try again", email kept, password cleared]
```

---

## Happy Path — valid credentials (AC1 → AC2)

```
[Empty LoginForm]
   -> User enters email (maria.p@northmail.com) and password
   -> (optionally taps 👁 to confirm the password is correct)
   -> "Sign in" is the clear primary action
   -> User submits (click or Enter)
        |
        v
   [Submitting state] : fields disabled, button = spinner + "Signing in…"
        |
        v
   POST /api/v1/auth/login  { email, password }                          <-- AC1
        |
        v
   API validates credentials -> 200 OK
        { "status": "2FA_REQUIRED", "sessionToken": "<temp-token>" }      <-- AC2
        |
        v
   [VerificationHandoff state]
     - Replaces the button area / form with a calm confirmation
     - Spinner + "Verifying it's you… We're sending a code to keep your account safe."
     - sessionToken held in transient app state (NEVER displayed)
        |
        v
   -> Navigate to US-003 verification screen, passing the sessionToken
   *** This story ends here: "Proceeding to verification…" ***
```

**Steps to success:** Land → enter 2 fields → submit → "Verifying it's you…" → US-003. No detours on the happy path.

---

## Invalid Credentials Path (AC3 — 401)

```
[User submits wrong email and/or wrong password]
        |
        v
   [Submitting state]
        |
        v
   POST /api/v1/auth/login
        |
        v
   API responds 401  { message: "Invalid email or password" }            <-- AC3
        |
        v
   [Form re-enabled]
   -> A single FORM-LEVEL error appears above the fields (role="alert"):
        "Invalid email or password. Please try again."
   -> The error does NOT say which field was wrong (no enumeration).
   -> Email is PRESERVED; Password field is CLEARED and re-focused.
   -> "Forgot password?" remains visible as the recovery path.
```

**Recovery:** Re-enter password and retry, or follow "Forgot password?" (US-004) — no dead-end.

---

## Approaching Lockout (AC4 — pre-lock warning)

```
[User keeps failing with 401s]
   -> Once the user is within N attempts of the limit (e.g. after the 3rd failure),
      the form-level message gains a calm warning line (role="alert"):
        "Invalid email or password. For your security, your account will be
         locked after {remaining} more failed attempts."
   -> "Forgot password?" is emphasised as the safer route before lockout.
```

> **Open question to BA / Architect:** The exact attempt number at which the warning begins,
> and whether the API returns a "remaining attempts" value, must be confirmed. If the API does
> NOT expose remaining attempts, the warning is shown generically ("Too many failed attempts
> will lock your account — try resetting your password instead.") without a live count.

---

## Account Locked Path (AC4 — 423 on the 5th failure)

```
[User submits the 5th consecutive failed attempt]
        |
        v
   [Submitting state]
        |
        v
   POST /api/v1/auth/login
        |
        v
   API sets account status = LOCKED and responds
   423  { message: "Account locked due to too many failed attempts" }     <-- AC4
        |
        v
   [LockedAccountNotice state]  (replaces the form body)
     - Heading: "Your account is locked"
     - Body: explains it was locked after too many failed sign-in attempts,
       for the customer's protection.
     - Recovery: "To unlock it, contact NorthBank support." (unlocked by a
       Bank Admin — described in human terms, US-019).             <-- AC5 preview
     - Secondary: "Forgot password?" still offered (does not unlock, but helps
       the user prepare correct credentials for after unlock).
     - The Email/Password inputs are removed so the user cannot keep hammering
       a locked account.
```

---

## Locked Account Re-attempt Path (AC5 — already LOCKED)

```
[A customer whose account is ALREADY locked enters correct OR incorrect credentials]
        |
        v
   POST /api/v1/auth/login
        |
        v
   API rejects because status = LOCKED -> 423
   { message: "Account locked due to too many failed attempts" }          <-- AC5
        |
        v
   [LockedAccountNotice state] (same as above)
     - Even with the *correct* password, a LOCKED account cannot proceed.
     - Only a Bank Admin unlock (US-019) restores access; the UI states this clearly.
     - No path from this screen logs the user in — the only forward action is
       "Contact support" (and optionally "Back to sign in" once they believe
       the account has been unlocked).
```

**No dead-end:** the locked screen always offers "Contact support" and a way back to `/login`.

---

## Network / Server Failure Path (non-AC, resilience)

```
[Submit] -> request fails (timeout / 5xx)
   -> Form re-enabled
   -> Email PRESERVED; Password CLEARED (never retain a secret across a failure)
   -> Non-blocking banner at top of form (role="alert"):
        "We couldn't sign you in right now. Please try again."
   -> Primary button returns to "Sign in"; user can resubmit.
   -> A failed request due to network error must NOT be counted as a credential
      failure toward lockout (flagged to Architect — lockout counts only true 401s).
```

---

## Notes / Open Questions raised to BA / Architect

- **Pre-lockout warning threshold (AC4):** Confirm at which failed attempt the warning starts and whether the API returns remaining-attempts. Falls back to a generic warning if not available.
- **Lock reason visibility (AC4/AC5):** Confirm it's acceptable to tell the user *why* they're locked and that an admin must unlock (US-019). Design assumes yes for trust/anti-panic reasons.
- **Support recovery channel (AC5):** Confirm the exact unlock channel/copy ("contact support" vs a self-service request). The unlock action itself is out of scope (US-019).
- **Network failures vs lockout count:** Confirm with Architect that only genuine 401s increment the failed-attempt counter, not 5xx/timeouts.
- **sessionToken handling (AC2):** Held in transient state and passed to US-003; never displayed or persisted to durable storage in this story.
