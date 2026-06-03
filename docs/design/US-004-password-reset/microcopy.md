# Microcopy — US-004: Password Reset

Tone: clear, professional, reassuring (banking context), never blaming. Errors always say *how to
fix*. Anti-enumeration copy (AC1) never confirms or denies an email's existence. No system terms
("token", "JWT", "400", "expiry timestamp") are surfaced to the user.
Brand placeholder: **NorthBank**.

---

## Entry point (US-002 Login screen)

| Element | Text | Notes / Tone |
|---------|------|--------------|
| Login link | **Forgot password?** | Plain, matches universal pattern; routes to `/forgot-password` |

---

## Screen 1 — ForgotPasswordForm (`/forgot-password`) — AC1, AC2

| Element | Text | Notes / Tone |
|---------|------|--------------|
| Page title (H1) | **Forgot your password?** | Plain language, matches mental model |
| Subtitle | **Enter the email address linked to your account and we'll send you a link to reset your password.** | Sets expectation; single clear task |
| Field label | **Email address** | — |
| Email placeholder | `name@example.com` | Supplementary only — label stays visible |
| Email helper | **We'll email a reset link if this address is registered.** | Subtle anti-enumeration framing ("if") |
| Primary button (default) | **Send reset link** | Verb + noun, action-oriented |
| Primary button (loading) | **Sending…** *(spinner; `aria-busy`)* | Visible status |
| Secondary link | **Back to sign in** | Escape hatch; routes to `/login` |
| Trust badge | **🔒 We never share your email address.** | Reassurance |

### Screen 1 — inline errors
| Case | Message |
|------|---------|
| Email blank | **Enter your email address.** |
| Invalid format | **Enter a valid email address, like name@example.com.** |

### Screen 1 — form-level errors
| Case | Message |
|------|---------|
| Server / network (5xx, timeout) | **Something went wrong on our end. Please try again.** |

---

## Screen 2 — ForgotPasswordConfirmation (`/forgot-password/sent`) — AC1

> **Critical:** copy is identical whether or not the email exists. Never echo the address.

| Element | Text | Notes / Tone |
|---------|------|--------------|
| Heading (H1) | **Check your email** | Neutral, non-committal on existence |
| Body line 1 | **If that email is registered, we've sent a link to reset your password.** | Anti-enumeration core message (AC1) |
| Body line 2 | **The link expires in 1 hour. Don't see it? Check your spam folder.** | Sets expiry expectation (AC2) + self-serve tip |
| Helper / trust line | **To protect your account, we don't reveal whether an email is registered.** | Explains the non-confirmation → trust signal |
| Secondary button (default) | **Resend email** | Re-triggers request; still neutral |
| Secondary button (loading) | **Sending…** | — |
| Resent confirmation (inline) | **Sent again — check your inbox.** | `aria-live="polite"`; still no enumeration |
| Tertiary link | **Use a different email** | Routes back to Screen 1, field cleared |
| Tertiary link | **Back to sign in** | Routes to `/login` |

---

## Screen 3 — ResetPasswordForm (`/reset-password?token=…`) — AC3, AC4

| Element | Text | Notes / Tone |
|---------|------|--------------|
| Page title (H1) | **Set a new password** | — |
| Subtitle | **Choose a new password for your NorthBank account.** | — |
| New password label | **New password** | — |
| New password helper | **Your password must contain:** *(followed by the live checklist)* | Same pattern as US-001 |
| Confirm label | **Confirm new password** | Typo guard |
| Show/hide toggle | **Show password** / **Hide password** | `aria-pressed` toggle |
| Primary button (default) | **Save new password** | Verb + noun |
| Primary button (loading) | **Saving…** *(spinner; `aria-busy`)* | Visible status |
| Secondary link | **Back to sign in** | Escape hatch |
| Trust badge | **🔒 Your password is encrypted.** | Reassurance |

### Password live checklist items (PasswordChecklist — reused from US-001)
- **At least 8 characters**
- **An uppercase letter (A–Z)**
- **A lowercase letter (a–z)**
- **A number (0–9)**
- **A special character (! ? @ # etc.)**

### Screen 3 — New password inline errors (AC3)
| Case | Message |
|------|---------|
| Blank | **Enter a new password.** |
| Too short (< 8) | **Password must be at least 8 characters.** |
| Missing uppercase | **Add at least one uppercase letter (A–Z).** |
| Missing lowercase | **Add at least one lowercase letter (a–z).** |
| Missing number | **Add at least one number (0–9).** |
| Missing special char | **Add at least one special character (! ? @ # etc.).** |
| Multiple rules missing | **Password must include {missing rules joined naturally}.** e.g. "Password must include an uppercase letter and a special character." |

> Dynamic multi-rule message assembled from the unmet checklist items (recognition over recall).
> Wording is **identical to US-001** so a returning user sees a consistent rulebook.

### Screen 3 — Confirm password inline errors
| Case | Message |
|------|---------|
| Blank | **Re-enter your new password to confirm.** |
| Mismatch | **Passwords don't match. Re-enter to confirm.** |

### Screen 3 — form-level errors
| Case | Message |
|------|---------|
| Server / network (5xx, timeout) | **Something went wrong on our end. Please try again.** |

---

## Screen 4 — ResetPasswordSuccess (`/reset-password/success`) — AC4, AC5

| Element | Text | Notes / Tone |
|---------|------|--------------|
| Heading (H1) | **Your password has been reset** | Confirms AC4 in human terms |
| Body line 1 | **You can now sign in with your new password.** | Forward guidance |
| Body line 2 (AC5) | **For your security, we've signed you out of all devices.** | Explains session invalidation up front as protection |
| Primary CTA | **Continue to sign in** | Routes to `/login` (email prefilled if available) |

---

## Screen 5 — ResetTokenError (invalid / expired / used token) — AC6, AC7

> Serves expired (AC6), already-used single-use (AC7), and malformed/invalid tokens with one
> human-readable screen. The raw API message `"Invalid or expired reset token"` is **mapped**, not shown.

| Element | Text | Notes / Tone |
|---------|------|--------------|
| Heading (H1) | **This reset link has expired** | Most common cause; covers all token failures |
| Body line 1 | **Password reset links are single-use and expire 1 hour after they're sent. This one is no longer valid.** | Explains AC6 + AC7 in plain language |
| Body line 2 | **No problem — request a new one and we'll email you a fresh link.** | Reassuring; never blames the user |
| Primary CTA | **Request a new link** | Routes back to Screen 1 — one-tap recovery |
| Secondary link | **Back to sign in** | Routes to `/login` |
| Tertiary link | **Contact support** | Fallback for repeat failures (`/support`) |

---

## Accessibility / writing rules applied
- Every error states the corrective action ("Enter…", "Add…", "Re-enter…") — never bare "Invalid input".
- No system/DB terms surfaced (no "token", "JWT", "401/400", "expiry timestamp", no raw exception text).
- **Anti-enumeration (AC1):** the confirmation copy never confirms or denies an email's existence,
  and never echoes the entered address.
- Consistent verb-first, sentence-case style across labels, errors, and buttons.
- Examples (`name@example.com`, `! ? @ #`) given wherever format matters.
- Password rule wording is **identical to US-001** for cross-flow consistency.
