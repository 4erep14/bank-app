# Wireframes — US-005: View & Update Customer Profile

Components referenced (PascalCase): `ProfilePage`, `ProfileView`, `ProfileEditForm`,
`ReadOnlyField`, `EditableField`, `PhoneField`, `LockIcon`, `LockTooltip`,
`FieldSkeleton`, `ProfileSkeleton`, `AppTopNav`, `UserAvatarMenu`, `SubmitButton`,
`CancelButton`, `FormErrorBanner`, `SuccessToast`, `PageErrorState`.

Reused from prior stories: `TextField` (US-001), `PhoneField` (US-001),
`SubmitButton` (US-001 / US-004), `FormErrorBanner` (US-001 / US-004), `TrustBadge` (US-001).

Legend:
  `[____]`  editable input field
  `████`    skeleton loading placeholder
  `🔒`      read-only lock icon
  `!`       error state indicator (red)
  `✓`       valid / success indicator (green)
  `⟳`       spinner (loading)
  `▼`       dropdown indicator
  `( Btn )` primary button
  `[ Btn ]` secondary / ghost button
  `···`     tooltip callout

---

## 1 · Loading State — Skeleton (`ProfileSkeleton`)

> Shown immediately on mount while GET /api/v1/profile is in flight.
> Prevents blank-flash; communicates that data is on its way.
> AC1 (data fetching in progress).

### Mobile (<640 px)

```
+----------------------------------+
| 🔒 NorthBank        [Alex M. ▼] |   ← AppTopNav (skeleton: avatar = grey circle)
+----------------------------------+
|                                  |
|  My profile                      |   ← H1 visible immediately (static)
|  ─────────────────────────────── |
|                                  |
|  First name                      |   ← label static
|  [████████████████████████]      |   ← FieldSkeleton (animated pulse)
|                                  |
|  Last name                       |
|  [████████████████████████]      |
|                                  |
|  Email address                   |
|  [████████████████████████]      |
|                                  |
|  Phone number                    |
|  [████████████████████████]      |
|                                  |
|  Date of birth                   |
|  [████████████████████████]      |
|                                  |
|  [██████████████]                |   ← "Edit profile" button skeleton
|                                  |
+----------------------------------+
| © 2026 NorthBank                 |
+----------------------------------+
```

- Skeleton bars: rounded corners, ~16px height, pulsing animation (opacity 0.4 → 1 → 0.4).
- Button skeleton mirrors the real "Edit profile" button dimensions.
- ARIA: `aria-busy="true"` on the main content region; `aria-label="Loading profile…"`.
- Labels are rendered statically (not skeletonised) so screen readers announce what is coming.

### Desktop (>1024 px)

```
+----------------------------------------------------------------------+
| 🔒 NorthBank    Dashboard  Accounts  Transfers     [Alex M. ▼]      |
+----------------------------------------------------------------------+
|                                                                      |
|   +------------------------------------------------------------+     |
|   |  My profile                                                |     |
|   |  ────────────────────────────────────────────────────────  |     |
|   |                                                            |     |
|   |  First name              Last name                        |     |
|   |  [████████████████]      [████████████████]               |     |
|   |                                                            |     |
|   |  Email address                                            |     |
|   |  [████████████████████████████████████]                   |     |
|   |                                                            |     |
|   |  Phone number                                             |     |
|   |  [████████████████████]                                   |     |
|   |                                                            |     |
|   |  Date of birth                                            |     |
|   |  [████████████████]                                       |     |
|   |                                                            |     |
|   |  [██████████████]    ← "Edit profile" button skeleton     |     |
|   +------------------------------------------------------------+     |
|                                                                      |
+----------------------------------------------------------------------+
| © 2026 NorthBank · Privacy · Terms                                   |
+----------------------------------------------------------------------+
```

- Card: max-width 640 px, centred, border-radius 8 px, box-shadow light blue tint.
- Desktop: First name + Last name share one row (2-column grid).

---

## 2 · View Mode — `ProfileView`

> Shown after successful GET /api/v1/profile.
> Displays all 5 fields. Lock icon on read-only fields.
> Single primary action: "Edit profile".
> AC1 (fields displayed), AC3 (email & DOB read-only, visually distinct).

### Mobile (<640 px)

```
+----------------------------------+
| 🔒 NorthBank        [Alex M. ▼] |   ← AppTopNav
+----------------------------------+
|                                  |
|  My profile                      |   ← H1 (receives focus on page load)
|  ─────────────────────────────── |
|                                  |
|  First name                      |   ← field label (muted, small)
|  Alexander                       |   ← field value (normal weight)
|                                  |
|  Last name                       |
|  Mitchell                        |
|                                  |
|  Email address                   |
|  alex.mitchell@email.com   🔒    |   ← LockIcon (focusable, role="img")
|                                  |
|  Phone number                    |
|  +44 7911 123456                 |
|                                  |
|  Date of birth                   |
|  14 March 1990             🔒    |   ← LockIcon
|                                  |
|  ( Edit profile )                |   ← SubmitButton variant, full-width mobile
|                                  |
+----------------------------------+
| © 2026 NorthBank                 |
+----------------------------------+
```

- All field values: `color: #1A1A2E` (near-black), `font-weight: 500`.
- Read-only field labels + lock icons: `color: #6B7280` (muted grey).
- Editable field values (first name, last name, phone): same style as read-only in view mode —
  differentiation only becomes visible when entering Edit Mode.
- "Edit profile": primary blue button (`#1D4ED8`), min-height 48 px (≥44 px touch target).
- Lock icon: `aria-label="Read-only field"`, `role="img"`, `tabindex="0"` so keyboard users
  can focus it to trigger the tooltip.

### Desktop (>1024 px)

```
+----------------------------------------------------------------------+
| 🔒 NorthBank    Dashboard  Accounts  Transfers     [Alex M. ▼]      |
+----------------------------------------------------------------------+
|                                                                      |
|   +------------------------------------------------------------+     |
|   |  My profile                        [ Edit profile ]        |     |
|   |  ────────────────────────────────────────────────────────  |     |
|   |                                                            |     |
|   |  First name              Last name                        |     |
|   |  Alexander               Mitchell                         |     |
|   |                                                            |     |
|   |  Email address                                            |     |
|   |  alex.mitchell@email.com                            🔒    |     |
|   |                                                            |     |
|   |  Phone number                                             |     |
|   |  +44 7911 123456                                          |     |
|   |                                                            |     |
|   |  Date of birth                                            |     |
|   |  14 March 1990                                      🔒    |     |
|   |                                                            |     |
|   +------------------------------------------------------------+     |
|                                                                      |
+----------------------------------------------------------------------+
| © 2026 NorthBank · Privacy · Terms                                   |
+----------------------------------------------------------------------+
```

- Desktop: "Edit profile" button right-aligned in the card header row.
- First name + Last name: two-column grid.
- Email and DOB rows: lock icon right-aligned on the same row as the value.

---

## 3 · Edit Mode — `ProfileEditForm`

> Activated when user clicks "Edit profile".
> Editable fields become inputs (pre-filled). Read-only fields remain as static text.
> Primary action: "Save changes". Secondary: "Cancel".
> AC2 (editable fields: first name, last name, phone), AC3 (email/DOB still static), AC4 (phone validation hint).

### Mobile (<640 px)

```
+----------------------------------+
| 🔒 NorthBank        [Alex M. ▼] |
+----------------------------------+
|                                  |
|  Edit profile                    |   ← H1 (updated from "My profile"; focus set here)
|  ─────────────────────────────── |
|                                  |
|  First name                      |   ← label
|  [Alexander___________________]  |   ← TextField, autocomplete="given-name"
|                                  |
|  Last name                       |
|  [Mitchell____________________]  |   ← TextField, autocomplete="family-name"
|                                  |
|  Email address            🔒     |   ← ReadOnlyField (static <p>, NOT <input>)
|  alex.mitchell@email.com         |     lock icon focusable for tooltip
|                                  |
|  Phone number                    |   ← label
|  [+447911123456_________________]|   ← PhoneField, type="tel",
|  Use international format:       |     autocomplete="tel"
|  + country code + number,        |   ← persistent helper text (always visible)
|  e.g. +447911123456              |
|                                  |
|  Date of birth             🔒    |   ← ReadOnlyField (static <p>, NOT <input>)
|  14 March 1990                   |     lock icon focusable for tooltip
|                                  |
|  ( Save changes )                |   ← SubmitButton, full-width, primary blue
|  [ Cancel ]                      |   ← CancelButton, full-width, ghost/text
|                                  |
+----------------------------------+
| © 2026 NorthBank                 |
+----------------------------------+
```

- Tab order (keyboard): First name → Last name → Phone → Save changes → Cancel.
  Email and DOB static rows are skipped by Tab (not interactive inputs).
- The lock icon on Email and DOB IS reachable by Tab (tabindex="0") so keyboard users can
  trigger the tooltip — but the fields themselves have no editable interaction.
- "Cancel" is visually secondary (ghost button or text link style) so "Save changes" is the
  clear primary action.
- Escape key in any editable field → same as clicking Cancel.

### Desktop (>1024 px)

```
+----------------------------------------------------------------------+
| 🔒 NorthBank    Dashboard  Accounts  Transfers     [Alex M. ▼]      |
+----------------------------------------------------------------------+
|                                                                      |
|   +------------------------------------------------------------+     |
|   |  Edit profile                                              |     |
|   |  ────────────────────────────────────────────────────────  |     |
|   |                                                            |     |
|   |  First name              Last name                        |     |
|   |  [Alexander_________]    [Mitchell__________]             |     |
|   |                                                            |     |
|   |  Email address                                     🔒     |     |
|   |  alex.mitchell@email.com                                   |     |
|   |                                                            |     |
|   |  Phone number                                             |     |
|   |  [+447911123456_______________________________]           |     |
|   |  Use international format: + country code + number,       |     |
|   |  e.g. +447911123456                                       |     |
|   |                                                            |     |
|   |  Date of birth                                     🔒     |     |
|   |  14 March 1990                                            |     |
|   |                                                            |     |
|   |          [ Cancel ]        ( Save changes )               |     |
|   +------------------------------------------------------------+     |
|                                                                      |
+----------------------------------------------------------------------+
```

- Desktop: Cancel + Save changes right-aligned in a button row at the bottom of the form.
  Cancel is left of Save changes; Save changes is the rightmost and visually dominant action.
- First name + Last name: two-column grid (same as View Mode).

---

## 4 · Lock Icon Tooltip — `LockTooltip`

> Appears on hover (mouse) or focus (keyboard/screen reader) of the 🔒 icon.
> Explains read-only policy; provides support direction.
> AC3 (email & DOB read-only — contextual explanation).

```
  Email address
  alex.mitchell@email.com    🔒
                              │
                    ┌─────────┴──────────────────────┐
                    │  🔒 This field can't be         │
                    │  changed online. Contact us      │  ← LockTooltip
                    │  if you need to update it.       │     role="tooltip"
                    └──────────────────────────────────┘
```

- Tooltip: white background, `border: 1px solid #E5E7EB`, `border-radius: 6px`,
  `box-shadow: 0 2px 8px rgba(0,0,0,0.12)`, max-width 240 px, 12 px padding.
- Appears above the icon on mobile (to avoid viewport edge); below on desktop.
- Keyboard: shown on focus, dismissed on blur or Escape.
- Mouse: shown on hover, dismissed on mouseout.
- ARIA: `role="tooltip"`, `id="email-lock-tooltip"`, icon has `aria-describedby="email-lock-tooltip"`.

---

## 5 · Validation Error State — Invalid Phone (AC4)

> Shown after user blurs the phone field with an invalid value,
> or after a PATCH returns 400 with a phone field error.

### Mobile (<640 px)

```
+----------------------------------+
|  Edit profile                    |
|  ─────────────────────────────── |
|                                  |
|  First name                      |
|  [Alexander___________________]  |
|                                  |
|  Last name                       |
|  [Mitchell____________________]  |
|                                  |
|  Email address            🔒     |
|  alex.mitchell@email.com         |
|                                  |
|  Phone number                    |
|  [07911123456__________________] !|  ← red border + ! icon (right)
|  ! Enter your number starting    |  ← inline error (role="alert")
|    with + and country code,      |
|    e.g. +447911123456.           |
|                                  |
|  Date of birth             🔒    |
|  14 March 1990                   |
|                                  |
|  ( Save changes )                |
|  [ Cancel ]                      |
+----------------------------------+
```

- Phone field border: `2px solid #DC2626` (red), background `#FEF2F2` (light red tint).
- Error icon: `!` exclamation in a red circle, right-aligned inside the field border.
- Inline error text: `color: #DC2626`, `font-size: 14px`, appears below the input.
- Error text uses `role="alert"` — announced immediately by screen readers on appearance.
- Helper text ("Use international format…") is REPLACED by the error text (not shown simultaneously).
- The `!` icon plus the red border plus the text ensures error is never colour-only (WCAG 1.4.1).
- Focus is placed on the phone field after a failed submit attempt.
- "Save changes" button is disabled while any field has an active error.

---

## 6 · Saving State — Submitting

> Shown from the moment "Save changes" is clicked until PATCH response arrives.
> Prevents double-submission. Communicates that work is in progress.

### Mobile (<640 px)

```
+----------------------------------+
|  Edit profile                    |
|  ─────────────────────────────── |
|                                  |
|  First name                      |
|  [Alexander___________________]  ← dimmed / disabled
|                                  |
|  Last name                       |
|  [Mitchell____________________]  ← dimmed / disabled
|                                  |
|  Email address            🔒     |
|  alex.mitchell@email.com         |
|                                  |
|  Phone number                    |
|  [+447911123456________________] ← dimmed / disabled
|  Use international format…       |
|                                  |
|  Date of birth             🔒    |
|  14 March 1990                   |
|                                  |
|  (  ⟳  Saving…  )               ← SubmitButton: spinner + label
|                                    aria-busy="true", disabled
|  [ Cancel ]                      ← also disabled during save
+----------------------------------+
```

- All editable inputs: `opacity: 0.6`, `pointer-events: none`, `aria-disabled="true"`.
- SubmitButton: spinner left of label, button background dims slightly, `cursor: not-allowed`.
- Cancel button is also disabled during save (prevents race conditions).
- Form has `aria-busy="true"` during submission.
- Button text changes from "Save changes" → "Saving…" (no layout shift — button width is fixed).

---

## 7 · Success State — After Successful PATCH (AC5)

> Shown after 200 OK response from PATCH /api/v1/profile.
> Returns to View Mode immediately; toast confirms success.
> Updated values visible in the ProfileView (closes the loop — AC5).

### Mobile (<640 px)

```
+----------------------------------+
| 🔒 NorthBank        [Alex M. ▼] |
+─────────────────────────────────+
| ✓  Profile updated               |  ← SuccessToast (role="status")
|                           [×]    |     top of page, green left-border accent
+----------------------------------+
|                                  |
|  My profile                      |   ← H1 (page title returned to view mode)
|  ─────────────────────────────── |   Focus set here on return to view mode
|                                  |
|  First name                      |
|  Alexander                       |   ← (updated value if changed)
|                                  |
|  Last name                       |
|  Mitchell                        |
|                                  |
|  Email address                   |
|  alex.mitchell@email.com   🔒    |
|                                  |
|  Phone number                    |
|  +44 7700 123456                 |   ← NEW value — confirms AC5 persistence
|                                  |
|  Date of birth                   |
|  14 March 1990             🔒    |
|                                  |
|  ( Edit profile )                |
+----------------------------------+
| © 2026 NorthBank                 |
+----------------------------------+
```

- `SuccessToast`: fixed position, top of viewport (desktop: top-right; mobile: top-centre),
  `background: #F0FDF4`, `border-left: 4px solid #16A34A` (green),
  green `✓` icon, text "Profile updated", dismiss `×` button.
- `role="status"` (not "alert" — success is not urgent); `aria-live="polite"`.
- Toast auto-dismisses after 4 seconds; `×` dismisses immediately.
- Focus is returned to the "Edit profile" button (logical focus management).
- The updated phone number is visible in View Mode, confirming to the user that the change
  persisted — satisfying AC5 visually without requiring a separate API call.

### Desktop (>1024 px) — Toast position

```
+----------------------------------------------------------------------+
| 🔒 NorthBank    Dashboard  Accounts  Transfers    [Alex M. ▼]       |
+----------------------------------------------------------------------+
|                                           +------------------------+ |
|   +-------------------------+             | ✓  Profile updated  × | | ← top-right toast
|   |  My profile   [ Edit ] |             +------------------------+ |
|   |  ─────────────────────  |                                        |
|   | ...updated values...   |                                        |
|   +-------------------------+                                        |
+----------------------------------------------------------------------+
```

---

## 8 · Network / Server Error State — `FormErrorBanner`

> Shown when PATCH returns 5xx or network timeout.
> Edit Mode is preserved; all entered data is retained.

### Mobile (<640 px)

```
+----------------------------------+
|  Edit profile                    |
|  ─────────────────────────────── |
|                                  |
|  !  We couldn't save your        |  ← FormErrorBanner (role="alert")
|     changes. Please try again.   |     amber/red left-border, ! icon
|     [ Try again ]                |     "Try again" re-fires PATCH
|                                  |
|  First name                      |
|  [Alexander___________________]  |  ← form back to enabled state
|                                  |
|  Last name                       |
|  [Mitchell____________________]  |
|                                  |
|  Email address            🔒     |
|  alex.mitchell@email.com         |
|                                  |
|  Phone number                    |
|  [+44 7700 123456_____________]  |  ← user's entered value preserved
|                                  |
|  Date of birth             🔒    |
|  14 March 1990                   |
|                                  |
|  ( Save changes )                |
|  [ Cancel ]                      |
+----------------------------------+
```

- `FormErrorBanner`: `background: #FEF2F2`, `border-left: 4px solid #DC2626`,
  `!` icon, error text, inline "Try again" link (not a full button — secondary prominence).
- `role="alert"` — announced immediately by screen readers.
- Focus is placed on the banner's heading/message after it appears.
- All form inputs are re-enabled; user's edits are preserved.

---

## 9 · Page Load Error State — `PageErrorState`

> Shown when GET /api/v1/profile fails with 5xx or network error (not 401).
> No profile data to display; edit mode is blocked.

```
+----------------------------------+
| 🔒 NorthBank        [Alex M. ▼] |
+----------------------------------+
|                                  |
|  My profile                      |
|  ─────────────────────────────── |
|                                  |
|       ⚠                          |  ← warning icon (decorative, aria-hidden)
|                                  |
|  We couldn't load your           |
|  profile right now.              |
|                                  |
|  Please try again. If the        |
|  problem continues, contact      |
|  our support team.               |
|                                  |
|  ( Try again )                   |  ← re-fires GET request
|                                  |
+----------------------------------+
```

---

## Responsive Behaviour Summary

| Breakpoint | Layout changes |
|------------|----------------|
| **Mobile (<640 px)** | Single column. All fields stacked. "Edit profile" / "Save changes" full-width. Cancel full-width below Save. Toast top-centre. Fields 100% width. Min input height 48 px. |
| **Tablet (640–1024 px)** | Card centred, max-width ~580 px. First name + Last name in 2-column row. Buttons right-aligned. Toast top-right. |
| **Desktop (>1024 px)** | Card centred, max-width 640 px, full app nav bar. 2-column name row. "Edit profile" right-aligned in card header. Cancel + Save right-aligned button row at bottom. Toast top-right fixed. |

---

## Component Map

| Component | Purpose | Key Props | Events | Reusable? |
|-----------|---------|-----------|--------|-----------|
| `ProfilePage` | Route container at `/profile`; owns API calls and mode state | — | — | No |
| `ProfileView` | View mode: displays all 5 fields as static text | `profile: ProfileData` | `onEdit()` | No |
| `ProfileEditForm` | Edit mode: form with 3 editable inputs | `profile: ProfileData`, `isSubmitting: boolean`, `serverError?: string`, `fieldErrors?: FieldErrors` | `onSubmit(values)`, `onCancel()` | No |
| `ReadOnlyField` | Static text display with optional lock icon | `label: string`, `value: string`, `locked?: boolean`, `lockTooltipId?: string` | — | **Yes** (US-005+) |
| `LockIcon` | Focusable lock icon that triggers tooltip | `tooltipId: string`, `aria-label: string` | `onFocus()`, `onBlur()`, `onMouseEnter()`, `onMouseLeave()` | **Yes** |
| `LockTooltip` | Tooltip for read-only field explanation | `id: string`, `text: string` | — | **Yes** |
| `FieldSkeleton` | Animated loading placeholder for a single field value | `width?: string` | — | **Yes** (all loading states) |
| `ProfileSkeleton` | Full profile card loading state (composes FieldSkeleton) | — | — | No |
| `AppTopNav` | Authenticated top navigation bar | `userName: string`, `navLinks: NavLink[]` | `onSignOut()`, `onProfileClick()` | **Yes** (all authenticated pages) |
| `UserAvatarMenu` | Dropdown menu from avatar in top nav | `userName: string`, `userInitials: string` | `onProfileClick()`, `onSignOut()` | **Yes** |
| `SuccessToast` | Transient success confirmation | `message: string`, `autoDismissMs?: number` | `onDismiss()` | **Yes** (US-001+) |
| `PageErrorState` | Full-page load failure state | `message: string`, `onRetry: () => void` | `onRetry()` | **Yes** |
| `SubmitButton` | Primary action button with loading state | `label: string`, `loadingLabel: string`, `isLoading: boolean`, `disabled: boolean` | `onClick()` | **Yes** (US-001, US-004) |
| `CancelButton` | Secondary ghost/text button | `label: string`, `disabled: boolean` | `onClick()` | **Yes** |
| `FormErrorBanner` | Non-field server/network error banner | `message: string`, `onRetry?: () => void` | `onRetry()` | **Yes** (US-001, US-004) |
| `TextField` | Labelled text input | `label`, `name`, `value`, `error`, `autocomplete`, `disabled` | `onChange`, `onBlur` | **Yes** (US-001) |
| `PhoneField` | Tel input + E.164 helper text | `label`, `value`, `error`, `helperText`, `disabled` | `onChange`, `onBlur` | **Yes** (US-001) |
