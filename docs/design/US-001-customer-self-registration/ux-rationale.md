# UX Rationale — US-001: Customer Self-Registration

> Banking platform · Trust-sensitive · First-time, unauthenticated user
> Satisfies: AC1, AC2, AC3, AC4, AC5, AC6

---

## User & Job-to-be-Done

- **Primary persona:** *Maria, the New Prospective Customer* — a first-time visitor with no existing account, **novice** with this specific platform but generally familiar with online forms and banking apps. She may be on a phone during a commute or at home on a laptop. She is mildly **anxious** because she is about to share sensitive personal data (date of birth, phone, password) with a financial institution she is still evaluating for trustworthiness.
- **JTBD:** *When* I decide I want to start banking with this provider, *I want to* create an account using my personal details quickly and without confusion, *so that* I can get access to the banking platform and begin using it.
- **Context of use:**
  - **Device:** Mobile-first (assume ~60% mobile). Must also scale gracefully to tablet/desktop.
  - **Environment:** Public/semi-public; user may be interrupted; possibly on slow or intermittent networks.
  - **Urgency:** Moderate — motivated but low tolerance for friction or unexplained rejections.
  - **Frequency:** Once per customer (one-and-done). The empty/first-run state *is* the whole experience.
  - **Emotional state:** Cautious, trust-seeking. Every field that asks for sensitive data must feel justified and safe.

---

## Mental Model & Information Architecture

- **Vocabulary:** Use plain human terms — "First name", "Email address", "Mobile number", "Date of birth", "Password" — never database/system terms like `status: PENDING_VERIFICATION` or `E.164`. The E.164 rule is translated into a friendly example, not jargon (see microcopy).
- **Where it lives:** Public, unauthenticated route `/register`, reachable from the marketing site and from the Login screen ("New here? Create an account"). After success, the user is routed toward Login.
- **Information priority:**
  - **Primary:** The registration form and its single primary action ("Create account").
  - **Secondary:** Password requirements helper, phone format helper, link back to Login.
  - **Tertiary:** Legal/security reassurance (lock icon, "Your data is encrypted"), footer links.
- **Grouping (chunking):** Fields are grouped into a logical, scannable order — *Identity* (first/last name) → *Contact* (email, phone) → *Identity verification* (date of birth) → *Security* (password). This mirrors how a person thinks about "who I am, how to reach me, proof of identity, how I'll sign in."

---

## Key Decisions

| Decision | Why (user benefit) | Alternative rejected |
|----------|--------------------|----------------------|
| **Single-page form** (not a multi-step wizard) | Only 6 fields; one screen lets the user see the whole commitment up front and reduces context switching/abandonment. | Multi-step wizard — adds navigation overhead and hides the total effort; overkill for 6 fields. |
| **Inline, on-blur validation** (validate each field when the user leaves it; re-validate on change after first error) | Catches mistakes early, near the field, so the user fixes them in context instead of after a full-page rejection. Directly supports error-prevention (AC3, AC4). | Submit-only validation — forces a frustrating round-trip and makes banking feel unforgiving. |
| **Live password strength + checklist** | Turns the abstract complexity rule (AC3) into a visible, achievable checklist; the user sees requirements turn green as they type — recognition over recall. | Hidden rules revealed only on error — punishes the user for not guessing the policy. |
| **Phone with format example + country hint** | E.164 (AC4) is invisible jargon; a concrete example (`+447911123456`) lets the user self-correct without knowing the standard's name. | Raw "must match E.164" message — meaningless to a novice. |
| **Email duplicate handled inline on the email field** (AC2 → 409) | The 409 is mapped to a field-level message *on the email input* with a direct recovery path ("Sign in instead"), not a generic banner — the user knows exactly what to fix and what to do next. | Generic top-of-form error — hides which field is the problem and offers no recovery. |
| **One primary action per screen** ("Create account") | Clear visual hierarchy; no competing CTAs. The "Sign in" path is a secondary text link. | Equal-weight "Create account" + "Sign in" buttons — splits attention. |
| **Native date input with min/max constraints** for date of birth | Prevents impossible dates (future DOB, absurd ages) before submission; mobile shows a native date picker. | Free-text date — invites format errors and locale ambiguity. |
| **Success = dedicated confirmation screen** (not just a toast) | Registration is a milestone; a full screen confirms persistence (AC5/AC6) and tells the user the next step (check email / proceed to login). | Inline toast then silence — leaves the user unsure whether it worked. |
| **Trust signals near sensitive fields** (lock icon, encryption note) | Reassures an anxious first-time banking user that data is safe — reduces abandonment at the password/DOB fields. | No reassurance — higher drop-off on sensitive fields. |

---

## Friction Removed / Cognitive Load

- **Only what's required.** Exactly the 6 fields from AC1 — nothing speculative (no "confirm email", no marketing opt-ins inside the critical path). Note: a "confirm password" field was deliberately **not** added because the live show-password toggle plus the visible checklist already prevents typos without doubling input effort. *(Flagged as a negotiable decision for the BA — see UX Flow note.)*
- **Sensible field order** matching the mental model (identity → contact → DOB → security) so the form reads top-to-bottom like a sentence about the user.
- **Show-password toggle** so the user can verify a complex password instead of retyping — reduces password-entry errors.
- **Live requirement checklist** means the user never has to remember the 4-part complexity rule (AC3); it's always visible.
- **Smart input types & attributes:** `type="email"` (email keyboard), `type="tel"` (numeric keypad) with `inputmode`, `autocomplete` tokens (`given-name`, `family-name`, `email`, `tel`, `bday`, `new-password`) so browsers/password managers can autofill — minimizing typing on mobile.
- **Inline error placement** keeps the fix where the eye already is; the first errored field receives focus after a failed submit so keyboard and screen-reader users land on the problem.
- **No dead-ends:** every error (duplicate email, validation, network failure) has a stated recovery action.

---

## Trust, Feedback & Safety

- **Visible system status:** the submit button shows a spinner + "Creating account…" while the request is in flight (AC6 round-trip); the whole form is disabled to prevent double submission.
- **Error prevention first:** input constraints (date min/max, email/tel types, live checklist) stop most invalid submissions before they reach the API (AC3, AC4).
- **Graceful server errors:** 409 (AC2) maps to the email field; 400 field-level errors (AC3/AC4) map to their respective fields; unexpected 5xx/network errors show a non-blocking form-level banner with a "Try again" affordance — the entered data is preserved.
- **No silent failures:** success is explicitly confirmed on its own screen referencing the next step.

---

## Heuristic Check (Nielsen's 10)

| Heuristic | How the design satisfies it |
|-----------|------------------------------|
| **1. Visibility of system status** | Loading spinner + "Creating account…" on submit; inline valid/error states per field; success screen confirms the result. |
| **2. Match the real world** | Human labels and a phone *example* instead of "E.164"; reassurance language; no DB terms like PENDING_VERIFICATION shown. |
| **3. User control & freedom** | "Sign in instead" recovery on duplicate email; back link to Login; data preserved on error; no irreversible trap. |
| **4. Consistency & standards** | Standard form patterns, standard date picker, one primary button, consistent error styling. |
| **5. Error prevention** | Inline on-blur validation, live password checklist, constrained date input, typed inputs (AC3, AC4). |
| **6. Recognition over recall** | Password rules shown as a persistent checklist; phone example always visible; labels never disappear (floating/persistent labels, not placeholder-only). |
| **7. Flexibility & efficiency** | Autocomplete/password-manager support, native pickers, keyboard-navigable, Enter-to-submit. |
| **8. Aesthetic & minimalist design** | Only the 6 required fields + essential helpers; no clutter; secondary actions de-emphasized. |
| **9. Help users recover from errors** | Every error message says *how to fix it* (e.g., add a special character), and maps to the offending field; 409 offers a path to sign in. |
| **10. Help & documentation** | Inline helper text under password and phone removes the need for external docs; security reassurance line. |

### Deliberate trade-offs
- **No "confirm password" field.** Trade-off accepted because the show-password toggle + live checklist mitigate typo risk while keeping the form short. If the BA/security policy requires confirmation, it slots in directly under Password with its own "Passwords don't match" inline error. **(Raised to BA.)**
- **No CAPTCHA / bot protection in this story.** Out of scope per the story; noted for the Architect as a likely follow-up so the visual layout reserves no space for it now.
- **Email verification not designed here** (explicitly out of scope). The success screen *mentions* "check your email" only as forward guidance; the actual verification flow is a separate story. The persisted `PENDING_VERIFICATION` status (AC5) is therefore not surfaced as a user-facing label.
