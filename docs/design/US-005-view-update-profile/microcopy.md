# Microcopy — US-005: View & Update Customer Profile

> Words are UI. Every string is specified here — nothing is left for the developer to invent.
>
> Tone: **professional, calm, direct** — consistent with NorthBank's financial brand.
> Never blame the user. Errors explain *how to fix*, not just *what went wrong*.
> Avoid technical jargon (no "E.164", "PATCH", "400", "read-only attribute").

---

## Page Title & Navigation

| Element | Text | Notes / Tone |
|---------|------|--------------|
| Browser `<title>` (view mode) | `My profile — NorthBank` | Standard "Page — Brand" pattern |
| Browser `<title>` (edit mode) | `Edit profile — NorthBank` | Updated on mode switch |
| `<h1>` — View Mode | `My profile` | Short, ownership-affirming |
| `<h1>` — Edit Mode | `Edit profile` | Clear intent; mirrors button label |
| AppTopNav — "My profile" menu item | `My profile` | Matches H1; consistent vocabulary |
| AppTopNav — "Sign out" menu item | `Sign out` | Two words; "Log out" is equally valid but "Sign out" chosen for consistency with US-003 |
| AppTopNav — avatar `aria-label` | `Account menu, [First name] [Last name]` | Dynamic; e.g. "Account menu, Alexander Mitchell" |
| AppTopNav — hamburger `aria-label` (mobile) | `Open navigation menu` | Descriptive action |
| Skip link (keyboard / screen reader only) | `Skip to main content` | WCAG 2.4.1 bypass blocks |
| Breadcrumb / back context | *(none — profile is reached from user menu; breadcrumb adds no value)* | Deliberate omission |

---

## Field Labels — View Mode

| Field | Label Text | Value Display Format |
|-------|-----------|---------------------|
| First name | `First name` | As stored (e.g. `Alexander`) |
| Last name | `Last name` | As stored (e.g. `Mitchell`) |
| Email address | `Email address` | As stored (e.g. `alex.mitchell@email.com`) |
| Phone number | `Phone number` | As stored (e.g. `+44 7911 123456`) — display may add space after country code for readability; value stored as E.164 |
| Date of birth | `Date of birth` | Human-readable format: `14 March 1990` (DD Month YYYY) — not ISO 8601 |

---

## Field Labels — Edit Mode

| Element | Text | Notes |
|---------|------|-------|
| First name label | `First name` | Consistent with view mode |
| First name `aria-label` | `First name` | Same as label — `<label for>` used |
| First name `autocomplete` | `given-name` | Browser autofill |
| Last name label | `Last name` | — |
| Last name `autocomplete` | `family-name` | — |
| Email address label | `Email address` | Label present on ReadOnlyField too |
| Phone number label | `Phone number` | — |
| Phone number `autocomplete` | `tel` | — |
| Phone number `placeholder` | `+447911123456` | Minimal placeholder — label is always visible; placeholder is supplementary only |
| Phone number `inputmode` | `tel` | Triggers phone keyboard on iOS/Android |
| Date of birth label | `Date of birth` | — |

---

## Helper Text

| Element | Text | Placement |
|---------|------|-----------|
| Phone number helper (persistent in edit mode) | `Use international format: + country code + number, e.g. +447911123456` | Below phone input; always visible while in edit mode; replaced by error text if error occurs |
| Lock icon tooltip — Email address | `Email address can't be changed online. Contact us if you need to update it.` | Tooltip on hover/focus of 🔒 icon |
| Lock icon tooltip — Date of birth | `Date of birth can't be changed online. Contact us if you need to update it.` | Tooltip on hover/focus of 🔒 icon |
| Lock icon `aria-label` | `Read-only field` | Accessible name of the 🔒 icon image |
| Phone field `aria-describedby` content | Points to the helper text `<span>` (or error `<span>` if error is active) | Programmatic; not visible text |

---

## Primary & Secondary Actions

| Element | Text | State variation |
|---------|------|----------------|
| Edit button (view mode) | `Edit profile` | Default |
| Edit button `aria-label` | `Edit your profile details` | More descriptive for screen readers |
| Save button (edit mode) | `Save changes` | Default |
| Save button — loading state | `Saving…` | While PATCH is in flight |
| Save button `aria-label` (loading) | `Saving your profile changes` | — |
| Cancel button | `Cancel` | Default |
| Cancel button `aria-label` | `Cancel editing and discard changes` | Clarifies consequence on screen reader |
| "Try again" (in error banner) | `Try again` | Short; action-oriented |
| "Try again" `aria-label` (in banner) | `Try again to save your changes` | Contextual |
| "Try again" (page load error) | `Try again` | Same label, different context |

---

## Success Messages

| Element | Text | Notes |
|---------|------|-------|
| `SuccessToast` message | `Profile updated` | Short, past tense — confirms result, not action |
| Toast `aria-label` | `Profile updated successfully` | Slightly expanded for screen reader context |
| Toast dismiss button `aria-label` | `Dismiss notification` | Standard dismiss affordance |

---

## Validation Error Messages — Client-Side

> Rules: field-level only. Say what is wrong AND how to fix it. No "Invalid input."

| Field | Trigger | Error Text |
|-------|---------|-----------|
| First name | Left blank | `First name is required.` |
| First name | Over 100 characters | `First name must be 100 characters or fewer.` |
| Last name | Left blank | `Last name is required.` |
| Last name | Over 100 characters | `Last name must be 100 characters or fewer.` |
| Phone number | Left blank | `Phone number is required.` |
| Phone number | Does not start with `+` | `Enter your number starting with + and country code, e.g. +447911123456.` |
| Phone number | Contains non-numeric characters after `+` | `Enter your number starting with + and country code, e.g. +447911123456.` |
| Phone number | Too short (< 8 digits after `+`) | `Enter your number starting with + and country code, e.g. +447911123456.` |
| Phone number | Too long (> 15 digits total) | `Enter your number starting with + and country code, e.g. +447911123456.` |

> **Design note:** All phone validation errors use the same message. The E.164 standard is not
> mentioned by name — the example (`+447911123456`) is the only instruction needed.
> Distinguishing between "too short" and "wrong format" at the user level adds no value.

---

## Validation Error Messages — Server-Side (API → UI mapping)

| API Response | Field | User-Facing Error Text | Notes |
|-------------|-------|----------------------|-------|
| `400 { phoneNumber: "Field format invalid" }` (AC4) | Phone field | `Enter your number starting with + and country code, e.g. +447911123456.` | Mirrors client-side message for consistency |
| `400 { field: "email", message: "Field is not editable" }` (AC3) | `FormErrorBanner` (fallback) | `We couldn't save your changes. Please refresh the page and try again.` | Should never be seen by users — UI prevents sending email in PATCH. Defensive fallback only. |
| `400 { field: "dateOfBirth", message: "Field is not editable" }` (AC3) | `FormErrorBanner` (fallback) | `We couldn't save your changes. Please refresh the page and try again.` | Same as above — defensive only |
| `400` (generic / unexpected field) | `FormErrorBanner` | `We couldn't save your changes. Please check your information and try again.` | — |
| `401 Unauthorized` | (global redirect) | *(handled by session expiry flow — see below)* | — |
| `500` / network error | `FormErrorBanner` | `We couldn't save your changes right now. Please try again in a moment.` | Avoids blaming the user; "in a moment" implies transient |

---

## Session & Authentication Messages

| Trigger | Location | Text |
|---------|----------|------|
| 401 on GET /api/v1/profile | Toast on `/login` page (on redirect) | `Your session has expired. Please sign in to continue.` |
| 401 on PATCH /api/v1/profile | Toast on `/login` page (on redirect) | `Your session has expired. Please sign in to continue.` |

> Consistent with the session-expiry message pattern established in US-003.

---

## Page Load Error State

| Element | Text | Notes |
|---------|------|-------|
| Error heading | `We couldn't load your profile` | States what went wrong without blaming the user |
| Error body | `Please try again. If the problem continues, contact our support team.` | Two-part recovery: self-serve first, support second |
| Retry button | `Try again` | Same as network error CTA — consistent vocabulary |
| Page `aria-label` (during error state) | `Profile page – error loading data` | Announces state to screen reader |

---

## Loading State

| Element | Text / ARIA | Notes |
|---------|------------|-------|
| Page `aria-busy` | `aria-busy="true"` | On `<main>` during GET |
| Page `aria-label` (loading) | `Loading your profile` | Announced by screen readers |
| FieldSkeleton `aria-hidden` | `aria-hidden="true"` | Decorative; not described individually |

---

## ARIA Labels — Key Interactive Elements

| Component | `aria-label` / `aria-*` | Notes |
|-----------|------------------------|-------|
| `<main>` (loading) | `aria-label="Loading your profile"`, `aria-busy="true"` | — |
| `<main>` (view mode) | `aria-label="Your profile"` | — |
| `<main>` (edit mode) | `aria-label="Edit your profile"` | — |
| `<form>` (edit mode) | `aria-label="Profile edit form"`, `aria-busy="true/false"` | — |
| Email `ReadOnlyField` | `aria-label="Email address, read-only"` on the value element; `aria-describedby="email-lock-tooltip"` | Combined label + tooltip description |
| DOB `ReadOnlyField` | `aria-label="Date of birth, read-only"` on the value element; `aria-describedby="dob-lock-tooltip"` | — |
| LockIcon (email) | `role="img"`, `aria-label="Read-only field"`, `tabindex="0"`, `aria-describedby="email-lock-tooltip"` | — |
| LockIcon (DOB) | `role="img"`, `aria-label="Read-only field"`, `tabindex="0"`, `aria-describedby="dob-lock-tooltip"` | — |
| LockTooltip (email) | `id="email-lock-tooltip"`, `role="tooltip"` | Linked from LockIcon |
| LockTooltip (DOB) | `id="dob-lock-tooltip"`, `role="tooltip"` | Linked from LockIcon |
| SubmitButton (saving) | `aria-busy="true"`, `aria-disabled="true"` | During PATCH in flight |
| SuccessToast | `role="status"`, `aria-live="polite"`, `aria-label="Profile updated successfully"` | — |
| FormErrorBanner | `role="alert"`, `aria-live="assertive"` | Immediate announcement |
| PhoneField error span | `role="alert"`, `id="phone-error"` | `aria-describedby="phone-error"` on the input |
| TextField error span | `role="alert"`, `id="[field]-error"` | `aria-describedby="[field]-error"` on the input |
| `×` dismiss (SuccessToast) | `aria-label="Dismiss notification"`, `type="button"` | — |

---

## Empty State

> **Not applicable for this page.** Profile data exists as soon as the user is authenticated
> (created during registration — US-001). There is no zero-data state for the profile page.
>
> Defensive: if the GET response returns empty/null values for individual fields (e.g. phone
> was never set), the field in View Mode displays `—` (an em-dash) to indicate "not provided",
> and the phone input in Edit Mode is empty (blank) rather than showing a placeholder as a value.

| Field | Text when value is absent |
|-------|--------------------------|
| Any field with null/missing value | `—` (em-dash, `aria-label="Not provided"`) |

---

## Tone Guide

| Situation | Tone | Example |
|-----------|------|---------|
| Field labels | Neutral, noun-only | `Phone number` (not "Your phone number") |
| Helper text | Instructional, friendly | `Use international format: + country code + number, e.g. +447911123456` |
| Error messages | Specific, constructive, never blaming | `Enter your number starting with + and country code, e.g. +447911123456.` (not "Invalid phone") |
| Success messages | Minimal, past-tense, confirming | `Profile updated` (not "You've successfully updated your profile!") |
| Disabled field explanation | Informative, supportive | `Email address can't be changed online. Contact us if you need to update it.` |
| Network error | Apologetic, temporary-feeling, action-available | `We couldn't save your changes right now. Please try again in a moment.` |
| Page load error | Matter-of-fact, solution-forward | `We couldn't load your profile. Please try again.` |
