// Story: US-003
/**
 * OtpVerificationPage — /verify-otp
 *
 * ⚠️  WIREFRAME / API MISMATCH (flagged for Architect review):
 *     The Product Designer's wireframe shows a masked phone number hint
 *     ("We sent a code to your phone number ending in **89"). However, the
 *     POST /api/v1/auth/login response contract (US-002) only returns
 *     { status, sessionToken } — no `maskedPhone` field. Until the backend
 *     exposes a `maskedPhone` in the login response (or a separate session
 *     info endpoint), the page falls back to a generic hint message.
 *
 * Acceptance Criteria implemented:
 *  AC1 — Guard: no sessionToken → redirect to /login; generic phone hint shown.
 *  AC2 — 401 "Invalid or expired OTP" → inline error banner.
 *  AC3 — 200 → tokens stored → navigate to / (dashboard placeholder).
 *  AC4 — 401 remainingAttempts > 0 → inline error + attempts warning.
 *  AC5 — 401 remainingAttempts === 0 → session-invalidated screen.
 */

import { useState, useEffect, useCallback } from 'react';
import { Navigate, Link, useNavigate } from 'react-router-dom';

import { SESSION_TOKEN_KEY } from '../types/auth.types';
import { useVerifyOtp } from '../hooks/useVerifyOtp';
import { useResendOtp } from '../hooks/useResendOtp';
import OtpInput from '../components/OtpInput';
import FormErrorBanner from '@/shared/components/FormErrorBanner';

// ── Constants ─────────────────────────────────────────────────────────────────

const RESEND_COUNTDOWN_SECONDS = 60;
/** Warn user when attempts are fewer than this threshold */
const ATTEMPTS_WARNING_THRESHOLD = 3;

// ── Guard wrapper ─────────────────────────────────────────────────────────────

/**
 * OtpVerificationPage
 *
 * Outer guard component — reads sessionToken exactly once (on mount via
 * useState lazy initializer) so a later sessionStorage.removeItem() triggered
 * by the AC5 error handler does NOT immediately redirect before the user sees
 * the "Too many attempts" screen.
 */
export default function OtpVerificationPage() {
  // Lazy initializer: reads sessionStorage once; stable for this page lifecycle.
  const [sessionToken] = useState<string | null>(
    () => sessionStorage.getItem(SESSION_TOKEN_KEY),
  );

  // AC1: no session token → redirect to /login immediately
  if (!sessionToken) {
    return <Navigate to="/login" replace />;
  }

  return <OtpVerificationContent />;
}

// ── Content ───────────────────────────────────────────────────────────────────

/**
 * OtpVerificationContent
 *
 * Rendered only when a valid sessionToken exists.
 * Contains all hooks and UI for the OTP verification flow.
 */
function OtpVerificationContent() {
  const navigate = useNavigate();

  // ── Mutation hooks ──────────────────────────────────────────────────────────

  const {
    verify,
    isPending: isVerifyPending,
    errorMessage,
    remainingAttempts,
    isSessionInvalidated,
    sessionInvalidatedMessage,
    reset: resetVerify,
  } = useVerifyOtp();

  const {
    resend,
    isPending: isResendPending,
    isSuccess: isResendSuccess,
    errorMessage: resendErrorMessage,
    reset: resetResend,
  } = useResendOtp();

  // ── Local state ─────────────────────────────────────────────────────────────

  const [otp, setOtp] = useState('');
  /** Key incremented to remount OtpInput (clears boxes) after a new code is sent. */
  const [otpInputKey, setOtpInputKey] = useState(0);
  const [countdown, setCountdown] = useState(RESEND_COUNTDOWN_SECONDS);

  // ── Countdown timer ─────────────────────────────────────────────────────────

  useEffect(() => {
    if (countdown <= 0) return;
    const id = setTimeout(() => setCountdown((c) => c - 1), 1_000);
    return () => clearTimeout(id);
  }, [countdown]);

  // ── Resend success: reset countdown + clear input ──────────────────────────

  useEffect(() => {
    if (!isResendSuccess) return;
    setCountdown(RESEND_COUNTDOWN_SECONDS);
    setOtp('');
    setOtpInputKey((k) => k + 1); // remount OtpInput — clear old digits
    resetResend();
    resetVerify(); // clear any lingering verify error
  }, [isResendSuccess, resetResend, resetVerify]);

  // ── Auto-submit with 300 ms debounce when all 6 digits are entered ─────────

  useEffect(() => {
    if (otp.length !== 6) return;
    const id = setTimeout(() => verify(otp), 300);
    return () => clearTimeout(id);
    // `verify` is React Query's stable mutate reference
  }, [otp, verify]);

  // ── OTP change handler ──────────────────────────────────────────────────────

  const handleOtpChange = useCallback(
    (value: string) => {
      setOtp(value);
      // Clear any existing verify error so the banner disappears as the user edits
      resetVerify();
    },
    [resetVerify],
  );

  // ── Manual submit (button) ──────────────────────────────────────────────────

  const handleManualSubmit = () => {
    if (otp.length === 6 && !isVerifyPending) {
      verify(otp);
    }
  };

  const isLoading = isVerifyPending;

  // ── AC5: Session-invalidated screen ────────────────────────────────────────

  if (isSessionInvalidated) {
    return (
      <main
        className="flex min-h-screen items-start justify-center bg-gray-50 px-4 py-12 sm:items-center"
        aria-labelledby="session-invalidated-heading"
      >
        <div className="w-full max-w-[440px] rounded-xl bg-white p-6 shadow-md sm:p-8">
          <BrandHeader />

          <section
            role="alert"
            aria-live="assertive"
            aria-labelledby="session-invalidated-heading"
            className="flex flex-col items-center gap-5 py-2 text-center"
          >
            {/* Shield / warning icon */}
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
                  d="M12 9v3.75m-9.303 3.376c-.866 1.5.217 3.374 1.948 3.374h14.71c1.73 0 2.813-1.874 1.948-3.374L13.949 3.378c-.866-1.5-3.032-1.5-3.898 0L2.697 16.126ZM12 15.75h.007v.008H12v-.008Z"
                />
              </svg>
            </div>

            <div className="flex flex-col gap-2">
              <h2
                id="session-invalidated-heading"
                className="text-xl font-bold text-gray-900"
              >
                Session expired
              </h2>
              <p className="text-sm text-gray-600">{sessionInvalidatedMessage}</p>
              <p className="text-sm text-gray-500">
                For your security, your verification session has ended. Please
                sign in again to receive a new code.
              </p>
            </div>

            <button
              type="button"
              onClick={() => navigate('/login', { replace: true })}
              className={[
                'flex w-full items-center justify-center gap-2 rounded-md px-4 py-2.5',
                'bg-brand-600 text-sm font-semibold text-white shadow-sm',
                'transition hover:bg-brand-700 focus:outline-none focus:ring-2',
                'focus:ring-brand-500 focus:ring-offset-2',
              ].join(' ')}
            >
              Sign in again
            </button>
          </section>
        </div>
      </main>
    );
  }

  // ── Normal OTP form ─────────────────────────────────────────────────────────

  return (
    <main
      className="flex min-h-screen items-start justify-center bg-gray-50 px-4 py-12 sm:items-center"
      aria-labelledby="otp-heading"
    >
      <div className="w-full max-w-[440px] rounded-xl bg-white p-6 shadow-md sm:p-8">
        {/* ── Brand header ────────────────────────────────────────────────── */}
        <BrandHeader />

        {/* ── Page heading ────────────────────────────────────────────────── */}
        <div className="mb-6 text-center">
          <h1
            id="otp-heading"
            className="text-2xl font-bold text-gray-900"
          >
            Verify your identity
          </h1>
          {/*
           * WIREFRAME / API MISMATCH NOTE:
           * The wireframe shows a masked phone number ("ending in **89").
           * LoginResponse does not include maskedPhone. Using generic message.
           * Fix: add maskedPhone to LoginResponse and store it in sessionStorage.
           */}
          <p className="mt-1 text-sm text-gray-500">
            We sent a 6-digit code to your registered phone number.
          </p>
        </div>

        {/* ── Verify error banner (AC2 / AC4 / unexpected) ────────────────── */}
        {errorMessage !== null && (
          <div className="mb-4">
            <FormErrorBanner message={errorMessage} />
          </div>
        )}

        {/* ── Resend error banner ──────────────────────────────────────────── */}
        {resendErrorMessage !== null && (
          <div className="mb-4">
            <FormErrorBanner message={resendErrorMessage} />
          </div>
        )}

        {/* ── OTP input group ─────────────────────────────────────────────── */}
        <div
          aria-label="Enter your 6-digit verification code"
          className="mb-4"
        >
          <OtpInput
            key={otpInputKey}
            onChange={handleOtpChange}
            disabled={isLoading}
            hasError={errorMessage !== null}
          />
        </div>

        {/* ── Remaining attempts warning (AC4: show when < threshold) ─────── */}
        {remainingAttempts !== null &&
          remainingAttempts > 0 &&
          remainingAttempts < ATTEMPTS_WARNING_THRESHOLD && (
            <p
              role="alert"
              aria-live="polite"
              className="mb-4 text-center text-sm font-medium text-amber-600"
            >
              <span aria-hidden="true">⚠ </span>
              {remainingAttempts === 1
                ? '1 attempt remaining before your session is locked.'
                : `${remainingAttempts} attempts remaining.`}
            </p>
          )}

        {/* ── Primary action: Verify ───────────────────────────────────────── */}
        <button
          type="button"
          onClick={handleManualSubmit}
          disabled={isLoading || otp.length !== 6}
          aria-disabled={isLoading || otp.length !== 6}
          aria-label={isLoading ? 'Verifying…' : 'Verify code'}
          className={[
            'flex w-full items-center justify-center gap-2 rounded-md px-4 py-2.5',
            'bg-brand-600 text-sm font-semibold text-white shadow-sm',
            'transition hover:bg-brand-700 focus:outline-none focus:ring-2',
            'focus:ring-brand-500 focus:ring-offset-2',
            'disabled:cursor-not-allowed disabled:opacity-60',
          ].join(' ')}
        >
          {isLoading && (
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
          {isLoading ? 'Verifying…' : 'Verify code'}
        </button>

        {/* ── Resend section ───────────────────────────────────────────────── */}
        <div className="mt-5 flex flex-col items-center gap-2">
          <p className="text-sm text-gray-500">Didn&rsquo;t receive a code?</p>

          {countdown > 0 ? (
            /* Countdown — button is disabled */
            <p
              aria-live="polite"
              aria-atomic="true"
              className="text-sm text-gray-400"
            >
              Resend code in{' '}
              <span className="font-medium tabular-nums text-gray-600">
                {countdown}s
              </span>
            </p>
          ) : (
            /* Countdown reached 0 — enable resend */
            <button
              type="button"
              onClick={() => resend()}
              disabled={isResendPending}
              aria-disabled={isResendPending}
              aria-label={isResendPending ? 'Sending a new code…' : 'Resend verification code'}
              className={[
                'text-sm font-medium text-brand-600',
                'hover:text-brand-700 hover:underline',
                'focus:outline-none focus-visible:ring-2 focus-visible:ring-brand-500 rounded',
                'disabled:cursor-not-allowed disabled:opacity-60',
              ].join(' ')}
            >
              {isResendPending ? 'Sending…' : 'Resend code'}
            </button>
          )}
        </div>

        {/* ── Back to sign in ──────────────────────────────────────────────── */}
        <div className="mt-4 text-center">
          <Link
            to="/login"
            aria-label="Back to sign in page"
            className={[
              'text-xs text-gray-400 hover:text-gray-600 hover:underline',
              'focus:outline-none focus-visible:ring-2 focus-visible:ring-brand-500 rounded',
            ].join(' ')}
          >
            ← Back to sign in
          </Link>
        </div>
      </div>
    </main>
  );
}

// ── Shared sub-component ──────────────────────────────────────────────────────

/**
 * BrandHeader — NorthBank logo + name, shared between page states.
 * Mirrors the header used in LoginPage and RegistrationPage for visual consistency.
 */
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
