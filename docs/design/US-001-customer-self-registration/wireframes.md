# Wireframes — US-001: Customer Self-Registration

Components referenced (PascalCase): `RegistrationPage`, `RegistrationForm`, `TextField`,
`PhoneField`, `DateField`, `PasswordField`, `PasswordChecklist`, `FormErrorBanner`,
`SubmitButton`, `RegistrationSuccess`, `TrustBadge`.

Legend: `[____]` input · `(•)` focus ring · `�sp▲` spinner · `✓` valid · `✗`/`!` error · `👁` show-password toggle.

---

## 1. Registration Form — Desktop (wide, two-column name row)

```
+--------------------------------------------------------------------------+
| HEADER:  🔒 NorthBank                              [ Sign in ]           |
+--------------------------------------------------------------------------+
|                                                                          |
|                 +--------------------------------------------+           |
|                 |  Create your account                        |  <- H1   |
|                 |  Join NorthBank in a couple of minutes.     |  <- sub  |
|                 |                                             |          |
|                 |  First name            Last name            |          |
|                 |  [__________________]  [__________________] |  <- 2col |
|                 |                                             |          |
|                 |  Email address                              |          |
|                 |  [_______________________________________] |          |
|                 |                                             |          |
|                 |  Mobile number                              |          |
|                 |  [_______________________________________] |          |
|                 |  Use international format, e.g. +447911123456|  <-helper|
|                 |                                             |          |
|                 |  Date of birth                              |          |
|                 |  [ dd / mm / yyyy            📅 ]           |          |
|                 |                                             |          |
|                 |  Password                              👁    |          |
|                 |  [_______________________________________] |          |
|                 |  Password must contain:                     |          |
|                 |   ○ At least 8 characters                   |  <-check |
|                 |   ○ An uppercase letter                     |   list   |
|                 |   ○ A lowercase letter                      |          |
|                 |   ○ A number                                |          |
|                 |   ○ A special character (! ? @ # …)         |          |
|                 |                                             |          |
|                 |  [        Create account        ]           |  <- 1°   |
|                 |                                             |          |
|                 |  Already have an account?  Sign in          |  <- 2°   |
|                 |                                             |          |
|                 |  🔒 Your details are encrypted and secure.  | TrustBadge|
|                 +--------------------------------------------+           |
|                                                                          |
+--------------------------------------------------------------------------+
| FOOTER:  © 2026 NorthBank · Privacy · Terms                              |
+--------------------------------------------------------------------------+
```

- Card max-width ~480–560px, centered. First/Last name share one row at ≥768px.
- One primary action ("Create account"); "Sign in" is a secondary text link.

---

## 2. Registration Form — Mobile (single column, stacked)

```
+----------------------------------+
| 🔒 NorthBank          [ Sign in ]|
+----------------------------------+
|                                  |
|  Create your account             |  H1
|  Join NorthBank in a couple      |  sub
|  of minutes.                     |
|                                  |
|  First name                      |
|  [____________________________]  |
|                                  |
|  Last name                       |
|  [____________________________]  |
|                                  |
|  Email address                   |
|  [____________________________]  |
|                                  |
|  Mobile number                   |
|  [____________________________]  |
|  Use international format, e.g.   |
|  +447911123456                   |
|                                  |
|  Date of birth                   |
|  [ dd / mm / yyyy         📅 ]   |
|                                  |
|  Password                   👁   |
|  [____________________________]  |
|  Password must contain:          |
|   ○ At least 8 characters        |
|   ○ An uppercase letter          |
|   ○ A lowercase letter           |
|   ○ A number                     |
|   ○ A special character          |
|                                  |
|  [      Create account       ]   |  full-width 1°
|                                  |
|  Already have an account?        |
|  Sign in                         |
|                                  |
|  🔒 Your details are encrypted.  |
+----------------------------------+
| © 2026 NorthBank · Privacy       |
+----------------------------------+
```

- Single column, all fields stacked; full-width primary button (≥44px tall touch target).
- Labels persist above inputs (never placeholder-only).

---

## 3. Validation Error State (email + password + phone errors at once)

```
+----------------------------------+
|  Create your account             |
|                                  |
|  ! Please fix the highlighted    |  <- FormErrorBanner (role="alert")
|    fields below.                 |
|                                  |
|  First name                      |
|  [ Maria______________________]  ✓
|                                  |
|  Last name                       |
|  [ Petrenko___________________]  ✓
|                                  |
|  Email address                   |
|  [ maria@bank.com ___________]  !   <- red border
|  ! This email is already         |  <- inline error (role="alert")
|    registered. Try signing in.   |
|    [ Sign in instead ]           |  <- recovery action (AC2 / 409)
|                                  |
|  Mobile number                   |
|  [ 07911 123456 _____________]  !   <- red border
|  ! Enter your number in          |  <- inline error (AC4 / 400)
|    international format,          |
|    starting with + (e.g.         |
|    +447911123456).               |
|                                  |
|  Date of birth                   |
|  [ 14 / 03 / 1990         📅 ]  ✓
|                                  |
|  Password                   👁   |
|  [ passw0rd _________________]  !   <- red border
|  Password must contain:          |
|   ✓ At least 8 characters        |  met = green check
|   ✗ An uppercase letter          |  unmet = red, not color-only (✗ icon)
|   ✓ A lowercase letter           |
|   ✓ A number                     |
|   ✗ A special character          |
|  ! Add an uppercase letter and a |  <- inline error (AC3 / 400)
|    special character.            |
|                                  |
|  [      Create account       ]   |
+----------------------------------+
```

- Errors never rely on color alone: red border **+** `!`/`✗` icon **+** text.
- Focus moves to the first errored field; banner summarizes for screen readers.

---

## 4. Submitting State (form disabled, button spinner)

```
+----------------------------------+
|  Create your account             |
|                                  |
|  First name                      |
|  [ Maria______________________]  (disabled, dimmed)
|  Last name                       |
|  [ Petrenko___________________]  (disabled, dimmed)
|  Email address                   |
|  [ maria.p@northmail.com _____]  (disabled, dimmed)
|  Mobile number                   |
|  [ +447911123456 ____________]  (disabled, dimmed)
|  Date of birth                   |
|  [ 14 / 03 / 1990         📅 ]  (disabled, dimmed)
|  Password                        |
|  [ ••••••••••• ______________]  (disabled, dimmed)
|                                  |
|  [   �sp▲  Creating account…  ]   |  <- SubmitButton: spinner + label,
|                                  |     aria-busy="true", disabled
|  Already have an account? Sign in|  (links disabled)
+----------------------------------+
```

- Entire form is non-interactive to prevent double submission (`aria-busy` on form).
- Button keeps its width (no layout shift); label changes to "Creating account…".

---

## 5. Success Screen (RegistrationSuccess)

```
+----------------------------------+
| 🔒 NorthBank                     |
+----------------------------------+
|                                  |
|            ( ✓ )                 |  <- success icon (decorative, aria-hidden)
|                                  |
|     Your account is ready        |  <- H1
|                                  |
|  Welcome to NorthBank, Maria.    |
|  We've created your account.     |  <- body, confirms AC5/AC6 outcome
|                                  |
|  Next: check your email to       |
|  verify your address, then sign  |  <- forward guidance (verification
|  in to start banking.            |     flow itself is out of scope)
|                                  |
|  [    Continue to sign in    ]   |  <- primary CTA -> /login
|                                  |
|  Didn't get an email? You can    |
|  resend it after signing in.     |  <- secondary helper (no dead-end)
|                                  |
+----------------------------------+
| © 2026 NorthBank                 |
+----------------------------------+
```

- Single primary action: "Continue to sign in".
- First name personalization optional (from form input, not from API).
- This is also the **first-run / completion** experience — designed on purpose to confirm success and route the user onward.

---

## Component map

| Component | Purpose | Key props | Events |
|-----------|---------|-----------|--------|
| `RegistrationPage` | Route container for `/register` | — | — |
| `RegistrationForm` | Hosts fields, validation, submit | `onSubmit(values)`, `isSubmitting`, `serverErrors` | `onSubmit`, `onFieldBlur` |
| `TextField` | Reusable labelled text input | `label`, `name`, `value`, `error`, `autocomplete`, `disabled` | `onChange`, `onBlur` |
| `PhoneField` | Tel input + E.164 helper | `label`, `value`, `error`, `helperText`, `disabled` | `onChange`, `onBlur` |
| `DateField` | Native date input w/ min/max | `label`, `value`, `error`, `min`, `max`, `disabled` | `onChange`, `onBlur` |
| `PasswordField` | Password input + show/hide toggle | `label`, `value`, `error`, `disabled`, `isVisible` | `onChange`, `onBlur`, `onToggleVisibility` |
| `PasswordChecklist` | Live complexity rule list | `rules: {met:boolean,label:string}[]` | — |
| `FormErrorBanner` | Summary/server-error banner | `message` | — |
| `SubmitButton` | Primary action w/ states | `label`, `loadingLabel`, `isLoading`, `disabled` | `onClick` |
| `RegistrationSuccess` | Confirmation screen | `firstName?` | `onContinue` |
| `TrustBadge` | Encryption reassurance line | `text` | — |
