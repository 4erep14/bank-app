# UX Flow — US-001: Customer Self-Registration

> AC coverage: AC1 (form fields), AC2 (409 duplicate email), AC3 (password complexity 400),
> AC4 (phone E.164 400), AC5 (persist PENDING_VERIFICATION), AC6 (201 + customer id).

---

## Screens / Views Involved

- **Screen A — RegistrationPage (`/register`):** The single-page registration form. Hosts all 6 fields (AC1), client-side validation, and the primary "Create account" action.
- **Screen B — RegistrationSuccess (`/register/success`):** Confirmation screen shown after a 201 response (AC6). Confirms the account was created (AC5) and points the user to the next step (check email / proceed to login).
- **Entry points:** Marketing site CTA, and Login screen link ("New here? Create an account").
- **Exit points:** Login screen (primary onward path), or "Sign in instead" recovery on duplicate email (AC2).

---

## User Journey (high level)

```
[Arrive at /register]
        |
        v
[See empty RegistrationForm + password/phone helpers]
        |
        v
[Fill 6 fields: first name, last name, email, phone, DOB, password]   <-- AC1
        |
        v
[Inline validation on blur — fix errors in context]                   <-- AC3, AC4 (client-side)
        |
        v
[Press "Create account"]
        |
        v
[Form disables -> button shows "Creating account…" spinner]           <-- visible status
        |
        v
   { API call POST /api/v1/customers }
        |
        +--> 201 Created (+ id) --------------------> [RegistrationSuccess]   <-- AC5, AC6
        |
        +--> 409 Conflict ("Email already registered") --> [Inline error on Email + "Sign in instead"]  <-- AC2
        |
        +--> 400 Bad Request (field errors) --------> [Inline field errors: password / phone]  <-- AC3, AC4
        |
        +--> 5xx / Network failure -----------------> [Form-level banner + "Try again", data preserved]
```

---

## Happy Path — valid submission (AC1 → AC5 → AC6)

```
[Empty form]
   -> User enters first name, last name, email, phone (+447911123456), DOB, password (Passw0rd!$)
   -> Each field passes inline validation (green/valid state)
   -> "Create account" becomes the clear primary action
   -> User submits
        |
        v
   [Submitting state] : form fields disabled, button = spinner + "Creating account…"
        |
        v
   POST /api/v1/customers  { firstName, lastName, email, phone, dateOfBirth, password }   <-- AC1
        |
        v
   API persists Customer with status = PENDING_VERIFICATION                                 <-- AC5
   API responds 201 Created  { id: "<uuid>" }                                               <-- AC6
        |
        v
   [RegistrationSuccess screen]
     - Heading: "Your account is ready"
     - Body: confirms account created, "check your email" forward guidance
     - Primary CTA: "Continue to sign in"  -> /login
```

**Steps to success:** Land → fill 6 fields → submit → confirmation. No detours on the happy path.

---

## Duplicate Email Path (AC2 — 409 Conflict)

```
[User submits a form whose email is already registered]
        |
        v
   [Submitting state]
        |
        v
   POST /api/v1/customers
        |
        v
   API responds 409 Conflict  { message: "Email already registered" }                       <-- AC2
        |
        v
   [Form re-enabled]
   -> Inline error rendered UNDER the Email field (red border + role="alert"):
        "This email is already registered. Try signing in instead."
   -> Email field receives focus
   -> A recovery link/button appears next to the message: "Sign in instead" -> /login
   -> All other entered data is preserved (user does not re-type anything)
```

**Recovery:** The user can correct the email, or follow "Sign in instead" — no dead-end.

---

## Validation Error Paths

### Password fails complexity (AC3 — 400)

```
Client-side (preferred — error prevention):
   [User leaves Password field with "passw0rd" (no uppercase, no special char)]
        -> Live checklist shows unmet rules in red:
             ✗ One uppercase letter
             ✗ One special character
        -> On submit attempt, submit is blocked; focus moves to Password; inline error:
             "Password must include an uppercase letter and a special character."

Server-side (defense in depth — if a non-compliant value reaches the API):
   POST /api/v1/customers
        -> API responds 400 with field-level error for "password"                            <-- AC3
        -> Inline error rendered under Password field (role="alert"),
           text mapped from the violated rule(s); checklist reflects the failing rules.
        -> Password field receives focus.
```

Per-rule messages (blank, too short, missing uppercase/lowercase/digit/special) are in `microcopy.md`.

### Phone invalid E.164 format (AC4 — 400)

```
Client-side:
   [User leaves Phone field with "07911 123456" (not E.164)]
        -> Inline error under Phone:
             "Enter your number in international format, starting with + (e.g. +447911123456)."

Server-side:
   POST /api/v1/customers
        -> API responds 400 with field-level error for "phone"                               <-- AC4
        -> Inline error rendered under Phone field (role="alert").
        -> Phone field receives focus.
```

### Multiple simultaneous errors

```
[Submit with bad email + weak password + bad phone]
   -> Server returns 400/409 with field map
   -> Each error rendered under its own field at the same time (see wireframes #3)
   -> A polite summary line appears above the form for screen readers / overview:
        "Please fix the highlighted fields below."
   -> Focus moves to the FIRST errored field in DOM order.
```

---

## Network / Server Failure Path (non-AC, resilience)

```
[Submit] -> request fails (timeout / 5xx)
   -> Form re-enabled, data preserved
   -> Non-blocking banner at top of form (role="alert"):
        "Something went wrong on our end. Please try again."
   -> Primary button returns to "Create account"; user can resubmit.
```

---

## Notes / Open Questions raised to BA
- **Confirm-password field:** intentionally omitted (see ux-rationale). Confirm whether security policy mandates it.
- **`PENDING_VERIFICATION` status (AC5)** is not shown to the user; the success copy only hints at email verification (which is out of scope). Confirm this is acceptable forward-guidance wording.
