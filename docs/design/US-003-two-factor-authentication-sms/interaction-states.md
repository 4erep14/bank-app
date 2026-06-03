# Interaction States — US-003: Two-Factor Authentication via SMS

Accessibility baseline (applies to every state):
- All interactive elements are keyboard-navigable and have a **visible focus ring**
  (≥2 px outline, ≥3:1 contrast against adjacent background — WCAG 2.1 SC 1.4.11).
- Text and icon contrast ≥4.5:1 against background (WCAG SC 1.4.3).
- Errors are conveyed by **icon + text + colour** — never colour alone (SC 1.4.1).
- All inputs have `<label>` associations or `aria-label`/`aria-labelledby`.
- Error messages are linked to their owning element via `aria-describedby`.
- `role="alert"` is used for error content that must be announced immediately.
- `role="status"` / `aria-live="polite"` is used for non-urgent status updates.
- The `sessionToken` is **never rendered to the DOM**.

---

## OtpInputGroup (wrapper — 6 digit boxes)

| State | Trigger | Visual Behaviour | Accessibility |
|-------|---------|-----------------|---------------|
| **Default / Empty** | Page load (fresh session) | 6 empty boxes with neutral border; first box has focus automatically (desktop) or on first interaction (mobile — avoids forced keyboard jump) | `role="group"`, `aria-label="6-digit verification code"`, `aria-describedby` → helper text ID. Tab order enters at box 1. |
| **Filling (in-progress)** | User types digits | Filled boxes: digit displayed, border shifts to "active-filled" colour (slight darkening, distinct from error/success). Focus ring on current box. Unfilled boxes remain neutral. | Focus advances automatically to next box via programmatic `focus()` call. Screen reader announces entered digit via `aria-live="polite"` on group. |
| **Complete (all 6 filled)** | 6th digit entered | All boxes show their digit in filled state; auto-submit fires after 300 ms debounce; briefly visible before transitioning to Submitting state. | "Code entered. Verifying…" announced via `aria-live="polite"` on the status region before the disable transition. |
| **Submitting** | Auto-submit or manual "Verify" pressed | All 6 boxes dimmed (CSS `opacity: 0.6`, `pointer-events: none`); `disabled` attribute set. No spinner on boxes themselves (spinner is on SubmitButton). | `aria-disabled="true"` on each `OtpDigitBox`; `aria-busy="true"` on the group wrapper; focus held on SubmitButton. |
| **Error — invalid/expired (AC4/AC2, attempts > 1)** | 401 response | All boxes cleared to empty; all 6 boxes get error outline (2 px red border); focus moved to box 1. `FormErrorBanner` (error) appears above the group. | `aria-invalid="true"` on each `OtpDigitBox`; `aria-describedby` → error banner ID. Focus programmatically moves to box 1. Banner announced via `role="alert"`. |
| **Warning — near lockout (AC4/AC5, 1 attempt remaining)** | 401 response with `remainingAttempts: 1` | All boxes cleared; all 6 boxes get amber/warning outline (2 px amber border); focus on box 1. `FormErrorBanner` (warning) appears above the group. | Same as Error state but `variant="warning"`. Amber border distinguishes from error without relying on colour alone (icon + text in banner). |
| **Success (AC3)** | 200 response | All 6 boxes briefly display a green checkmark overlay (SVG, animated, 300 ms). Box borders turn green. Transitional — visible for ~1.5 s before route changes. | `aria-live="polite"` announces "Identity verified!" before navigation. Boxes set `aria-invalid="false"`. |
| **Re-enabled after network error** | 5xx / timeout | Boxes remain filled (digits preserved — a network error is not a wrong-code error). Borders return to filled-neutral state. Focus returns to last filled or first empty box. | `aria-busy="false"` on group. `FormErrorBanner` announced. |

---

## OtpDigitBox (individual digit input — × 6)

| State | Trigger | Visual Behaviour | Accessibility |
|-------|---------|-----------------|---------------|
| **Default / Empty** | Box rendered | White/light-grey background; neutral medium border (e.g. #9CA3AF); centred placeholder dot or thin cursor bar on focus. Min size: 48 × 56 px (mobile), 52 × 60 px (desktop). | `type="text"`, `inputmode="numeric"`, `pattern="[0-9]"`, `maxlength="1"`, `autocomplete="one-time-code"` (on box 1 only; others `autocomplete="off"`), `aria-label="Digit 1 of 6"` (through "Digit 6 of 6"). |
| **Focus** | Keyboard focus or programmatic `focus()` | 2 px solid primary-blue focus ring (distinct from default border colour). Box may subtly scale (1.02×) to indicate active slot. | Focus ring must be visible, ≥2 px, with sufficient contrast. Box is within a `<fieldset>` or `role="group"` for logical grouping. |
| **Filled (valid digit)** | User types a numeric character | Digit displayed in a larger font (e.g. 1.5rem, bold); border shifts to "filled-active" state (e.g. slightly darker neutral). Focus immediately advances to next box. | Digit echoed visually; `aria-label` is updated (e.g. "Digit 1 of 6: 4") or announced via a live region. |
| **Filled — last box** | User types digit in box 6 | Same as Filled, but focus stays momentarily before auto-submit fires. | "Code entered" announced before disable. |
| **Backspace / Delete on empty** | User presses Backspace on an empty box | Focus moves to the previous box and clears it; "erased" box returns to empty state. | Standard keyboard editing semantics; screen reader reads the newly focused empty box label. |
| **Error** | 401 response (AC2/AC4) | Red/error border (2 px); digit cleared; box returns to empty state with red outline. | `aria-invalid="true"`. |
| **Warning** | Near-lockout state | Amber/warning border; digit cleared. | `aria-invalid="true"` (still invalid input context). |
| **Success** | 200 response | Green border; checkmark icon overlaid briefly. | `aria-invalid="false"`. Success state communicated via group-level `aria-live`. |
| **Disabled** | Submitting or session invalidated | Dimmed (`opacity: 0.6`); `cursor: not-allowed`; `pointer-events: none`. | `disabled` attribute; `aria-disabled="true"`. |
| **Paste** | User pastes into any box | Up to 6 numeric characters extracted from the paste data (strip non-numeric, strip whitespace/dashes). Digits distributed across boxes 1–6 starting from box 1 (regardless of which box received the paste event). Auto-submit fires if all 6 are filled. Excess characters beyond 6 are silently discarded. | Paste handled via `onPaste` event on the group wrapper. Screen reader: "Code entered" announced via live region. |

---

## ResendCodeControl

| State | Trigger | Visual Behaviour | Accessibility |
|-------|---------|-----------------|---------------|
| **Cooldown — initial (0:59 → 0:01)** | Page load / after each resend | Label text: "Didn't get a code? Resend in 0:XX" — the countdown updates every second. The "Resend in 0:XX" portion is plain text (not a link/button — cannot be activated). Grey/muted colour. | Countdown is NOT a live region — announcing every second would be disruptive. Announced only at 0:00 transition. Aria-live region fires once: "Resend code is now available." |
| **Resend available (cooldown at 0:00)** | 60-second timer elapses | Label: "Didn't get a code?" + "Resend code" becomes an underlined, primary-colour link/button. Cursor: pointer. Hover: underline + slight colour shift. | `role="button"` or `<button>` element (not `<a>` — no href navigation). `aria-label="Resend verification code"`. `aria-live="polite"` region fires: "Resend code is now available." |
| **Resend focus** | Keyboard focus | Visible focus ring on "Resend code" link. | Standard ≥2px ring. Tab sequence: SubmitButton → ResendCodeControl → BackToSignInLink. |
| **Resending (in-flight)** | User activates "Resend code" | "Resend code" text replaced by `~spinner~ Sending…`; link/button disabled. | `aria-disabled="true"` on control; spinner `aria-hidden="true"`; "Sending…" text conveys status. |
| **Resend success** | 200 from resend API | Inline message replaces "Didn't get a code?" row: "✓ A new code has been sent to +*** *** **89." Green text + checkmark icon. Cooldown resets to 0:59. OtpInputGroup boxes cleared; focus moves to box 1. | `role="status"` / `aria-live="polite"` on confirmation message. Checkmark icon `aria-hidden="true"`. |
| **Resend error** | 5xx / network failure on resend | Inline error below control: "We couldn't send a new code. Please try again." Red text + ! icon. "Resend code" link re-enabled (no cooldown penalty). | `role="alert"` on error message. |
| **Disabled (during OTP submission)** | OtpInputGroup submitting | Entire ResendCodeControl dimmed and non-interactive (`pointer-events: none`). | `aria-disabled="true"`. |

---

## SubmitButton — "Verify"

| State | Trigger | Visual Behaviour | Accessibility |
|-------|---------|-----------------|---------------|
| **Default (enabled)** | Page interactive | Solid primary blue fill; label "Verify"; full-width on mobile (≥48px tall); standard width on desktop (matches card width). | `type="submit"` or `type="button"` with `onClick`; `aria-label="Verify code"` (or text is self-descriptive). Min touch target 48×48px. |
| **Hover (desktop)** | Pointer over | Fill darkens ~8–10% (e.g. #1D4ED8 → #1E40AF); `cursor: pointer`. | — |
| **Focus** | Keyboard focus | Visible focus ring, distinct from hover fill change; 2 px outline offset. | ≥2 px, ≥3:1 contrast on background. |
| **Loading / Disabled (Submitting)** | Auto-submit fires or user clicks | Spinner icon + label "Verifying…"; same button width; `disabled` attribute; fill may lighten slightly to communicate non-interactivity. | `aria-busy="true"`, `disabled`. Spinner `aria-hidden`; "Verifying…" text is the SR announcement. |
| **Success** | 200 response | Fill changes to success green; label "Verified ✓"; brief (300 ms) before route change. Spinner stops. | `aria-live="polite"` on adjacent status region: "Identity verified! Redirecting to your account." Button `aria-disabled="true"` (no further interaction possible). |
| **Disabled — incomplete input** | OtpInputGroup has < 6 digits and user presses button | Button remains visually enabled (NOT permanently disabled). Pressing it triggers the inline validation message ("Enter all 6 digits of the code.") beneath the `OtpInputGroup`. Focus moves to first empty box. | Per US-002 pattern: button never permanently disabled (avoids "why won't it work" confusion). Validation announced via `role="alert"` on the inline error below the group. |

---

## FormErrorBanner

Shared component from US-002. Applied without modification; documented here for completeness.

| State | Visual Behaviour | Accessibility |
|-------|-----------------|---------------|
| **Hidden** | Not rendered (not in DOM or `display: none`). | — |
| **Error (variant="error")** | Red background bar (`#FEF2F2`) + red left border (`#EF4444`) + `!` icon + message text. | `role="alert"`; `id` referenced by `aria-describedby` on `OtpInputGroup`. Announced immediately on render. |
| **Warning (variant="warning")** | Amber background (`#FFFBEB`) + amber border (`#F59E0B`) + `⚠` icon + message text. | `role="alert"`; same wiring. Amber communicates elevated caution, not immediate failure. |
| **Success / Info (variant="success")** | Green background (`#F0FDF4`) + green border (`#22C55E`) + `✓` icon. Used for "New code sent" confirmation inline (below `ResendCodeControl`, not as a full banner). | `role="status"` / `aria-live="polite"` (non-urgent). |
| **Dismissible** | On the `/login` flash banner (AC5), an `×` close button is shown (rightmost). Other banners on this page are non-dismissible (they clear when the user takes action). | Close button `aria-label="Dismiss message"`; removes the element from DOM on click; focus returns to next logical element. |

---

## OtpVerificationPage (whole page)

| State | Trigger | Visual Behaviour | Accessibility |
|-------|---------|-----------------|---------------|
| **Loading / mounting** | Route navigated to from US-002 | Page renders fully (no skeleton needed — layout is simple and static content is known). First `OtpDigitBox` receives auto-focus on desktop. | Page `<title>` = "Verify your identity – NorthBank"; `<h1>` = "Verify it's you". Focus management: on mount, focus on OtpInputGroup (box 1) on desktop; on mobile, no forced auto-focus (avoids keyboard jump). |
| **Default (interactive)** | Mounted, no submission | All inputs enabled; SubmitButton enabled; ResendCodeControl in cooldown. | Logical tab order: OtpDigitBox 1 → 2 → 3 → 4 → 5 → 6 → SubmitButton → ResendCodeControl (when active) → BackToSignInLink. |
| **Submitting** | Auto-submit or manual submit | All interactive elements non-interactive; spinner on SubmitButton. | `aria-busy="true"` on page main landmark or form element; `aria-live` region announces "Verifying…". |
| **Error / warning** | 401 response | `FormErrorBanner` inserted above `OtpInputGroup`; boxes cleared + outlined; focus on box 1. | Banner `role="alert"` announced immediately. Focus moved to box 1 (not the banner — the user's next action is re-entry, not reading the error in detail; the error is read by the screen reader via the alert role). |
| **Success** | 200 response | Success animation on OtpInputGroup + SubmitButton. Route changes to `/dashboard` after ~1.5 s. | `aria-live="polite"` status region announces "Identity verified! Redirecting to your account." Route navigation updates page `<title>` and `<h1>` for the new page. |
| **Session invalidated (AC5)** | 3rd 401 | Brief error banner shown; all inputs disabled; immediate redirect to `/login`. | `role="alert"` on the banner before redirect. Flash message set in `sessionStorage`, read by `/login` on mount. |
| **No session guard** | Direct URL access without `sessionToken` | Immediate redirect to `/login`; no error shown. | Route guard fires synchronously before render; no flash of content. |

---

## BackToSignInLink

| State | Trigger | Visual Behaviour | Accessibility |
|-------|---------|-----------------|---------------|
| **Default** | Page interactive | Small secondary text link: "Back to sign in". Muted colour (lower hierarchy than primary + resend controls). Underlined on hover. | `role="button"` or `<button>` styled as link; clears `sessionToken` before navigating — must NOT be a plain `<a href>` that bypasses the cleanup logic. `aria-label="Back to sign in page"`. |
| **Focus** | Keyboard focus | Visible focus ring. | ≥2 px ring. |
| **Disabled** | During OTP submission | Dimmed; `pointer-events: none`. | `aria-disabled="true"`. |

---

## Responsive Behaviour Summary

| Breakpoint | Layout Changes |
|------------|----------------|
| **Mobile (< 768 px)** | Single column, full viewport width. No card border/shadow. OtpInputGroup full-width, boxes auto-sized with equal flex shares. SubmitButton full-width (≥48px tall). No auto-focus on box 1 (avoids keyboard jump on load). Touch targets: all interactive elements ≥44×44px, OtpDigitBox ≥48×56px. |
| **Tablet / Desktop (≥ 768 px)** | Content centred in a card (max-width 440px, border-radius 12px, box-shadow). OtpDigitBox 52×60px. Auto-focus on box 1 on desktop (keyboard-first users land here). SubmitButton spans card content width (same as US-002 "Sign in"). |

---

## Form Validation Rules

| Scenario | Rule | Client-side or Server |
|----------|------|-----------------------|
| Digit boxes — character type | Accept only `0–9`; reject letters, symbols, spaces silently on key press | Client-side (keydown event filter) |
| Paste sanitisation | Strip non-numeric characters; trim to 6 characters; fill from box 1 | Client-side (paste event handler on group) |
| Incomplete submission | All 6 boxes must be filled before API call; if button pressed early, show inline message "Enter all 6 digits of the code." and focus first empty box | Client-side (before submit) |
| OTP validity (correct digits, not expired) | Validated by API; 401 on failure | Server (AC2, AC4) |
| Session validity (`sessionToken`) | API validates; 401/403 if session expired or invalid | Server |
| Maximum attempts (3 consecutive) | API invalidates session; UI redirects on count (from `remainingAttempts` in response or from client counter) | Server (AC5); UI reflects |
| SMS autofill (OS suggestion) | `autocomplete="one-time-code"` on box 1; browser/OS suggests code from received SMS; accepted as paste | Browser/OS + client paste handler |
