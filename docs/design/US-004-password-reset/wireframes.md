# Wireframes — US-004: Password Reset

> Mobile-first ASCII wireframes. Single-column, centred card layout on a NorthBank-branded page.
> One primary action per screen. Reuses US-001 components (PasswordField, PasswordChecklist,
> SubmitButton, FormErrorBanner, TrustBadge). Desktop = same card centred on a wider canvas.

Legend: `[____]` input · `( Button )` primary · `‹ link ›` text link · `○ ✓ ✗` checklist states
· `🔒` trust badge · `!` error icon · `⟳` spinner.

---

## Screen 1 — ForgotPasswordForm  (`/forgot-password`)  — AC1, AC2

```
+------------------------------------------+
|            NorthBank   (logo)            |
+------------------------------------------+
|                                          |
|   Forgot your password?            (H1)  |
|   Enter the email address linked to      |
|   your account and we'll send you a      |
|   link to reset your password.           |
|                                          |
|   Email address                          |
|   [______________________________]       |
|   We'll email a reset link if this       |
|   address is registered.        (helper) |
|                                          |
|   (        Send reset link        )      |
|                                          |
|   ‹ Back to sign in ›                     |
|                                          |
|   🔒 We never share your email address.  |
+------------------------------------------+
```

**Hierarchy:** H1 → single email field → one primary button → secondary "Back to sign in".
**Primary action:** `Send reset link`. Only one field — lowest possible friction.

---

## Screen 1 (error state) — invalid / empty email (client-side prevention)

```
+------------------------------------------+
|   Forgot your password?            (H1)  |
|                                          |
|   Email address                          |
|   [ john.doe@@example ............] !     |   ← red border + ! icon
|   ! Enter a valid email address, like    |   ← role="alert"
|     name@example.com.                     |
|                                          |
|   (        Send reset link        )      |
|   ‹ Back to sign in ›                     |
+------------------------------------------+
```

---

## Screen 1 (loading state) — request in flight

```
|   (   ⟳  Sending…   )   ← disabled, aria-busy
```

---

## Screen 2 — ForgotPasswordConfirmation  (`/forgot-password/sent`)  — AC1

```
+------------------------------------------+
|            NorthBank   (logo)            |
+------------------------------------------+
|                                          |
|              ✉  (icon)                    |
|                                          |
|   Check your email                 (H1)  |
|                                          |
|   If that email is registered, we've     |
|   sent a link to reset your password.    |
|                                          |
|   The link expires in 1 hour. Don't      |
|   see it? Check your spam folder.        |
|                                          |
|   To protect your account, we don't      |
|   reveal whether an email is             |
|   registered.                   (helper) |
|                                          |
|   ( Resend email )                        |   ← secondary button
|                                          |
|   ‹ Use a different email ›               |
|   ‹ Back to sign in ›                     |
+------------------------------------------+
```

> **Anti-enumeration (AC1):** copy is identical whether or not the email exists. No address is
> echoed back, and the helper line explains *why* — turning a non-confirmation into a trust signal.

---

## Screen 3 — ResetPasswordForm  (`/reset-password?token=…`)  — AC3, AC4

```
+------------------------------------------+
|            NorthBank   (logo)            |
+------------------------------------------+
|                                          |
|   Set a new password               (H1)  |
|   Choose a new password for your         |
|   NorthBank account.                     |
|                                          |
|   New password                           |
|   [____________________]  👁 Show         |
|                                          |
|   Your password must contain:            |
|     ○ At least 8 characters              |
|     ○ An uppercase letter (A–Z)          |
|     ○ A lowercase letter (a–z)           |
|     ○ A number (0–9)                     |
|     ○ A special character (! ? @ # etc.) |
|        (PasswordChecklist — live)        |
|                                          |
|   Confirm new password                   |
|   [____________________]  👁 Show         |
|                                          |
|   (       Save new password       )      |
|                                          |
|   ‹ Back to sign in ›                     |
|   🔒 Your password is encrypted.         |
+------------------------------------------+
```

**Live checklist:** each `○` flips to `✓` (met) or shows `✗` (unmet after interaction). Icon + text
+ colour — never colour alone. **Primary action:** `Save new password`.

### Screen 3 — typing progress (checklist updating)

```
|   New password                           |
|   [ Passw0 ............]  👁 Show          |
|     ✓ At least 8 characters? ✗ (6 so far) |
|     ✓ An uppercase letter (A–Z)          |
|     ✓ A lowercase letter (a–z)           |
|     ✓ A number (0–9)                     |
|     ✗ A special character (! ? @ # etc.) |
```

### Screen 3 — error state (AC3 complexity fail / mismatch on submit)

```
|   New password                           |
|   [ ******* .........]  👁 Show       !   |   ← red border
|   ! Add at least one special character   |   ← role="alert"
|     (! ? @ # etc.).                       |
|     ○ ... ✗ A special character (...)     |   ← checklist mirrors failure
|                                          |
|   Confirm new password                   |
|   [ ******** ........]  👁 Show       !   |
|   ! Passwords don't match. Re-enter to   |
|     confirm.                              |
|                                          |
|   (       Save new password       )      |
```

### Screen 3 — loading state

```
|   (   ⟳  Saving…   )   ← disabled, aria-busy
```

---

## Screen 4 — ResetPasswordSuccess  (`/reset-password/success`)  — AC4, AC5

```
+------------------------------------------+
|            NorthBank   (logo)            |
+------------------------------------------+
|                                          |
|              ✓  (success icon)           |
|                                          |
|   Your password has been reset    (H1)   |
|                                          |
|   You can now sign in with your new      |
|   password.                              |
|                                          |
|   For your security, we've signed you    |
|   out of all devices.           (AC5)    |
|                                          |
|   (      Continue to sign in      )      |
|                                          |
+------------------------------------------+
```

> Success H1 receives focus on mount; the AC5 sign-out-everywhere note pre-empts a confusing
> "logged out elsewhere" surprise and frames it as protection.

---

## Screen 5 — ResetTokenError  (invalid / expired / already-used token)  — AC6, AC7

```
+------------------------------------------+
|            NorthBank   (logo)            |
+------------------------------------------+
|                                          |
|              ⚠  (alert icon)             |
|                                          |
|   This reset link has expired     (H1)   |
|                                          |
|   Password reset links are single-use    |
|   and expire 1 hour after they're sent.  |
|   This one is no longer valid.           |
|                                          |
|   No problem — request a new one and     |
|   we'll email you a fresh link.          |
|                                          |
|   (       Request a new link      )      |
|                                          |
|   ‹ Back to sign in ›                     |
|   ‹ Contact support ›                     |
+------------------------------------------+
```

> One screen serves all token failures (expired **AC6**, already-used single-use **AC7**, malformed/
> invalid). Human-readable cause + a one-tap recovery (`Request a new link` → Screen 1). Never a
> dead-end. The raw API message `"Invalid or expired reset token"` is **not** shown verbatim.

---

## Responsive Behaviour

| Breakpoint | Layout |
|------------|--------|
| **Mobile (<640px)** | Single column, card fills width with 16px gutters. Full-width inputs and buttons (≥44px touch targets). Checklist stacked vertically. |
| **Tablet (640–1024px)** | Centred card ~440px max-width on branded background. Same single-column content. |
| **Desktop (>1024px)** | Same centred card (~440px), optionally a brand panel/illustration to one side. Content and hierarchy unchanged — consistency across breakpoints. |

All screens keep **one primary action**, visible focus order top→bottom, and the "Back to sign in"
escape on every view.
