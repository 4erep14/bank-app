// Story: US-002
import { Link } from 'react-router-dom';

/**
 * LockedAccountNotice
 *
 * Shown in place of the login form when the backend returns 423 (AC4 / AC5).
 * The email/password inputs are intentionally absent so a locked user cannot
 * keep hammering a locked account endpoint.
 *
 * Wireframe: state 6 — "Your account is locked"
 *
 * AC4: Displayed when the 5th consecutive failure triggers account lockout.
 * AC5: Displayed when an already-locked account attempts to sign in.
 */

interface LockedAccountNoticeProps {
  /**
   * Human-readable locked reason extracted from the RFC 7807 ProblemDetail
   * `detail` field (AC4/AC5). Displayed as a subtitle beneath the heading.
   */
  lockedMessage: string;
  /** Called when the user clicks "Back to sign in"; resets mutation state. */
  onBackToSignIn: () => void;
}

export default function LockedAccountNotice({
  lockedMessage,
  onBackToSignIn,
}: LockedAccountNoticeProps) {
  return (
    /*
     * role="alert" ensures screen readers announce this section immediately
     * when it mounts (replaces the form — a significant status change).
     */
    <section
      role="alert"
      aria-live="assertive"
      aria-labelledby="locked-heading"
      className="flex flex-col items-center gap-6 py-2 text-center"
    >
      {/* ── Lock icon (decorative) ─────────────────────────────────────── */}
      <div
        aria-hidden="true"
        className="flex h-16 w-16 items-center justify-center rounded-full bg-red-100 text-red-600"
      >
        <svg
          className="h-8 w-8"
          fill="none"
          stroke="currentColor"
          strokeWidth={1.75}
          viewBox="0 0 24 24"
          aria-hidden="true"
        >
          <path
            strokeLinecap="round"
            strokeLinejoin="round"
            d="M16.5 10.5V6.75a4.5 4.5 0 1 0-9 0v3.75m-.75 11.25h10.5a2.25 2.25 0 0 0 2.25-2.25v-6.75a2.25 2.25 0 0 0-2.25-2.25H6.75a2.25 2.25 0 0 0-2.25 2.25v6.75a2.25 2.25 0 0 0 2.25 2.25Z"
          />
        </svg>
      </div>

      {/* ── Heading + explanation ──────────────────────────────────────── */}
      <div className="flex flex-col gap-2">
        <h2
          id="locked-heading"
          className="text-xl font-bold text-gray-900"
        >
          Your account is locked
        </h2>

        {/* Server-supplied reason (from RFC 7807 `detail`) */}
        <p className="text-sm text-gray-600">{lockedMessage}</p>

        <p className="text-sm text-gray-500">
          To unlock your account, please contact NorthBank support. Our team
          will verify your identity and restore access.
        </p>
      </div>

      {/* ── Primary action ────────────────────────────────────────────── */}
      <a
        href="mailto:support@northbank.example"
        aria-label="Contact NorthBank support to unlock your account"
        className={[
          'flex w-full items-center justify-center gap-2 rounded-md px-4 py-2.5',
          'bg-brand-600 text-sm font-semibold text-white shadow-sm',
          'transition hover:bg-brand-700 focus:outline-none focus:ring-2',
          'focus:ring-brand-500 focus:ring-offset-2',
        ].join(' ')}
      >
        Contact support
      </a>

      {/* ── Secondary links ───────────────────────────────────────────── */}
      <div className="flex items-center gap-4 text-sm">
        <Link
          to="/forgot-password"
          aria-label="Go to forgot password page"
          className="text-brand-600 hover:text-brand-700 hover:underline focus:outline-none focus-visible:ring-2 focus-visible:ring-brand-500 rounded"
        >
          Forgot password?
        </Link>

        <span aria-hidden="true" className="text-gray-300">
          ·
        </span>

        <button
          type="button"
          onClick={onBackToSignIn}
          aria-label="Back to sign in form"
          className="text-brand-600 hover:text-brand-700 hover:underline focus:outline-none focus-visible:ring-2 focus-visible:ring-brand-500 rounded"
        >
          Back to sign in
        </button>
      </div>
    </section>
  );
}
