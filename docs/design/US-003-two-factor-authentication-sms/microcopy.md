# Microcopy — US-003: Two-Factor Authentication via SMS

> Tone: clear, professional, calm, security-aware without being intimidating.
> Errors say *what happened* and *what to do next* — never just "Error" or "Invalid input".
> Language: user's vocabulary ("code", "text message", "phone") not system vocabulary ("OTP",
> "token", "sessionToken", "401", "2FA").
> Brand placeholder: **NorthBank**. Masked phone placeholder: **+\*\*\* \*\*\* \*\*89**.

---

## Page Shell

| Element | Text | Notes / Tone |
|---------|------|--------------|
| Browser `<title>` | **Verify your identity – NorthBank** | Standard format: page purpose – brand |
| Page H1 | **Verify it's you** | Concise, human, security-framing without jargon |
| Page subtitle line 1 | **We sent a 6-digit code to** | Explains the action the system already took |
| Page subtitle line 2 | **+\*\*\* \*\*\* \*\*89** | Masked number — rendered from API value; visually distinct (slightly larger, medium weight) |
| Page subtitle line 3 | **Check your messages.** | Gentle instruction; "messages" covers SMS inbox on all platforms |
| Header (logo only) | **NorthBank** (logo mark + wordmark) | No nav links — focused authentication mode |

---

## OtpInputGroup — Labels and Helper Text

| Element | Text | Notes / Tone |
|---------|------|--------------|
| Group accessible label (`aria-label`) | **6-digit verification code** | Read by screen readers as the group name when focus enters |
| Individual box `aria-label` (box 1) | **Digit 1 of 6** | Gives position context; repeated pattern: "Digit 2 of 6" through "Digit 6 of 6" |
| Helper text below group | **This code expires in 5 minutes.** | Sets expectation without inducing anxiety. Static — not a live countdown. |
| Incomplete entry validation (below group) | **Enter all 6 digits of the code.** | Shown if "Verify" pressed before all boxes filled. Short, specific, tells exactly what is needed. |

---

## SubmitButton

| State | Label | Notes |
|-------|-------|-------|
| Default / enabled | **Verify** | Short verb; conveys the single action on this page |
| Loading | **Verifying…** *(with spinner; `aria-busy`)* | Continuous tense confirms the action is in progress |
| Success | **Verified ✓** *(brief, before route change)* | Past tense confirms completion; checkmark icon reinforces |

---

## ResendCodeControl

| Element / State | Text | Notes / Tone |
|----------------|------|--------------|
| Label prefix (always visible) | **Didn't get a code?** | Friendly, non-blaming; validates the user's potential experience |
| Cooldown — countdown text | **Resend in 0:59** (counts down to **Resend in 0:01**) | Plain countdown; not a link; muted colour |
| Cooldown — accessible announcement (fires once at 0:00) | *(via `aria-live`)* **Resend code is now available.** | Screen reader only; fires once when the link activates |
| Active link label | **Resend code** | Clear verb; underlined link style |
| Active link `aria-label` | **Resend verification code** | Expanded for screen readers; avoids ambiguity |
| Resending — in-flight | **Sending…** *(with spinner)* | Continuous tense; spinner `aria-hidden`; text is the SR cue |
| Resend success message | **✓ A new code has been sent to +\*\*\* \*\*\* \*\*89.** | Confirms exactly what happened and where. Green text + checkmark. Checkmark `aria-hidden`. |
| Resend error message | **We couldn't send a new code. Please try again.** | Honest about the failure; actionable ("try again"). Keeps the resend link active. |

---

## FormErrorBanner — All Variants

### Error variant (red) — AC4: invalid OTP, ≥2 attempts remaining

| Element | Text | Notes |
|---------|------|-------|
| Banner message (with `remainingAttempts` from API) | **Incorrect or expired code. Check your SMS and try again. You have {n} attempts remaining.** | "Incorrect or expired" covers both AC4 (invalid) and AC2 (expired) — the API returns the same 401. "{n}" is dynamic (2 → 1). |
| Banner message (fallback: no `remainingAttempts` in response) | **Incorrect or expired code. Check your SMS and try again.** | Used if API does not return remaining count. No mention of attempts to avoid ambiguity. |

### Warning variant (amber) — AC4/AC5: near-lockout (1 attempt remaining)

| Element | Text | Notes |
|---------|------|-------|
| Banner message | **Incorrect or expired code. This is your last attempt — after one more incorrect entry you will need to sign in again from the beginning. Consider requesting a new code below.** | Amber/warning colour. Full sentence structure for clarity. Directs user to resend link as safer option. Firm but not accusatory. |

### Session-ending state — transitional before redirect (AC5)

| Element | Text | Notes |
|---------|------|-------|
| Banner message (on `/verify-otp` — shown briefly before redirect) | **Session ended. Too many incorrect attempts. Redirecting to sign in…** | Brief, factual. Error variant (red). User sees it for ~1.5 s. |

### Flash banner on `/login` after AC5 redirect

| Element | Text | Notes |
|---------|------|-------|
| Flash banner message | **Too many attempts. Please sign in again.** | **Exact wording required by AC5.** Warning variant (amber) on the login page. Dismissible. Cleared after first render. |

### Network / server error (non-AC resilience)

| Element | Text | Notes |
|---------|------|-------|
| Banner message | **We couldn't verify your code right now. Please try again.** | Error variant (red). Honest about the transient failure; does not blame the user or the code. Attempt counter NOT incremented. |

---

## Success State

| Element | Text | Notes |
|---------|------|-------|
| Live region announcement (`aria-live="polite"`) | **Identity verified! Redirecting to your account…** | Announced to screen readers; sighted users see the animation. |
| OtpInputGroup success overlay | *(no text — green checkmark icon on each box)* | Visual only; SR announcement above covers it. |
| SubmitButton label (briefly) | **Verified ✓** | Confirms the button action succeeded before the page changes. |

---

## BackToSignInLink

| Element | Text | Notes |
|---------|------|-------|
| Link text | **Back to sign in** | Plain, clear. Lowercase after initial cap — consistent with US-002 secondary link style. |
| `aria-label` | **Go back to the sign in page** | Expanded for screen readers to clarify destination. |
| Confirmation (no dialog needed) | *(none)* | "Back to sign in" is a deliberate abandonment action, not destructive (no data is lost beyond the current session, which the user is actively walking away from). No confirmation dialog needed — it would add friction. |

---

## TrustBadge

| Element | Text | Notes |
|---------|------|-------|
| Trust text | **🔒 Your connection is encrypted and secure.** | Shared from US-002; unchanged. Provides reassurance during a security checkpoint. Lock icon `aria-hidden`. |

---

## ARIA / Screen Reader Announcements Summary

| Event | Announcement method | Text announced |
|-------|--------------------|-|
| Page loads | Static `<h1>` | "Verify it's you" |
| Digit entered (each box) | `aria-live="polite"` on group | "Digit {n} of 6 entered" *(implementation detail; confirm with Frontend Dev)* |
| Code entered (all 6 filled) | `aria-live="polite"` | "Code entered. Verifying…" |
| API error (AC4) | `role="alert"` on FormErrorBanner | Full error text including remaining attempts |
| Near-lockout warning (AC4/AC5) | `role="alert"` on FormErrorBanner | Full warning text |
| Session ended (AC5) | `role="alert"` | "Session ended. Too many incorrect attempts. Redirecting to sign in…" |
| Resend available | `aria-live="polite"` | "Resend code is now available." |
| Resend success | `role="status"` / `aria-live="polite"` | "A new code has been sent to +*** *** **89." |
| Resend error | `role="alert"` | "We couldn't send a new code. Please try again." |
| Success | `aria-live="polite"` | "Identity verified! Redirecting to your account." |
| Incomplete submission | `role="alert"` (inline, below group) | "Enter all 6 digits of the code." |

---

## Copy Rules Applied

1. **Never expose system internals:** No "OTP", "token", "sessionToken", "401", "session
   invalidated", "2FA", "two-factor". Users receive "code", "verification code", "sign in",
   "session ended" (human-readable equivalent).

2. **Every error says how to fix it:**
   - Invalid/expired: "Check your SMS and try again" + "request a new code" option.
   - Near-lockout: "Consider requesting a new code below" (points to safer route).
   - Network error: "Please try again" (single, clear action).
   - Session ended: "Please sign in again" (clear next step on /login).

3. **The AC5 message is verbatim:** "Too many attempts. Please sign in again." — the flash banner
   on `/login` uses this exact wording as specified. It is the one piece of copy that is
   fixed by requirement.

4. **Counting language:**
   - "{n} attempts remaining" — positive framing (what's left), not "you have failed {n} times".
   - "This is your last attempt" — concrete, fair warning.
   - Never: "You have tried {n} times" (backwards, punitive framing).

5. **Masked phone:**
   - Rendered exactly as returned by the API (e.g. "+*** *** **89").
   - The UI does NOT attempt to format or re-mask this value; the API is the source of truth.
   - If the API returns `null` or no phone number, fall back to: **"the phone number registered
     to your account"** (avoids a broken masked-number display).

6. **Resend confirmation is specific:** "A new code has been sent to +*** *** **89" — not just
   "Code sent". The masked number confirms delivery target (especially useful if the user has
   multiple devices or is unsure which number is registered).

7. **Tone consistency with US-002:** calm, professional, plain English, verb-first buttons,
   sentence case everywhere. No exclamation marks on errors. "Identity verified!" is the
   single positive exclamation — it earns the emphasis because it is the success moment.
