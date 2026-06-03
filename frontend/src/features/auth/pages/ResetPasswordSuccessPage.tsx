// Story: US-004
import { Link } from 'react-router-dom';

/**
 * ResetPasswordSuccessPage — /reset-password/success
 *
 * Screen 4 of the Password Reset flow (US-004).
 *
 * AC4 / AC5: Shown after a successful POST /api/v1/auth/reset-password.
 * Confirms the password was updated and informs the user that all active
 * sessions have been invalidated (AC5).
 */
export default function ResetPasswordSuccessPage() {
  return (
    <main
      className="flex min-h-screen items-start justify-center bg-gray-50 px-4 py-12 sm:items-center"
      aria-labelledby="reset-success-heading"
    >
      <div className="w-full max-w-[440px] rounded-xl bg-white p-6 shadow-md sm:p-8">
        {/* ── Brand header ────────────────────────────────────────────────── */}
        <BrandHeader />

        <div className="flex flex-col items-center gap-5 py-2 text-center">
          {/* ── Success icon ────────────────────────────────────────────── */}
          <div
            aria-hidden="true"
            className="flex h-16 w-16 items-center justify-center rounded-full bg-green-100 text-green-600"
          >
            <svg
              className="h-8 w-8"
              fill="none"
              stroke="currentColor"
              strokeWidth={2}
              viewBox="0 0 24 24"
              aria-hidden="true"
            >
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                d="M9 12.75 11.25 15 15 9.75M21 12a9 9 0 1 1-18 0 9 9 0 0 1 18 0Z"
              />
            </svg>
          </div>

          {/* ── Heading + explanation ────────────────────────────────────── */}
          <div className="flex flex-col gap-2">
            <h1
              id="reset-success-heading"
              className="text-2xl font-bold text-gray-900"
            >
              Your password has been reset
            </h1>
            {/* AC5: inform the user their sessions were invalidated */}
            <p className="text-sm text-gray-600">
              For your security, we&rsquo;ve signed you out of all devices.
            </p>
          </div>

          {/* ── Continue to sign in ─────────────────────────────────────── */}
          <Link
            to="/login"
            aria-label="Continue to sign in"
            className={[
              'flex w-full items-center justify-center gap-2 rounded-md px-4 py-2.5',
              'bg-brand-600 text-sm font-semibold text-white shadow-sm',
              'transition hover:bg-brand-700 focus:outline-none focus:ring-2',
              'focus:ring-brand-500 focus:ring-offset-2',
            ].join(' ')}
          >
            Continue to sign in
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
