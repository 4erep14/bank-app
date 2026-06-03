# Interaction States — US-005: View & Update Customer Profile

> One section per key component. Covers all required states:
> default · hover · focus · loading · success · error · empty · disabled.
> WCAG 2.1 AA requirements noted per component.

---

## `ProfilePage` — Page-level states

| State | Visual Behaviour | ARIA / Accessibility |
|-------|-----------------|----------------------|
| **Loading** | `ProfileSkeleton` rendered; page `<main>` has `aria-busy="true"` and `aria-label="Loading your profile"` | Live region; screen readers announce "Loading your profile" |
| **Loaded (view)** | `ProfileView` rendered; H1 "My profile" receives focus on mount | `<h1>` focus on navigation |
| **Loaded (edit)** | `ProfileEditForm` rendered; H1 "Edit profile" receives focus | H1 updated; `<h1>` receives focus |
| **Load error** | `PageErrorState` rendered with retry CTA | `role="alert"` on error message; focus set to error heading |
| **Save success** | Returns to view mode; `SuccessToast` shown | `role="status"` toast announced politely |
| **401 (any request)** | Redirect to `/login` (global interceptor); session-expired toast at destination | No content shown; no error on this page |

---

## `ProfileView` — View Mode

| State | Visual Behaviour | Notes |
|-------|-----------------|-------|
| **Default** | All 5 fields displayed as label + value pairs. "Edit profile" primary button visible. | Static; no form elements present |
| **Hover on "Edit profile" button** | Button background darkens (`#1E40AF`), cursor pointer | Consistent with NorthBank button hover pattern |
| **Focus on "Edit profile" button** | 2px solid `#2563EB` focus ring, 2px offset | Keyboard visible focus (WCAG 2.4.7) |
| **Hover on 🔒 LockIcon** | `LockTooltip` appears; cursor changes to `help` | `aria-describedby` links icon to tooltip |
| **Focus on 🔒 LockIcon** (keyboard) | `LockTooltip` appears; 2px focus ring on icon | Tooltip announced by screen reader via `aria-describedby` |
| **Empty (no profile data)** | Should not occur in authenticated flow; if it does, `PageErrorState` shown | Defensive fallback |

---

## `ProfileEditForm` — Edit Mode

| State | Visual Behaviour | Notes |
|-------|-----------------|-------|
| **Default (idle)** | 3 editable inputs pre-filled; 2 ReadOnlyFields; "Save changes" (primary) + "Cancel" (secondary) | Form is ready for input |
| **Dirty (user has typed)** | No change to form appearance; "Save changes" remains enabled | No unsaved-changes warning needed (non-destructive task) |
| **Submitting** | All inputs dimmed (`opacity: 0.6`), `pointer-events: none`. SubmitButton shows spinner + "Saving…". Cancel disabled. `aria-busy="true"` on form. | Prevents double-submission |
| **Success** | Form unmounts; ProfileView mounts; SuccessToast shown | See SuccessToast states |
| **Field validation error** | Errored field shows red border + ! icon + inline error. Save button disabled while error active. | Focus set to first errored field |
| **Server error (non-field)** | `FormErrorBanner` appears above first field; form re-enabled; data preserved | `role="alert"` announces error |
| **Escape key pressed** | Same as clicking Cancel; Edit Mode unmounts; View Mode restores | User control & freedom (Nielsen #3) |

---

## `TextField` — Generic Editable Input (First name, Last name)

| State | Visual Behaviour | ARIA / Accessibility |
|-------|-----------------|----------------------|
| **Default** | White background, `1px solid #D1D5DB` border, `border-radius: 6px`. Label above. Value pre-filled. | `<label for="fieldId">` associated via `for` / `id` |
| **Hover** | Border colour → `#9CA3AF` (slightly darker). Cursor: `text`. | No ARIA change |
| **Focus** | Border: `2px solid #2563EB` (blue). Subtle `box-shadow: 0 0 0 3px rgba(37,99,235,0.15)`. | `aria-describedby` links to error/helper if present |
| **Valid (after blur, value present)** | Border returns to default. No explicit ✓ icon (not needed for name fields). | Error node removed from DOM |
| **Error** | Border: `2px solid #DC2626` (red). Background: `#FEF2F2`. `!` icon right-aligned in field. Inline error text below. | Error `<span>` has `role="alert"`, `id` linked via `aria-describedby` on input |
| **Disabled (during submit)** | `opacity: 0.6`, `background: #F9FAFB`, `cursor: not-allowed`. `pointer-events: none`. | `aria-disabled="true"`, `disabled` attribute |
| **Empty required (on submit attempt)** | Same error style as above. Error: "First name is required." | Announced by screen reader |

---

## `PhoneField` — Phone Number Input (E.164)

| State | Visual Behaviour | ARIA / Accessibility |
|-------|-----------------|----------------------|
| **Default** | Same as `TextField`. Helper text below: "Use international format: + country code + number, e.g. +447911123456" in muted grey `#6B7280`. | `aria-describedby` links to helper text `<span>` |
| **Hover** | Same as `TextField` hover. | — |
| **Focus** | Same as `TextField` focus. Helper text remains visible. | — |
| **Typing (in progress)** | No live validation while typing (validate on blur only). | Avoids premature errors mid-typing |
| **Valid (after blur)** | Border returns to default. Helper text remains (reinforcement). | Error node removed if previously shown |
| **Invalid (after blur — E.164)** | Red border + `!` icon. Helper text replaced by error message. | Error `<span>` `role="alert"` replaces helper. `aria-describedby` updated |
| **Disabled (during submit)** | Same as `TextField` disabled. | Same as `TextField` |
| **Server error (400 from PATCH)** | Same red error style; error text from server mapped to friendly copy. | `role="alert"` on error span |

---

## `ReadOnlyField` — Read-Only Display (Email address, Date of birth)

| State | Visual Behaviour | ARIA / Accessibility |
|-------|-----------------|----------------------|
| **Default** | Label in muted grey (`#6B7280`). Value in normal text (`#374151`). Lock icon (🔒) to the right. NOT an `<input>` element — rendered as `<p>` or `<dd>`. | `<dt>` label + `<dd>` value; lock icon: `tabindex="0"`, `role="img"`, `aria-label="Read-only"`, `aria-describedby="[field]-lock-tooltip"` |
| **Hover (on lock icon)** | Cursor: `help`. Tooltip fades in (200ms opacity transition). | `LockTooltip` with `role="tooltip"` becomes visible |
| **Focus (on lock icon — keyboard)** | 2px focus ring on lock icon. Tooltip appears. | Tooltip announced via `aria-describedby` |
| **Blur (lock icon)** | Tooltip fades out (100ms). | — |
| **No hover/focus** | Tooltip hidden (`display: none` or `visibility: hidden`). | Hidden from screen readers when not active |

**Key rule:** `ReadOnlyField` is NEVER an `<input disabled>`. It is a semantic display element.
The value is never placed inside a form element. This prevents any possibility of accidental
form submission of read-only values and avoids the visual/ARIA ambiguity of a "disabled input."

---

## `LockIcon` + `LockTooltip`

| State | Visual Behaviour | ARIA |
|-------|-----------------|------|
| **Default** | 🔒 icon, `color: #9CA3AF` (grey). 16×16px. | `tabindex="0"`, `role="img"`, `aria-label="Read-only field"`, `aria-describedby="[id]-tooltip"` |
| **Hover / Focus** | Icon colour → `#4B5563` (darker). Tooltip visible. | `LockTooltip` appears; `role="tooltip"`, `id="[id]-tooltip"` |
| **Tooltip content** | White card: "This field can't be changed online. Contact us if you need to update it." | Plain text; no interactive elements inside tooltip |

---

## `SubmitButton` — "Save changes" / "Edit profile"

| State | Visual Behaviour | ARIA / Accessibility |
|-------|-----------------|----------------------|
| **Default** | Background `#1D4ED8` (blue), white text, `border-radius: 6px`, `min-height: 48px`. Label: "Save changes" or "Edit profile". | `type="submit"` / `type="button"`, accessible name = visible label |
| **Hover** | Background → `#1E40AF` (darker blue). Cursor: pointer. Subtle lift shadow. | — |
| **Focus** | 2px solid `#93C5FD` focus ring (lighter blue) at 2px offset. | Visible focus (WCAG 2.4.7) |
| **Active (click)** | Background → `#1D3A8A` (pressed). Scale(0.98) micro-interaction. | — |
| **Loading** | Background dims to `#3B82F6`. Left spinner (⟳) + "Saving…" label. Width unchanged (no layout shift). `disabled` attribute set. | `aria-busy="true"`, `aria-disabled="true"` on button and parent form |
| **Disabled (form errors)** | Background: `#93C5FD` (light blue). `cursor: not-allowed`. `opacity: 0.7`. | `aria-disabled="true"`, `disabled` |

---

## `CancelButton` — "Cancel"

| State | Visual Behaviour | ARIA / Accessibility |
|-------|-----------------|----------------------|
| **Default** | Ghost style: transparent background, `border: 1px solid #D1D5DB`, `color: #374151`. | `type="button"`, accessible name "Cancel" |
| **Hover** | Background: `#F3F4F6` (very light grey). Border darkens slightly. | — |
| **Focus** | 2px solid `#2563EB` focus ring. | Visible focus |
| **Disabled (during submit)** | `opacity: 0.4`, `cursor: not-allowed`. | `aria-disabled="true"`, `disabled` |
| **Mobile (full-width)** | Expands to 100% width; min-height 48px. Positioned below SubmitButton. | — |

---

## `SuccessToast` — Profile updated confirmation

| State | Visual Behaviour | ARIA / Accessibility |
|-------|-----------------|----------------------|
| **Entering** | Slides in from top (mobile: top-centre; desktop: top-right). Fade + translate animation (200ms). | `role="status"`, `aria-live="polite"` — screen reader announces "Profile updated" when it appears |
| **Visible** | `background: #F0FDF4`, `border-left: 4px solid #16A34A`, green ✓ icon, "Profile updated" text, `×` dismiss button. | ✓ icon is `aria-hidden="true"` (decorative). `×` has `aria-label="Dismiss notification"` |
| **Auto-dismissing** | After 4,000ms: fade out + slide up (200ms). Removed from DOM after transition. | Removal is silent (not announced — `role="status"` only announces insertion) |
| **Dismissed by user** | Same fade + slide transition on `×` click. | `×` is keyboard-focusable, `type="button"` |
| **Focus behaviour** | Toast does NOT receive focus on appearance (non-urgent). Focus remains on "Edit profile" button in View Mode. | `role="status"` (polite); not `role="alert"` (urgent) — success is not an emergency |

---

## `FormErrorBanner` — Network / Server error

| State | Visual Behaviour | ARIA / Accessibility |
|-------|-----------------|----------------------|
| **Entering** | Appears at top of edit form above first field. Fade-in 150ms. | `role="alert"` — announced immediately; `aria-live="assertive"` |
| **Visible** | `background: #FEF2F2`, `border-left: 4px solid #DC2626`, `!` icon, error text, "Try again" link. | Focus set to banner element on appearance |
| **"Try again" clicked** | Banner stays visible until new attempt; spinner on SubmitButton resumes. | "Try again" is `type="button"`, labelled "Try again" |
| **Dismissing** | Hidden when next PATCH succeeds. | Removed from DOM |
| **Multiple triggers** | Banner content updates to latest error; no stacking. | Single `aria-live` region; update replaces previous content |

---

## `PageErrorState` — Profile load failure

| State | Visual Behaviour | ARIA / Accessibility |
|-------|-----------------|----------------------|
| **Default** | Centred in card: ⚠ icon (aria-hidden), heading, explanatory copy, "Try again" button. | ⚠ is decorative; heading is `<h2>` inside main `<h1>` page context; focus set to heading |
| **"Try again" hover / focus** | Same as `SubmitButton` hover/focus pattern. | — |
| **Retrying** | "Try again" → spinner + "Retrying…" — same SubmitButton loading pattern. | `aria-busy="true"` on content region |

---

## `AppTopNav` — Authenticated Navigation Bar

| State | Visual Behaviour | ARIA / Accessibility |
|-------|-----------------|----------------------|
| **Default** | White bar, NorthBank logo left, nav links centre, `UserAvatarMenu` right. `border-bottom: 1px solid #E5E7EB`. | `<nav aria-label="Main navigation">`, `<header>` landmark |
| **Nav link hover** | Text colour → `#1D4ED8`, underline appears. | — |
| **Nav link focus** | 2px focus ring. Current page link: `aria-current="page"`. | — |
| **Avatar menu — default** | Circle with user initials, chevron `▼`. | `aria-haspopup="true"`, `aria-expanded="false"`, `aria-label="Account menu, [Name]"` |
| **Avatar menu — open** | Dropdown: "My profile" link + "Sign out" link. | `aria-expanded="true"`; dropdown has `role="menu"`; items have `role="menuitem"` |
| **Mobile (<640px)** | Logo + hamburger menu `☰`. Nav links hidden. Tapping `☰` reveals a full-screen drawer with nav links + user options. | `aria-label="Open navigation menu"` on hamburger |

---

## `FieldSkeleton` / `ProfileSkeleton` — Loading Placeholders

| State | Visual Behaviour | ARIA / Accessibility |
|-------|-----------------|----------------------|
| **Default** | Rounded rectangle, `background: linear-gradient(90deg, #E5E7EB 25%, #F3F4F6 50%, #E5E7EB 75%)`. Background-position animates left-to-right (pulse/shimmer). | `aria-hidden="true"` — decorative; page-level `aria-busy="true"` covers the announcement |
| **Width variants** | Short (120px for names), medium (200px for phone), long (280px for email), extra-short (100px for DOB). | — |
| **Height** | 20px (matches line-height of rendered text values). | — |

---

## Keyboard Navigation Map — Edit Mode

```
Tab order in Edit Mode:
  1. AppTopNav links (skip to main content available)
  2. [Skip to main content] link (visually hidden, first focusable element)
  3. H1 "Edit profile" (focusable heading, receives focus on mode switch)
  4. First name input
  5. Last name input
  6. Email address LockIcon (🔒) — focusable, opens tooltip; NOT an input
  7. Phone number input
  8. Date of birth LockIcon (🔒) — focusable, opens tooltip; NOT an input
  9. Save changes button
  10. Cancel button

Keyboard shortcuts:
  Enter (in any input)  → submits form (same as Save changes click)
  Escape (anywhere)     → Cancel (same as Cancel button click)
  Tab on last input (phone) → moves to Save changes, NOT Cancel (Save is primary)
```
