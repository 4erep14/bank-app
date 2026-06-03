# Microcopy — US-001: Customer Self-Registration

Tone: clear, professional, trust-building, never blaming. Errors always say *how to fix*.
Brand placeholder: **NorthBank** (swap for real brand name during implementation).

---

## Page header (RegistrationPage)

| Element | Text | Notes / Tone |
|---------|------|--------------|
| Page title (H1) | **Create your account** | Action-oriented, plain language |
| Subtitle | **Join NorthBank in a couple of minutes.** | Sets a low-effort expectation |
| Header link | **Sign in** | For users who already have an account |

---

## Field labels

| Field | Label |
|-------|-------|
| First name | **First name** |
| Last name | **Last name** |
| Email | **Email address** |
| Phone | **Mobile number** |
| Date of birth | **Date of birth** |
| Password | **Password** |

---

## Placeholder text

> Placeholders are supplementary only — labels always remain visible above the input.

| Field | Placeholder |
|-------|-------------|
| First name | *(none — label suffices)* |
| Last name | *(none)* |
| Email | `name@example.com` |
| Phone | `+447911123456` |
| Date of birth | `dd / mm / yyyy` |
| Password | *(none — masked input)* |

---

## Helper text

| Field | Helper text | Purpose |
|-------|-------------|---------|
| Phone | **Use international format, e.g. +447911123456** | Translates E.164 (AC4) into a concrete example, no jargon |
| Password | **Password must contain:** *(followed by the live checklist)* | Translates complexity rule (AC3) into a visible checklist |

### Password live checklist items (PasswordChecklist)
- **At least 8 characters**
- **An uppercase letter (A–Z)**
- **A lowercase letter (a–z)**
- **A number (0–9)**
- **A special character (! ? @ # etc.)**

---

## Inline error messages

### First name / Last name
| Case | Message |
|------|---------|
| First name blank | **Enter your first name.** |
| Last name blank | **Enter your last name.** |

### Email
| Case | Message |
|------|---------|
| Blank | **Enter your email address.** |
| Invalid format | **Enter a valid email address, like name@example.com.** |
| Already registered (AC2 / 409) | **This email is already registered. Try signing in instead.** *(with adjacent "Sign in instead" link)* |

### Password (AC3)
| Case | Message |
|------|---------|
| Blank | **Enter a password.** |
| Too short (< 8) | **Password must be at least 8 characters.** |
| Missing uppercase | **Add at least one uppercase letter (A–Z).** |
| Missing lowercase | **Add at least one lowercase letter (a–z).** |
| Missing digit | **Add at least one number (0–9).** |
| Missing special char | **Add at least one special character (! ? @ # etc.).** |
| Multiple rules missing | **Password must include {missing rules joined naturally}.** e.g. "Password must include an uppercase letter and a special character." |

> The dynamic multi-rule message is assembled from the unmet checklist items so the user sees exactly what's missing. Met rules are also shown as ✓ in the checklist (recognition over recall).

### Phone (AC4)
| Case | Message |
|------|---------|
| Blank | **Enter your mobile number.** |
| Invalid E.164 format | **Enter your number in international format, starting with + (e.g. +447911123456).** |

### Date of birth
| Case | Message |
|------|---------|
| Blank | **Enter your date of birth.** |
| Out of range (future / implausible) | **Enter a valid date of birth.** |

---

## Form-level messages

| Element | Text | Trigger |
|---------|------|---------|
| Validation summary banner | **Please fix the highlighted fields below.** | One or more field errors on submit |
| Server/network error banner | **Something went wrong on our end. Please try again.** | 5xx / timeout / network failure |

---

## Submit button (SubmitButton)

| State | Label |
|-------|-------|
| Default | **Create account** |
| Loading | **Creating account…** *(with spinner; `aria-busy`)* |

---

## Secondary actions

| Element | Text | Destination |
|---------|------|-------------|
| Already-have-account link | **Already have an account? Sign in** | `/login` |
| Duplicate-email recovery | **Sign in instead** | `/login` (email prefilled if feasible) |
| Trust badge | **🔒 Your details are encrypted and secure.** | — |

---

## Success screen (RegistrationSuccess)

| Element | Text | Notes |
|---------|------|-------|
| Heading (H1) | **Your account is ready** | Confirms persistence (AC5) & 201 (AC6) in human terms |
| Body line 1 | **Welcome to NorthBank, {firstName}. We've created your account.** | `{firstName}` from form input; falls back to "Welcome to NorthBank. We've created your account." if absent |
| Body line 2 | **Next: check your email to verify your address, then sign in to start banking.** | Forward guidance only — verification flow is out of scope |
| Primary CTA | **Continue to sign in** | Routes to `/login` |
| Secondary helper | **Didn't get an email? You can resend it after signing in.** | Prevents a dead-end without building the verification flow here |

---

## Accessibility / writing rules applied
- Every error states the corrective action ("Add…", "Enter…") — never bare "Invalid input".
- No system/DB terms surfaced (no "E.164", no "PENDING_VERIFICATION", no raw exception text).
- Consistent verb-first, sentence-case style across labels, errors, and buttons.
- Examples (`name@example.com`, `+447911123456`) given wherever format matters.
