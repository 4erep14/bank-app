# Interaction States — US-001: Customer Self-Registration

Accessibility baseline for every state: keyboard-navigable, visible focus ring (≥2px, ≥3:1
contrast against background), text/icon contrast ≥4.5:1, errors conveyed by **icon + text +
color** (never color alone), inline errors wired via `aria-describedby` and announced with
`role="alert"`. Invalid fields set `aria-invalid="true"`.

---

## RegistrationForm (whole form)

| State | Trigger | Visual / Behavior | A11y |
|-------|---------|-------------------|------|
| Default / Empty (first-run) | Page load | All fields empty, labels visible, password & phone helpers shown, primary button enabled. No errors. | Logical tab order top→bottom; first field NOT auto-focused on mobile (avoids keyboard jump), focusable. |
| Validating (in-field) | User blurs a field | That field flips to Valid or Error; rest unchanged. | Live region announces field error if present. |
| Submitting | Valid submit | All inputs + links disabled/dimmed; button shows spinner + "Creating account…"; no double submit. | `aria-busy="true"` on form; focus retained on button. |
| Error-returned (server) | 400 / 409 response | Form re-enabled, data preserved; offending fields show inline errors; summary banner appears; focus to first errored field. | Banner `role="alert"`; each error `role="alert"`. |
| Network/5xx error | request fails | Form re-enabled, data preserved; top banner "Something went wrong… Please try again."; button back to default. | Banner `role="alert"`. |
| Success | 201 response (AC6) | Navigate to `RegistrationSuccess` screen. | Move focus to success H1; announce confirmation. |

---

## TextField — First Name

| State | Visual | A11y |
|-------|--------|------|
| Default | Neutral border, label above, empty input | `<label for>` linked; `autocomplete="given-name"` |
| Focus | Visible focus ring | — |
| Filled/Valid | Neutral border + subtle ✓ at right when non-empty & passes | `aria-invalid="false"` |
| Error | Red border + `!` icon + message "Enter your first name." below | `aria-invalid="true"`, `aria-describedby` → error id, `role="alert"` |
| Disabled | Dimmed, non-interactive (during submit) | `disabled` |

## TextField — Last Name

| State | Visual | A11y |
|-------|--------|------|
| Default | Neutral border, label above | `autocomplete="family-name"` |
| Focus | Visible focus ring | — |
| Filled/Valid | Neutral border + ✓ | `aria-invalid="false"` |
| Error | Red border + `!` + "Enter your last name." | `aria-invalid="true"`, `role="alert"` |
| Disabled | Dimmed, non-interactive | `disabled` |

## TextField — Email

| State | Visual | A11y |
|-------|--------|------|
| Default | Neutral border, label above | `type="email"`, `autocomplete="email"`, `inputmode="email"` |
| Focus | Visible focus ring | — |
| Filled/Valid | Neutral border + ✓ when format valid | `aria-invalid="false"` |
| Error — blank | Red border + `!` + "Enter your email address." | `role="alert"` |
| Error — invalid format | Red border + `!` + "Enter a valid email address, like name@example.com." | `role="alert"` |
| Error — already registered (AC2/409) | Red border + `!` + "This email is already registered. Try signing in instead." + inline "Sign in instead" link | `role="alert"`; recovery link keyboard-focusable |
| Disabled | Dimmed, non-interactive | `disabled` |

## PhoneField — Mobile Number

| State | Visual | A11y |
|-------|--------|------|
| Default | Neutral border; helper "Use international format, e.g. +447911123456" | `type="tel"`, `autocomplete="tel"`, `inputmode="tel"`, helper via `aria-describedby` |
| Focus | Visible focus ring; helper remains | — |
| Filled/Valid | Neutral border + ✓ when E.164 valid | `aria-invalid="false"` |
| Error — blank | Red border + `!` + "Enter your mobile number." | `role="alert"` |
| Error — invalid E.164 (AC4/400) | Red border + `!` + "Enter your number in international format, starting with + (e.g. +447911123456)." | `role="alert"` |
| Disabled | Dimmed, non-interactive | `disabled` |

## DateField — Date of Birth

| State | Visual | A11y |
|-------|--------|------|
| Default | Native date input, placeholder dd/mm/yyyy, calendar affordance 📅 | `type="date"`, `autocomplete="bday"`, `min`/`max` set (no future dates) |
| Focus | Visible focus ring | — |
| Filled/Valid | Neutral border + ✓ | `aria-invalid="false"` |
| Error — blank | Red border + `!` + "Enter your date of birth." | `role="alert"` |
| Error — out of range (future / implausible) | Red border + `!` + "Enter a valid date of birth." | `role="alert"` |
| Disabled | Dimmed, non-interactive | `disabled` |

## PasswordField — Password

| State | Visual | A11y |
|-------|--------|------|
| Default | Neutral border; 👁 show/hide toggle; `PasswordChecklist` shown below with all rules neutral (○) | `type="password"`, `autocomplete="new-password"`, checklist via `aria-describedby` |
| Focus | Visible focus ring; checklist updates live as user types | Checklist updates announced politely (`aria-live="polite"`) |
| Typing/Progress | Each met rule flips ○ → ✓ (green); unmet stays ✗ (red) — icon + text, not color only | — |
| Filled/Valid | All 5 rules ✓; border neutral + field ✓ | `aria-invalid="false"` |
| Error — blank | Red border + `!` + "Enter a password." | `role="alert"` |
| Error — too short (<8) | Red border + `!` + "Password must be at least 8 characters." | `role="alert"` |
| Error — missing complexity (AC3/400) | Red border + `!` + dynamic message naming missing rule(s); checklist shows ✗ on failing rules | `role="alert"` |
| Show/Hide toggle | 👁 toggles plaintext/masked | Toggle is a `button` with `aria-pressed` + label "Show password"/"Hide password" |
| Disabled | Dimmed, non-interactive; toggle disabled | `disabled` |

## SubmitButton — "Create account"

| State | Trigger | Visual | A11y |
|-------|---------|--------|------|
| Default | Form interactive | Solid primary fill, label "Create account", full-width on mobile | Focusable, ≥44px target |
| Hover | Pointer over (desktop) | Darken fill ~8–10%; cursor pointer | — |
| Focus | Keyboard focus | Visible focus ring distinct from hover | — |
| Loading/Disabled | Submit in flight | Spinner + "Creating account…"; same width; not clickable | `aria-busy="true"`, `disabled` |
| Disabled (optional pre-validation) | If form blocks submit | Reduced opacity; not clickable | `disabled` with reason available |
| Success | 201 received | Brief checkmark possible before route change to success screen | Focus moves to success H1 |

> Note: button is **not** permanently disabled while fields are invalid — the user may press it to trigger validation and receive guidance (avoids the "why won't this button work?" trap). It only becomes disabled during the submitting/loading state.

## FormErrorBanner

| State | Visual | A11y |
|-------|--------|------|
| Hidden | Not rendered | — |
| Field-summary | Amber/red bar + `!` icon: "Please fix the highlighted fields below." | `role="alert"` |
| Server/network | Red bar + `!` icon: "Something went wrong on our end. Please try again." | `role="alert"` |

## TrustBadge

| State | Visual | A11y |
|-------|--------|------|
| Default | 🔒 + "Your details are encrypted and secure." | Decorative icon `aria-hidden`; text readable |
