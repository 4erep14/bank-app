# Wireframes — US-003: Two-Factor Authentication via SMS

Components referenced (PascalCase):
`OtpVerificationPage`, `OtpInputGroup`, `OtpDigitBox`, `ResendCodeControl`,
`FormErrorBanner`, `SubmitButton`, `TrustBadge`, `BackToSignInLink`.

Shared from US-002 (unchanged props/behaviour): `FormErrorBanner`, `SubmitButton`, `TrustBadge`.

Legend:
- `[_]` empty OTP digit box · `[4]` filled digit box · `(•)` keyboard focus ring
- `~spinner~` loading indicator · `✓` success · `!` error/warning icon
- `[  Verify  ]` primary button · `░░░░░` disabled/dimmed element

---

## 1. Default State — Mobile (≤ 767 px)

> AC1: User arrives here from US-002 VerificationHandoff. `sessionToken` is in `sessionStorage`.

```
+----------------------------------+
| 🔒 NorthBank                     |   ← page header (no nav links)
+----------------------------------+
|                                  |
|  Verify it's you                 |   ← H1 (PageTitle)
|                                  |
|  We sent a 6-digit code to       |   ← contextual subtitle
|  +*** *** **89                   |   ← masked phone (secondary emphasis)
|  Check your messages.            |
|                                  |
|  +----+----+----+----+----+----+ |   ← OtpInputGroup
|  | _  | _  | _  | _  | _  | _  | |     6 × OtpDigitBox
|  +----+----+----+----+----+----+ |     large touch targets (≥48×56px each)
|                                  |
|  This code expires in 5 minutes. |   ← helper text (tertiary)
|                                  |
|  [          Verify          ]    |   ← SubmitButton (full-width, primary)
|                                  |
|  Didn't get a code?              |   ← ResendCodeControl label
|  Resend in 0:59                  |   ← countdown (disabled state)
|                                  |
|  Back to sign in                 |   ← BackToSignInLink (text link, secondary)
|                                  |
|  🔒 Your connection is encrypted.|   ← TrustBadge
+----------------------------------+
| © 2026 NorthBank · Privacy       |
+----------------------------------+
```

**Layout notes (mobile):**
- Single column; no sidebar.
- OtpInputGroup spans full width with equal-width boxes (auto-sized via CSS flex/grid).
- `[Verify]` button: full-width, min-height 48px, solid primary blue fill.
- ResendCodeControl sits below the button, left-aligned, body text size.
- BackToSignInLink: small secondary link, centred, sufficient spacing below TrustBadge.
- Page is vertically centred in the viewport on short screens (flex column, justify-center).

---

## 2. Default State — Desktop (≥ 768 px, centered card)

```
+--------------------------------------------------------------------------+
| HEADER: 🔒 NorthBank                                                     |
+--------------------------------------------------------------------------+
|                                                                          |
|                 +--------------------------------------------+           |
|                 |  Verify it's you                           |  ← H1    |
|                 |                                            |          |
|                 |  We sent a 6-digit code to                 |  ← sub   |
|                 |  +*** *** **89                             |          |
|                 |  Check your messages.                      |          |
|                 |                                            |          |
|                 |  +----+----+----+----+----+----+           |          |
|                 |  | _  | _  | _  | _  | _  | _  |          |          |
|                 |  +----+----+----+----+----+----+           |          |
|                 |       OtpInputGroup (centred)              |          |
|                 |                                            |          |
|                 |  This code expires in 5 minutes.           |          |
|                 |                                            |          |
|                 |  [            Verify            ]          |  ← 1°   |
|                 |                                            |          |
|                 |  Didn't get a code?  Resend in 0:59        |  ← 2°   |
|                 |                                            |          |
|                 |  Back to sign in                           |  ← 2°   |
|                 |                                            |          |
|                 |  🔒 Your connection is encrypted and secure.|          |
|                 +--------------------------------------------+           |
|                      Card max-width: ~400–440 px                        |
+--------------------------------------------------------------------------+
| FOOTER: © 2026 NorthBank · Privacy · Terms                               |
+--------------------------------------------------------------------------+
```

**Layout notes (desktop):**
- Card is centred, max-width 440 px, with rounded corners and a subtle box-shadow.
- OtpInputGroup is centred within the card; each box is 52×60px with generous visual spacing.
- The "Verify" button spans the full card content width (same as US-002 "Sign in").
- Everything below the button (Resend, Back to sign in, TrustBadge) is left-aligned in the card.

---

## 3. Partially-Filled State (typing in progress)

> User has entered 3 of 6 digits; focus is on box 4.

```
+----------------------------------+    (mobile view)
|  Verify it's you                 |
|                                  |
|  We sent a 6-digit code to       |
|  +*** *** **89                   |
|                                  |
|  +----+----+----+----+----+----+ |
|  | 4  | 8  | 2  |(•) | _  | _  | |   ← box 4 has focus ring (•)
|  +----+----+----+----+----+----+ |     boxes 1-3: filled (neutral border)
|                                  |     boxes 4-6: empty
|  This code expires in 5 minutes. |
|                                  |
|  [          Verify          ]    |   ← enabled (primary blue, not disabled)
|                                  |     Note: pressing Verify on a partial
|  Didn't get a code?              |     fill triggers validation inline
|  Resend in 0:42                  |   ← countdown continues
|                                  |
|  Back to sign in                 |
+----------------------------------+
```

- Filled boxes show the digit in a larger font; border changes to a "filled" colour (e.g. a
  slightly darker neutral, distinct from the empty state).
- Focus ring is a 2 px solid blue outline on box 4 (WCAG visible focus, ≥3:1 on background).
- SubmitButton remains enabled so a user can manually trigger submission at any point.
- Pressing "Verify" before all 6 are filled shows an inline validation message (see State table).

---

## 4. Submitting / Loading State

> Auto-submit or manual "Verify" pressed with all 6 digits filled.

```
+----------------------------------+
|  Verify it's you                 |
|                                  |
|  We sent a 6-digit code to       |
|  +*** *** **89                   |
|                                  |
|  +----+----+----+----+----+----+ |
|  |░4░ |░8░ |░2░ |░9░ |░1░ |░7░ | |   ← all boxes disabled/dimmed
|  +----+----+----+----+----+----+ |
|                                  |
|  This code expires in 5 minutes. |
|                                  |
|  [ ~spinner~  Verifying…    ]    |   ← SubmitButton: spinner + "Verifying…"
|    (disabled, aria-busy="true")  |     same width as "Verify" (no layout shift)
|                                  |
|  ░Didn't get a code?░            |   ← ResendCodeControl: dimmed, not interactive
|  ░Resend in 0:38░                |
|                                  |
|  ░Back to sign in░               |   ← dimmed, not interactive
+----------------------------------+
```

- Entire page non-interactive during the API call.
- `aria-busy="true"` on the OtpInputGroup wrapper.
- Spinner is decorative: `aria-hidden="true"` on the icon; "Verifying…" text conveys status.
- No layout shift: button width remains constant; spinner replaces the icon slot only.

---

## 5. Invalid / Expired OTP Error State (AC4 / AC2) — attempts remaining

> First or second failure: the API returns 401 "Invalid or expired OTP".
> `remainingAttempts` = 2 (first failure) or 1 (second failure — see wireframe 5b).

```
+----------------------------------+
|  Verify it's you                 |
|                                  |
|  We sent a 6-digit code to       |
|  +*** *** **89                   |
|                                  |
|  +----------------------------------+
|  | ! Incorrect or expired code.    |   ← FormErrorBanner (role="alert")
|  |   Check your SMS and try again. |     variant="error" (red bar + ! icon)
|  |   You have 2 attempts remaining.|     "2" is dynamically rendered
|  +----------------------------------+
|                                  |
|  +----+----+----+----+----+----+ |
|  |(•) | _  | _  | _  | _  | _  | |   ← all boxes CLEARED; focus on box 1
|  +----+----+----+----+----+----+ |     red outline on all 6 boxes
|                                  |     (whole group indicates error, not
|                                  |      one specific box)
|  This code expires in 5 minutes. |
|                                  |
|  [          Verify          ]    |   ← re-enabled (primary blue)
|                                  |
|  Didn't get a code?              |
|  Resend in 0:12                  |   ← or "Resend code" if cooldown elapsed
|                                  |
|  Back to sign in                 |
+----------------------------------+
```

- All 6 boxes get the error outline simultaneously (the *whole code* is wrong, not one digit).
- Boxes are **cleared** — user types a fresh 6-digit code; no partial digit from the previous
  wrong attempt is left sitting in a box.
- Focus moves to box 1 immediately on error so the user can start re-entering without a tap.
- `FormErrorBanner` is announced to screen readers via `role="alert"`.
- "You have 2 attempts remaining." is part of the alert text (not a separate ARIA region).

---

## 5b. Near-Lockout Warning State (AC4 / AC5) — 1 attempt remaining

> Second failure: `remainingAttempts` = 1. Warning variant (amber) replaces error variant.

```
+----------------------------------+
|  Verify it's you                 |
|                                  |
|  We sent a 6-digit code to       |
|  +*** *** **89                   |
|                                  |
|  +----------------------------------+
|  | ⚠ Incorrect or expired code.    |   ← FormErrorBanner (role="alert")
|  |   This is your last attempt —   |     variant="warning" (amber bar + ⚠)
|  |   after one more incorrect      |
|  |   entry you will need to sign   |
|  |   in again from the beginning.  |
|  |   Consider requesting a new     |
|  |   code below.                   |
|  +----------------------------------+
|                                  |
|  +----+----+----+----+----+----+ |
|  |(•) | _  | _  | _  | _  | _  | |   ← cleared; focus box 1; amber outlines
|  +----+----+----+----+----+----+ |
|                                  |
|  [          Verify          ]    |
|                                  |
|  Didn't get a code?              |
|  **Resend code**                 |   ← ResendCodeControl: active link (bold,
|  (or "Resend in 0:xx" if active)  |     underlined) — strongly recommended in copy
|                                  |
|  Back to sign in                 |
+----------------------------------+
```

- Amber/warning variant uses a different background and icon from the red error variant — but
  conveys the same severity through icon + text + colour (never colour alone).
- "Resend code" is surfaced with higher visual emphasis (bolder link text) because the copy
  explicitly recommends it as the safer option.
- Box outlines are amber (matching the warning palette), not red.

---

## 6. Session Invalidated (AC5) — transitional state before redirect

> Third consecutive failure. Shown for ~1.5 s before navigating to `/login`.

```
+----------------------------------+
|  Verify it's you                 |
|                                  |
|  +----------------------------------+
|  | ! Session ended.                |   ← FormErrorBanner (role="alert")
|  |   Too many incorrect attempts.  |     variant="error"
|  |   Redirecting to sign in…       |
|  +----------------------------------+
|                                  |
|  +----+----+----+----+----+----+ |
|  |░_░ |░_░ |░_░ |░_░ |░_░ |░_░ | |   ← all boxes disabled/dimmed
|  +----+----+----+----+----+----+ |
|                                  |
|  [░░░░░░  Verify  ░░░░░░░░░░]    |   ← disabled
|                                  |
|  ░Didn't get a code?░            |   ← dimmed
|  ░Resend in …░                   |
|                                  |
|  ░Back to sign in░               |   ← also dimmed (auto-nav imminent)
+----------------------------------+
```

Then `/login` renders with:

```
+----------------------------------+
| 🔒 NorthBank   [ Create account ]|
+----------------------------------+
|  +----------------------------------+
|  | ⚠ Too many attempts.             |   ← Flash banner (role="alert")
|  |   Please sign in again.          |     variant="warning", dismissible
|  +----------------------------------+
|                                  |
|  Sign in to NorthBank            |   ← US-002 login form (clean default state)
|  Enter your details to continue. |
|  ...                             |
+----------------------------------+
```

- The flash message uses the exact wording specified in AC5.
- It is rendered as a dismissible banner at the top of the login form.
- It disappears after first render (cleared from `sessionStorage`), so a page reload shows
  the clean login form with no lingering message.

---

## 7. Success State — brief confirmation before dashboard (AC3)

```
+----------------------------------+
|  Verify it's you                 |
|                                  |
|  +----+----+----+----+----+----+ |
|  | ✓  | ✓  | ✓  | ✓  | ✓  | ✓  | |   ← all boxes show green checkmark overlay
|  +----+----+----+----+----+----+ |     (300ms animation)
|                                  |
|  ✓  Identity verified!           |   ← aria-live="polite" announcement
|     Redirecting to your account… |
|                                  |
|  [  ✓  Verified  ✓  ]            |   ← SubmitButton: green fill variant
|                                  |
+----------------------------------+
```

- This state lasts ~1.5 s (the time for the animation + route navigation).
- If the route transition is fast, this state may only flash briefly — that is acceptable
  and expected. It confirms the action worked before the view changes.
- `aria-live="polite"` announces "Identity verified! Redirecting to your account…" to screen
  reader users without interrupting other announcements.
- The green state uses a distinct success palette, not just "not red".

---

## 8. Resend Code — in-flight state

```
+----------------------------------+
|  Verify it's you                 |
|  We sent a 6-digit code to       |
|  +*** *** **89                   |
|                                  |
|  +----+----+----+----+----+----+ |
|  | _  | _  | _  | _  | _  | _  | |   ← boxes cleared, awaiting new code
|  +----+----+----+----+----+----+ |
|                                  |
|  [          Verify          ]    |
|                                  |
|  Didn't get a code?              |
|  ~spinner~ Sending…              |   ← ResendCodeControl: spinner + "Sending…"
|  (link disabled)                 |
|                                  |
|  Back to sign in                 |
+----------------------------------+
```

## 8b. Resend Code — success confirmation

```
+----------------------------------+
|  Verify it's you                 |
|                                  |
|  ✓ A new code has been sent to   |   ← inline success message (role="status")
|    +*** *** **89                 |     green text + checkmark icon
|                                  |
|  +----+----+----+----+----+----+ |
|  |(•) | _  | _  | _  | _  | _  | |   ← focus on box 1; boxes cleared
|  +----+----+----+----+----+----+ |
|                                  |
|  This code expires in 5 minutes. |
|                                  |
|  [          Verify          ]    |
|                                  |
|  Didn't get a code?              |
|  Resend in 0:59                  |   ← cooldown reset to 60 s
|                                  |
|  Back to sign in                 |
+----------------------------------+
```

---

## 9. Incomplete Submit Error (partial fill — validation)

> User presses "Verify" before all 6 boxes are filled.

```
+----------------------------------+
|  Verify it's you                 |
|                                  |
|  We sent a 6-digit code to       |
|  +*** *** **89                   |
|                                  |
|  +----+----+----+----+----+----+ |
|  | 4  | 8  |(•) | _  | _  | _  | |   ← focus stays on last empty box
|  +----+----+----+----+----+----+ |     boxes 3-6 show a red outline
|                                  |
|  Enter all 6 digits of the code. |   ← inline validation message (below group)
|  (role="alert", id linked to     |     NOT a FormErrorBanner — local group error
|   aria-describedby on group)     |
|                                  |
|  [          Verify          ]    |
|                                  |
|  Didn't get a code?  Resend…     |
|  Back to sign in                 |
+----------------------------------+
```

- This is a client-side validation error, not an API response.
- Focus moves to the first empty box so the user can complete the entry immediately.
- The message does NOT use `FormErrorBanner` (that's for API-level errors) — it uses a smaller
  inline message directly below the `OtpInputGroup`, linked via `aria-describedby`.

---

## Component Map

| Component | Purpose | Key Props | Events | Reusable |
|-----------|---------|-----------|--------|----------|
| `OtpVerificationPage` | Route container for `/verify-otp`; orchestrates all states and guards against missing `sessionToken` | `maskedPhone: string`, `sessionToken: string` | — | No |
| `OtpInputGroup` | Wraps 6 × `OtpDigitBox`; manages focus-advance, backspace, paste, auto-submit | `value: string[6]`, `isDisabled: boolean`, `hasError: boolean`, `errorVariant: 'error'|'warning'`, `onComplete(digits: string): void` | `onComplete`, `onChange` | Yes (US-003 only for now) |
| `OtpDigitBox` | Single digit input cell; numeric-only; controlled | `index: number`, `value: string`, `isFocused: boolean`, `isDisabled: boolean`, `hasError: boolean`, `variant: 'default'|'error'|'warning'|'success'` | `onChange`, `onKeyDown`, `onPaste` | Used only within `OtpInputGroup` |
| `ResendCodeControl` | Countdown timer + active/disabled "Resend code" link | `cooldownSeconds: number`, `onResend(): void`, `isResending: boolean`, `resendSuccess: boolean`, `maskedPhone: string` | `onResend` | Yes (could be reused for email OTP in future) |
| `FormErrorBanner` | Form-level error or warning banner | `message: string`, `variant: 'error'|'warning'` | — | Yes (shared w/ US-002) |
| `SubmitButton` | Primary action button with loading state | `label: string`, `loadingLabel: string`, `isLoading: boolean`, `disabled: boolean`, `variant: 'default'|'success'` | `onClick` | Yes (shared w/ US-002) |
| `TrustBadge` | Encryption reassurance line | `text: string` | — | Yes (shared w/ US-002) |
| `BackToSignInLink` | Text link to `/login`; clears `sessionToken` on click | `onNavigate(): void` | `onNavigate` | No (too context-specific) |

> **Reuse note:** `FormErrorBanner`, `SubmitButton`, and `TrustBadge` are carried over unchanged
> from US-002. `OtpInputGroup` and `ResendCodeControl` are new components specific to 2FA flows
> but designed for reuse if email OTP or TOTP is added in a future epic.
