# UX Flow — US-005: View & Update Customer Profile

> Satisfies: AC1 · AC2 · AC3 · AC4 · AC5 · AC6

---

## Screens / Views Involved

| Screen | Route | Purpose |
|--------|-------|---------|
| `ProfilePage` | `/profile` | Container; owns GET/PATCH API calls and mode state |
| `ProfileView` | (mode: view) | Displays current profile data; "Edit profile" CTA |
| `ProfileEditForm` | (mode: edit) | Editable form; pre-filled; validates; POSTs PATCH |
| `AppTopNav` | (persistent) | Authenticated navigation bar; user avatar menu |
| Login Page | `/login` | Recovery target for expired sessions (AC6) |

---

## Flow 1 — Happy Path: View Profile (AC1)

```
User clicks avatar menu → "My profile"
          │
          ▼
[ProfilePage mounts at /profile]
          │
          ▼
[Loading state: skeleton placeholders shown]
  GET /api/v1/profile fired (JWT from localStorage — AC1)
          │
     ┌────┴─────────────────────┐
     │ 200 OK                   │ 401 Unauthorized
     ▼                          ▼
[ProfileView rendered]     [Redirect → /login]
  All 5 fields displayed   Session-expired toast shown
  • First name             (handled globally — AC6)
  • Last name
  • Email address  🔒
  • Phone number
  • Date of birth  🔒
  [ Edit profile ]  ← primary CTA
```

**AC satisfied:** AC1 (GET profile fields displayed), AC6 (401 handled)

---

## Flow 2 — Happy Path: Update Profile (AC2, AC5)

```
[ProfileView — view mode]
          │
User clicks [ Edit profile ]
          │
          ▼
[ProfileEditForm — edit mode activated]
  Editable inputs pre-filled:
    First name  [Alexander___]
    Last name   [Mitchell____]
    Phone       [+447911123456]
  Read-only displayed as static text:
    Email address  alex@email.com 🔒
    Date of birth  14 Mar 1990   🔒
  [ Save changes ]   [ Cancel ]
          │
User edits one or more fields (e.g. updates phone)
          │
User clicks [ Save changes ]
          │
          ▼
[Saving state]
  Button → spinner + "Saving…" (disabled)
  Form fields disabled
  PATCH /api/v1/profile fired (changed fields only — AC2)
          │
     ┌────┴──────────────────────────┐
     │ 200 OK                        │ Error (see Flow 4 / 5)
     ▼                               ▼
[Success: mode → view]         [Error handling flows]
  "Profile updated" toast (role="status")
  View Mode restored, updated values shown
  Toast auto-dismisses after 4 seconds
  Focus returns to "Edit profile" button (AC5)
```

**AC satisfied:** AC2 (PATCH editable fields), AC5 (updated data shown in next render)

---

## Flow 3 — Happy Path: Cancel Edit (no changes saved)

```
[ProfileEditForm — edit mode]
          │
User clicks [ Cancel ] (or presses Escape)
          │
          ▼
[ProfileView — view mode restored]
  Original values displayed (no PATCH fired)
  Focus returns to "Edit profile" button
  No toast shown (no action taken)
```

**UX note:** No confirm dialog — the cancel is non-destructive (user can always re-enter
edit mode). Pressing Escape in any input field in Edit Mode triggers the same Cancel behaviour.

---

## Flow 4 — Error Path: Invalid Phone Number (AC4)

```
[ProfileEditForm — edit mode]
          │
User types phone number without + prefix (e.g. "07911123456")
User tabs out of phone field (blur event)
          │
          ▼
[Client-side E.164 validation fires]
  Phone field: red border + ! icon
  Inline error: "Enter your number starting with + and country
                 code, e.g. +447911123456"  (role="alert")
  [ Save changes ] remains visible but form has errors → submit
  is blocked (or if submitted, error persists)
          │
User corrects phone → "+447911123456"
User blurs field again
          │
          ▼
[Validation passes]
  Red border removed, error cleared
  User can now click [ Save changes ] → Flow 2 continues
```

**Variant — Server-side 400 (AC4):**
```
PATCH /api/v1/profile → 400
  Response: { field: "phoneNumber", message: "Field format invalid" }
          │
          ▼
[Edit Mode preserved]
  Phone field: red border + server error message displayed
  "Enter your number starting with + and country code,
   e.g. +447911123456"
  Other fields retain user-entered values
  User corrects and retries → Flow 2
```

**AC satisfied:** AC4 (E.164 validation, 400 response handled gracefully)

---

## Flow 5 — Error Path: Network / Server Failure

```
PATCH /api/v1/profile → 5xx or no response
          │
          ▼
[Edit Mode preserved]
  FormErrorBanner appears at top of form:
  "We couldn't save your changes. Please try again."
  [ Try again ] → re-fires PATCH with same values
  [ Cancel ] still visible
  All user-entered data retained (not reset)
  Spinner gone, "Save changes" button re-enabled
```

---

## Flow 6 — Error Path: Session Expired During Save (AC6)

```
PATCH /api/v1/profile → 401
          │
          ▼
[Global Axios interceptor (US-003) fires]
  Redirect → /login
  Session-expiry message shown on login page:
  "Your session has expired. Please sign in to continue."
```

**AC satisfied:** AC6 (unauthenticated/expired requests return 401, handled globally)

---

## Flow 7 — Read-Only Field Interaction (AC3)

```
[ProfileView OR ProfileEditForm]
          │
User hovers or focuses the 🔒 icon next to Email or Date of birth
          │
          ▼
[Tooltip appears]
  "Email address and date of birth cannot be changed online.
   Contact us if you need to update either field."
  (role="tooltip", aria-describedby on the field label)
          │
User moves focus away → tooltip dismisses
```

**No error is ever shown** — the lock icon + tooltip is purely informational.
Email and DOB are never rendered as `<input>` elements at any point (AC3 prevention at render level).

**Variant — attempted PATCH with email or DOB in payload:**
This is a developer/integration concern only. The UI never sends these fields. The server's
400 "Field is not editable" response (AC3) would appear as a `FormErrorBanner` if it somehow
reached the client — but the UI design makes this unreachable by the end user.

---

## Flow 8 — Loading Error (GET fails)

```
GET /api/v1/profile → 5xx or network error (not 401)
          │
          ▼
[ProfilePage shows error state]
  Skeleton replaced by:
  ⚠ "We couldn't load your profile. Please try again."
  [ Try again ] → re-fires GET
  No edit mode available until data is loaded
```

---

## Decision Points Summary

| Decision Point | Paths |
|----------------|-------|
| GET /api/v1/profile response | 200 → View Mode · 401 → /login · 5xx → load error state |
| User clicks "Edit profile" | Edit Mode activated |
| Phone field on blur (edit mode) | Valid → no feedback · Invalid → inline error |
| User clicks "Save changes" | PATCH fired → 200 success · 400 phone error · 5xx error banner · 401 → /login |
| User clicks "Cancel" or presses Escape | Edit Mode cancelled → View Mode restored |
| User hovers/focuses lock icon | Tooltip shown; no action possible |

---

## Navigation Context

```
[/login (US-003)]
     │ authenticated
     ▼
[/dashboard] ← avatar menu → "My profile" → [/profile ← YOU ARE HERE]
                                              │
                                              ▼ (edit mode on same page)
                                              │ save or cancel
                                              ▼
                                          [/profile — view mode updated]
```

The profile page is a **dead-end page** in the best sense: the user completes their task and
either stays to view their data or navigates back via the top nav. There is no "next step"
after saving a profile — it is a self-contained task.
