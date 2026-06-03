# Microcopy — US-002: Customer Login

Tone: clear, professional, calm, trust-building, never blaming. Errors say *what to do next*.
Security language is translated into plain human terms — no `2FA_REQUIRED`, `sessionToken`,
`LOCKED`, or HTTP codes are ever shown. Brand placeholder: **NorthBank**.

---

## Page header (LoginPage)

| Element | Text | Notes / Tone |
|---------|------|--------------|
| Page title (H1) | **Sign in to NorthBank** | Action-oriented, plain language |
| Subtitle | **Enter your details to continue.** | Low-effort, reassuring |
| Header link | **Create account** | For users without an account → `/register` |

---

## Field labels

| Field | Label |
|-------|-------|
| Email | **Email address** |
| Password | **Password** |

---

## Placeholder text

> Placeholders are supplementary only — labels always remain visible above the input.

| Field | Placeholder |
|-------|-------------|
| Email | `name@example.com` |
| Password | *(none — masked input)* |

---

## Inline (field-level) error messages

> Field-level errors are used **only** for empty/malformed input (error prevention).
> They are **never** used to indicate wrong credentials — that is a single, generic,
> form-level message (see below) to avoid revealing which field was wrong (AC3).

| Field | Case | Message |
|-------|------|---------|
| Email | Blank | **Enter your email address.** |
| Email | Invalid format | **Enter a valid email address, like name@example.com.** |
| Password | Blank | **Enter your password.** |

---

## Form-level messages

| Element | Text | Trigger | Variant |
|---------|------|---------|---------|
| Invalid credentials (AC3 / 401) | **Invalid email or password. Please try again.** | API returns 401 | error |
| Approaching lockout (AC4) — with count | **Invalid email or password. For your security, your account will be locked after {remaining} more failed attempts.** | 401 while within N of the limit and API exposes remaining count | warning |
| Approaching lockout (AC4) — generic fallback | **Invalid email or password. Too many failed attempts will lock your account — try resetting your password instead.** | 401 near limit when no count is available | warning |
| Server/network error | **We couldn't sign you in right now. Please try again.** | 5xx / timeout / network failure | error |

> The core "Invalid email or password" wording is fixed by AC3. We append "Please try again."
> for warmth and a clear next step, and never name a specific field.

---

## Password show/hide toggle (PasswordField)

| State | Accessible label |
|-------|------------------|
| Masked (default) | **Show password** |
| Revealed | **Hide password** |

---

## Submit button (SubmitButton)

| State | Label |
|-------|-------|
| Default | **Sign in** |
| Loading | **Signing in…** *(with spinner; `aria-busy`)* |

---

## Verification hand-off (VerificationHandoff — AC2)

| Element | Text | Notes |
|---------|------|-------|
| Heading (H1) | **Verifying it's you…** | Calm progress status; confirms login succeeded without exposing `2FA_REQUIRED` |
| Body | **We're sending a code to keep your account safe. Hang on a moment.** | Sets expectation for the US-003 OTP step |
| Fallback button (if routing stalls) | **Continue to verification** | Prevents a dead-end; routes to US-003 |

> The temporary `sessionToken` from the 200 response is held in transient state and passed to
> US-003. It is never shown to the user. This story ends at "Proceeding to verification…".

---

## Locked account state (LockedAccountNotice — AC4 / AC5)

| Element | Text | Notes |
|---------|------|-------|
| Heading (H1) | **Your account is locked** | Plain, calm statement of fact |
| Body line 1 | **For your security, we locked your account after too many failed sign-in attempts.** | Translates AC4 / 423 into human terms — explains *why*, doesn't blame |
| Body line 2 | **To unlock it, please contact NorthBank support. Our team will verify you and restore access.** | States the recovery path: unlocked by a Bank Admin (US-019), in user language |
| Primary CTA | **Contact support** | The clear forward action — no dead-end |
| Secondary link | **Forgot password?** | Helps the user prepare correct credentials (does not itself unlock) |
| Secondary link | **Back to sign in** | Returns to `/login` once the user believes the account is unlocked |

> Even with the correct password, a locked account cannot proceed (AC5); the copy makes the
> admin-unlock requirement explicit so the user doesn't keep retrying.

---

## Secondary actions (LoginForm)

| Element | Text | Destination |
|---------|------|-------------|
| Forgot-password link | **Forgot password?** | `/forgot-password` (US-004) |
| Create-account link | **New to NorthBank? Create an account** | `/register` (US-001) |
| Trust badge | **🔒 Your connection is encrypted and secure.** | — |

---

## Accessibility / writing rules applied
- Wrong credentials → one generic, form-level message; never a per-field "wrong password" (anti-enumeration, AC3).
- Every actionable message states the next step ("Please try again.", "Contact support.", "try resetting your password").
- No system/security internals surfaced (no `2FA_REQUIRED`, `sessionToken`, `LOCKED`, or HTTP codes).
- Consistent verb-first, sentence-case style across labels, errors, and buttons — matching US-001.
- Examples (`name@example.com`) given where format matters; the lockout explanation states cause and cure.
