// Story: US-004
import { useState } from 'react';
import { Link } from 'react-router-dom';

import { FORGOT_PASSWORD_EMAIL_KEY } from '../types/auth.types';
import { useForgotPassword } from '../hooks/useForgotPassword';
import FormErrorBanner from '@/shared/components/FormErrorBanner';

/**
 * ForgotPasswordSentPage — /forgot-password/sent
 *
 * Screen 2 of the Password Reset flow (US-004).
 *
 * Anti-enumeration confirmation:
 *   "If that email is registered, we've sent a link to reset your password."
 *
 * - Shows the email submitted in the previous step (read from sessionStorage).
 * - "Resend email" button re-submits the same email via useForgotPassword.
 *   On success the hook navigates to /forgot-password/sent (same page) — the
 *   component stays mounted; `isSuccess` is used to show a brief confirmation.
 * - "Use a different email" → /forgot-password
 * - "Back to sign in" → /login
 */
export default function ForgotPasswordSentPage() {
  // Read email once from sessionStorage (stable for the page's lifetime).
  const [email] = useState<string>(
    () => sessionStorage.getItem(FORGOT_PASSWORD_EMAIL_KEY) ?? '',
  );

  const { submit, isPending, isSuccess, bannerError, reset: resetMutation } =
    useForgotPassword();

  const handleResend = () => {
    if (!email) return;
    // Clear previous mutation state so isSuccess resets before the new call.
    resetMutation();
    submit({ email });
  };

  return (
    <main
      className="flex min-h-screen items-start justify-center bg-gray-50 px-4 py-12 sm:items-center"
      aria-labelledby="sent-heading"
    >
      <div className="w-full max-w-[440px] rounded-xl bg-white p-6 shadow-md sm:p-8">
        {/* ── Brand header ────────────────────────────────────────────────── */}
        <BrandHeader />

        <div className="flex flex-col items-center gap-5 py-2 text-center">
          {/* ── Email icon ────────────────────────────────────────────────── */}
          <div
            aria-hidden="true"
            className="flex h-16 w-16 items-center justify-center rounded-full bg-brand-100 text-brand-600"
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
                d="M21.75 6.75v10.5a2.25 2.25 0 0 1-2.25 2.25h-15a2.25 2.25 0 0 1-2.25-2.25V6.75m19.5 0A2.25 2.25 0 0 0 19.5 4.5h-15a2.25 2.25 0 0 0-2.25 2.25m19.5 0v.243a2.25 2.25 0 0 1-1.07 1.916l-7.5 4.615a2.25 2.25 0 0 1-2.36 0L3.32 8.91a2.25 2.25 0 0 1-1.07-1.916V6.75"
              />
            </svg>
          </div>

          {/* ── Heading + message ────────────────────────────────────────── */}
          <div className="flex flex-col gap-2">
            <h1
              id="sent-heading"
              className="text-2xl font-bold text-gray-900"
            >
              Check your email
            </h1>
            <p className="text-sm text-gray-600">
              If that email is registered, we&rsquo;ve sent a link to reset
              your password.
            </p>
            {email && (
              <p className="text-sm font-medium text-gray-700">{email}</p>
            )}
          </div>

          {/* ── Resend success confirmation ──────────────────────────────── */}
          {isSuccess && (
            <p
              role="status"
              aria-live="polite"
              className="flex items-center gap-1.5 text-sm font-medium text-green-700"
            >
              <svg
                aria-hidden="true"
                className="h-4 w-4 flex-shrink-0"
                viewBox="0 0 20 20"
                fill="currentColor"
              >
                <path
                  fillRule="evenodd"
                  d="M16.704 4.153a.75.75 0 0 1 .143 1.052l-8 10.5a.75.75 0 0 1-1.127.075l-4.5-4.5a.75.75 0 0 1 1.06-1.06l3.894 3.893 7.48-9.817a.75.75 0 0 1 1.05-.143Z"
                  clipRule="evenodd"
                />
              </svg>
              Email sent again.
            </p>
          )}

          {/* ── Error banner (unexpected errors only) ───────────────────── */}
          {bannerError !== null && (
            <div className="w-full">
              <FormErrorBanner message={bannerError} />
            </div>
          )}

          {/* ── Resend email button ──────────────────────────────────────── */}
          <button
            type="button"
            onClick={handleResend}
            disabled={isPending || !email}
            aria-disabled={isPending || !email}
            aria-label={isPending ? 'Resending reset email…' : 'Resend reset email'}
            className={[
              'flex w-full items-center justify-center gap-2 rounded-md px-4 py-2.5',
              'border border-gray-300 bg-white text-sm font-semibold text-gray-700 shadow-sm',
              'transition hover:bg-gray-50 focus:outline-none focus:ring-2',
              'focus:ring-brand-500 focus:ring-offset-2',
              'disabled:cursor-not-allowed disabled:opacity-60',
            ].join(' ')}
          >
            {isPending && (
              <svg
                aria-hidden="true"
                className="h-4 w-4 animate-spin"
                fill="none"
                viewBox="0 0 24 24"
              >
                <circle
                  className="opacity-25"
                  cx="12"
                  cy="12"
                  r="10"
                  stroke="currentColor"
                  strokeWidth="4"
                />
                <path
                  className="opacity-75"
                  fill="currentColor"
                  d="M4 12a8 8 0 0 1 8-8V0C5.373 0 0 5.373 0 12h4Z"
                />
              </svg>
            )}
            {isPending ? 'Resending…' : 'Resend email'}
          </button>

          {/* ── Secondary links ──────────────────────────────────────────── */}
          <div className="flex flex-col items-center gap-2">
            <Link
              to="/forgot-password"
              aria-label="Use a different email address"
              className="text-sm font-medium text-brand-600 hover:text-brand-700 hover:underline focus:outline-none focus-visible:ring-2 focus-visible:ring-brand-500 rounded"
            >
              Use a different email
            </Link>

            <Link
              to="/login"
              aria-label="Back to sign in page"
              className="text-xs text-gray-400 hover:text-gray-600 hover:underline focus:outline-none focus-visible:ring-2 focus-visible:ring-brand-500 rounded"
            >
              ← Back to sign in
            </Link>
          </div>
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
