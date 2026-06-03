// Story: US-004
import { Link } from 'react-router-dom';

/**
 * ResetTokenErrorPage — /reset-password/error
 *
 * Screen 5 of the Password Reset flow (US-004).
 *
 * Shown when:
 *   a) The URL contains no `?token=` query parameter (rendered inline from
 *      ResetPasswordPage without navigation, so the URL stays /reset-password).
 *   b) The backend returns 400 { detail: "Invalid or expired reset token" }
 *      (AC6/AC7). In this case useResetPassword navigates here directly.
 *
 * The component is a standalone page so it can also be rendered at
 * /reset-password/error as a full route.
 */
export default function ResetTokenErrorPage() {
  return (
    <main
      className="flex min-h-screen items-start justify-center bg-gray-50 px-4 py-12 sm:items-center"
      aria-labelledby="token-error-heading"
    >
      <div className="w-full max-w-[440px] rounded-xl bg-white p-6 shadow-md sm:p-8">
        {/* ── Brand header ────────────────────────────────────────────────── */}
        <BrandHeader />

        <div className="flex flex-col items-center gap-5 py-2 text-center">
          {/* ── Warning icon ────────────────────────────────────────────── */}
          <div
            aria-hidden="true"
            className="flex h-16 w-16 items-center justify-center rounded-full bg-amber-100 text-amber-600"
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
                d="M12 9v3.75m-9.303 3.376c-.866 1.5.217 3.374 1.948 3.374h14.71c1.73 0 2.813-1.874 1.948-3.374L13.949 3.378c-.866-1.5-3.032-1.5-3.898 0L2.697 16.126ZM12 15.75h.007v.008H12v-.008Z"
              />
            </svg>
          </div>

          {/* ── Heading + explanation ────────────────────────────────────── */}
          <div className="flex flex-col gap-2">
            <h1
              id="token-error-heading"
              className="text-2xl font-bold text-gray-900"
            >
              This reset link has expired
            </h1>
            <p className="text-sm text-gray-600">
              Password reset links are single-use and expire 1 hour after
              they&rsquo;re sent.
            </p>
          </div>

          {/* ── Primary CTA ─────────────────────────────────────────────── */}
          <Link
            to="/forgot-password"
            aria-label="Request a new password reset link"
            className={[
              'flex w-full items-center justify-center gap-2 rounded-md px-4 py-2.5',
              'bg-brand-600 text-sm font-semibold text-white shadow-sm',
              'transition hover:bg-brand-700 focus:outline-none focus:ring-2',
              'focus:ring-brand-500 focus:ring-offset-2',
            ].join(' ')}
          >
            Request a new link
          </Link>

          {/* ── Secondary link ──────────────────────────────────────────── */}
          <Link
            to="/login"
            aria-label="Back to sign in page"
            className="text-xs text-gray-400 hover:text-gray-600 hover:underline focus:outline-none focus-visible:ring-2 focus-visible:ring-brand-500 rounded"
          >
            ← Back to sign in
          </Link>
        </div>
      </div>
    </main>
  );
}

// ── Shared sub-component ──────────────────────────────────────────────────────

function BrandHeader() {
  return (
    <div className="mb-6 flex flex-col items-center gap-2">
      <div
        aria-hidden="true"
        className="flex h-10 w-10 items-center justify-center rounded-full bg-brand-600"
      >
        <svg
          className="h-6 w-6 text-white"
          fill="none"
          stroke="currentColor"
          strokeWidth={2}
          viewBox="0 0 24 24"
          aria-hidden="true"
        >
          <path
            strokeLinecap="round"
            strokeLinejoin="round"
            d="M12 3L2 9h20L12 3ZM4 9v9m4-9v9m4-9v9m4-9v9M2 18h20"
          />
        </svg>
      </div>
      <span className="text-lg font-bold tracking-tight text-gray-900">
        NorthBank
      </span>
    </div>
  );
}
