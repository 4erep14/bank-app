# UX Rationale — US-005: View & Update Customer Profile

> NorthBank · Authenticated session · Trust-sensitive · Returning user
> Satisfies: AC1, AC2, AC3, AC4, AC5, AC6

---

## User & Job-to-be-Done

- **Primary persona:** *Alex, the Existing NorthBank Customer* — an authenticated, **returning user** who
  has already registered (US-001) and logged in (US-003). Moderately comfortable with banking apps.
  Visits this page infrequently (once every few months at most), almost always with a specific,
  small task in mind: verify contact details are current, or update a phone number after changing SIM.
- **JTBD:** *When* my contact details change or I want to confirm my information is accurate,
  *I want to* see my current profile details and update the ones I am allowed to change,
  *so that* my bank always holds my correct information and I continue to receive important account
  communications (OTPs, statements, alerts) without interruption.
- **Context of use:**
  - **Device:** Mix of mobile (~55%) and desktop (~45%); desktop is slightly more common for
    deliberate "admin" tasks like profile management.
  - **Urgency:** Low-to-moderate — the user has a targeted task and wants fast confirmation of
    success, but is not in crisis.
  - **Frequency:** Infrequent. The page must be easily *discoverable* via the user avatar menu; the
    experience must be quick and reassuring once found.
  - **Emotional state:** Calm, purposeful. Trust is already established (authenticated session).
    Still expects a professional, secure experience consistent with a financial product.

---

## Mental Model & Information Architecture

- **Vocabulary:** "Profile" / "My profile" — not "User entity" or "Customer record".
  Field labels: "First name", "Last name", "Email address", "Phone number", "Date of birth" —
  human terms, not database columns or API field names (`firstName`, `phoneNumber`, etc.).
- **Where it lives:** Authenticated route `/profile`. Accessible via the top-navigation user
  avatar menu → "My profile". The user avatar menu is the established IA location for personal
  settings across all major banking and consumer apps — matches the user's existing mental model.
- **Information priority:**
  - **Primary:** Current profile data (all 5 fields at a glance) and the "Edit profile" CTA.
  - **Secondary:** Visual distinction between editable and read-only fields — set *before* the
    user even enters Edit Mode, so they arrive with correct expectations.
  - **Tertiary:** Why certain fields are locked (brief explanation on tooltip / on-focus of lock
    icon), and how to get help if needed.
- **View then Edit pattern:** Users expect to *see* their data before editing it. Launching
  directly into an edit form would be disorienting ("What was it before?"). Showing the data
  first lets them confirm whether a change is actually needed, then act with intent.
- **Introduced: Authenticated App Shell.** US-005 is the first post-login page design.
  It establishes the authenticated layout: persistent top navigation bar (logo, primary nav
  links, user avatar menu) used on all subsequent authenticated pages. No left sidebar is
  introduced for profile — it is a single-section page reached via the user menu, not a
  product navigation item.

---

## Key Decisions

| Decision | Why (user benefit) | Alternative rejected |
|----------|--------------------|----------------------|
| **"View then Edit" on a single page** (two view-modes, no navigation) | Fewest steps: see current data → click Edit → change fields → Save → see updated data. No context switching, no extra page load. | Separate `/profile/edit` route — redundant navigation for ≤3 editable fields. Full-page modal — hides read-only context. |
| **Read-only fields rendered as static text in BOTH modes** (email, DOB — AC3) | Prevents any accidental interaction. The user can never accidentally click into them. The lock icon sets expectation *before* they try. | `disabled` `<input>` — still implies potential editability, creates visual noise, and is announced ambiguously by some screen readers. |
| **Lock icon (🔒) with descriptive tooltip on read-only fields** | Converts potential frustration into understanding: "This field can't be changed. Contact us if you need help." — the user leaves knowing *why*, not confused. | Hiding explanation entirely — prompts unnecessary support calls. Error message on click — reactive rather than preventive. |
| **Inline E.164 phone validation on blur** (client-side, before PATCH — AC4) | Catches format errors near the field, in context, immediately actionable with a concrete example. Prevents a wasted API round-trip for ~90% of invalid entries. | Submit-only validation — punishes the user after completing all fields. |
| **Success: toast + automatic return to View Mode** | Quick, non-disruptive confirmation. Returning to View Mode *shows the updated data* — closes the loop visually ("I can see my new number is saved"). Satisfies AC5 without requiring a page reload. | Dedicated success screen — disproportionate to a 3-field update. Full-page success banner — intrusive, slow. |
| **Cancel always visible in Edit Mode** | User safety and freedom — they can always abandon without saving. Critical on mobile where the back gesture may trigger unintended navigation. | No Cancel — traps the user; anti-pattern for editing flows. |
| **Pre-filled editable inputs** from the GET /api/v1/profile response (AC1) | The user only changes what needs changing; no re-typing of unchanged values. Reduces effort and input errors. | Empty form — forces re-entry of all fields; annoying and error-prone. |
| **One primary action per mode** | View mode: "Edit profile". Edit mode: "Save changes" (primary) + "Cancel" (secondary text). Clear visual hierarchy at all times. | Two equal-weight buttons in view mode — splits attention, raises accidental action risk. |
| **Authenticated App Shell top navigation** (new component: `AppTopNav`) | Establishes the authenticated layout pattern for all post-login pages. User avatar menu provides discoverable access to profile, settings, and sign-out. | Separate header per page — inconsistency; no persistent user context. No app shell — each page reinvents navigation. |

---

## Friction Removed / Cognitive Load

- **Pre-populated form** from AC1 GET — user only touches what changes.
- **Only 3 editable fields** — the shortest possible form; no chunking or pagination needed.
- **Autocomplete attributes** on name (`given-name`, `family-name`) and phone (`tel`) fields so
  browser/password managers can assist on mobile.
- **No confirmation step** — a 3-field, non-destructive, reversible update does not warrant a
  confirm dialog. Reversibility (re-edit immediately) replaces the need for confirmation.
- **Read-only fields skip tab order** in Edit Mode — keyboard users Tab directly through the
  3 editable fields without pausing on fields they cannot interact with.
- **Lock icon tooltip** answers the "why" passively — no extra screen, no support contact needed
  for the common case.
- **Persistent phone helper text** in Edit Mode eliminates the need to remember E.164 format.

---

## Trust, Feedback & Safety

| Signal | Mechanism |
|--------|-----------|
| Loading | Skeleton placeholders (not blank flash) while GET is in flight |
| Saving | "Save changes" button shows spinner + "Saving…" label; form is disabled |
| Success | `role="status"` toast: "Profile updated" — auto-dismissed after 4 s |
| Inline validation | Phone field validated on blur; error cleared as user corrects |
| Server 400 (phone) | Field-level error on phone input; Edit Mode preserved; data retained |
| Server 400 (read-only field attempted) | Prevented at UI level — never reachable by the user; server-side safety net only |
| Server 401 | Global Axios interceptor (established in US-003) redirects to `/login` with session-expiry message |
| Server 5xx / network | Non-blocking `FormErrorBanner` at top of Edit form: "We couldn't save your changes. Please try again." — data retained, Edit Mode preserved |

---

## Heuristic Check (Nielsen's 10)

| Heuristic | How the design satisfies it |
|-----------|------------------------------|
| **1. Visibility of system status** | Skeleton on initial load; spinner on save; success toast; inline field validation states; error banner for network failures. |
| **2. Match the real world** | Human field labels; "Phone number" not "E.164 MSISDN"; lock icon = universally understood "cannot change"; "Edit profile" / "Save changes" / "Cancel" — banking-standard vocabulary. |
| **3. User control & freedom** | "Cancel" always visible in Edit Mode; read-only fields blocked at render level; 401 redirects gracefully to login; no destructive or irreversible action on this page. |
| **4. Consistency & standards** | Reuses `TextField`, `PhoneField`, `SubmitButton`, `FormErrorBanner`, `TrustBadge` from US-001 / US-004. Same NorthBank card style, spacing, and button hierarchy. New `ReadOnlyField` and `AppTopNav` components follow established PascalCase naming and prop conventions. |
| **5. Error prevention** | Read-only fields blocked at render (never `<input>`); client-side E.164 validation on blur; form disabled during submission to prevent double-submit. |
| **6. Recognition over recall** | All current values pre-filled; lock icon + persistent tooltip explains read-only policy without the user needing to remember a rule; phone helper text always visible in Edit Mode. |
| **7. Flexibility & efficiency** | Keyboard: Tab skips read-only fields in Edit Mode; Enter submits; Escape cancels. Autocomplete support on name/phone. Power users can navigate directly to `/profile`. |
| **8. Aesthetic & minimalist design** | 5 fields, clean card, no decorative clutter; Edit Mode activates only the 3 mutable inputs; secondary actions (Cancel) visually subordinate to primary (Save). |
| **9. Help users recover from errors** | Phone error message specifies exactly how to fix ("Start with + then country code and number, e.g. +447911123456"). Network/server error has a "Try again" affordance. 401 provides a path to sign in. |
| **10. Help & documentation** | Phone helper text always present in Edit Mode. Lock icon tooltip explains the read-only policy. No external docs needed for this page. |

### Deliberate trade-offs

- **No "confirm email" flow** — email is permanently read-only per AC3. If a future story
  enables email changes, it would require a separate identity-verification sub-flow (out of scope
  here). The lock icon + tooltip pre-empts user confusion without implying the feature will come.
- **No undo after Save** — the update is non-destructive (re-editable immediately), so a
  dedicated undo affordance adds complexity without meaningful safety benefit.
- **Lock icon tooltip, not an error** — preventing the interaction is better than responding to
  it. A screen-reader user tabbing through read-only fields will hear the field label and
  accessible description ("read-only, contact support to update") without triggering an error
  state — this is correct and intentional behaviour.
- **No sidebar navigation** on the profile page — profile is accessed via the user menu, not
  the product navigation. Adding a sidebar would impose navigation chrome for a single-page
  settings task. This decision should be revisited if a broader "Settings" section is added to
  a future Epic.
