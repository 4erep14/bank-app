// Story: US-003
import { useMutation } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import { isAxiosError } from 'axios';
import { verifyOtp } from '../api/authApi';
import {
  SESSION_TOKEN_KEY,
  ACCESS_TOKEN_KEY,
  REFRESH_TOKEN_KEY,
} from '../types/auth.types';
import type { VerifyOtpResponse } from '../types/auth.types';

// ── Fallback error messages ───────────────────────────────────────────────────

const ERROR_INVALID_OTP = 'Invalid or expired OTP.';
const ERROR_TOO_MANY_ATTEMPTS = 'Too many attempts. Please sign in again.';
const ERROR_UNEXPECTED = 'An unexpected error occurred. Please try again.';

// ── Public interface ──────────────────────────────────────────────────────────

export interface UseVerifyOtpResult {
  /** Trigger the OTP verification mutation with the 6-digit code */
  verify: (otp: string) => void;
  /** True while the network request is in-flight */
  isPending: boolean;
  /**
   * Human-readable error message for AC2 (expired OTP) and AC4 (invalid OTP).
   * null when there is no error or when isSessionInvalidated is true.
   */
  errorMessage: string | null;
  /**
   * Number of remaining OTP attempts from the RFC 7807 error response.
   * null if the error did not include this field or there is no error.
   * AC4: shown as a warning when value is > 0 but < 3.
   */
  remainingAttempts: number | null;
  /**
   * AC5: true when the server indicates 0 remaining attempts (session invalidated).
   * The page should display the "Too many attempts" error state.
   */
  isSessionInvalidated: boolean;
  /**
   * The human-readable message to display on session invalidation (AC5).
   * Always set when isSessionInvalidated is true.
   */
  sessionInvalidatedMessage: string;
  /** Clear mutation state (e.g. when the user edits the OTP to retry) */
  reset: () => void;
}

// ── Helper: safely extract RFC 7807 OTP error fields ─────────────────────────

interface OtpErrorFields {
  detail: string | null;
  remainingAttempts: number | null;
}

function extractOtpErrorFields(data: unknown): OtpErrorFields {
  if (data !== null && typeof data === 'object') {
    const d = data as Record<string, unknown>;
    const detail =
      typeof d.detail === 'string' ? d.detail : null;
    const remainingAttempts =
      typeof d.remainingAttempts === 'number' ? d.remainingAttempts : null;
    return { detail, remainingAttempts };
  }
  return { detail: null, remainingAttempts: null };
}

// ── Hook ─────────────────────────────────────────────────────────────────────

/**
 * Wraps POST /api/v1/auth/verify-otp.
 *
 * AC2: on 401 with detail "Invalid or expired OTP" → surfaces errorMessage
 * AC3: on 200 → stores accessToken + refreshToken in localStorage,
 *              removes sessionToken from sessionStorage, navigates to /
 * AC4: on 401 with remainingAttempts > 0 → surfaces errorMessage + remainingAttempts
 * AC5: on 401 with remainingAttempts === 0 → sets isSessionInvalidated=true,
 *              clears sessionStorage (side-effect runs in onError)
 *
 * NOTE: The router currently has no /dashboard route. Navigation goes to "/" as a
 * temporary fallback; update to "/dashboard" once that route is implemented.
 */
export function useVerifyOtp(): UseVerifyOtpResult {
  const navigate = useNavigate();

  // Read sessionToken once — it is stable for the lifetime of this hook call
  // (cleared in onSuccess / onError side-effects, not between renders).
  const sessionToken = sessionStorage.getItem(SESSION_TOKEN_KEY) ?? '';

  const {
    mutate: rawMutate,
    isPending,
    error,
    reset,
  } = useMutation<VerifyOtpResponse, unknown, string>({
    mutationFn: (otp: string) => verifyOtp({ sessionToken, otp }),

    onSuccess: (data) => {
      // AC3: valid OTP → persist tokens → clear session → navigate to dashboard
      localStorage.setItem(ACCESS_TOKEN_KEY, data.accessToken);
      localStorage.setItem(REFRESH_TOKEN_KEY, data.refreshToken);
      sessionStorage.removeItem(SESSION_TOKEN_KEY);
      navigate('/dashboard');
    },

    onError: (err) => {
      if (isAxiosError(err) && err.response?.status === 401) {
        const { remainingAttempts: ra } = extractOtpErrorFields(err.response?.data);
        if (ra !== null && ra === 0) {
          // AC5: session invalidated — clear the short-lived session token
          sessionStorage.removeItem(SESSION_TOKEN_KEY);
        }
      }
    },
  });

  // ── Derive error state from mutation error ──────────────────────────────────
  let errorMessage: string | null = null;
  let remainingAttempts: number | null = null;
  let isSessionInvalidated = false;

  if (error !== null && error !== undefined) {
    if (isAxiosError(error) && error.response?.status === 401) {
      const { detail, remainingAttempts: ra } = extractOtpErrorFields(
        error.response?.data,
      );

      if (ra !== null && ra === 0) {
        // AC5: too many failures
        isSessionInvalidated = true;
      } else {
        // AC2 / AC4: show error detail + remaining attempts
        errorMessage = detail ?? ERROR_INVALID_OTP;
        remainingAttempts = ra;
      }
    } else {
      errorMessage = ERROR_UNEXPECTED;
    }
  }

  return {
    verify: rawMutate,
    isPending,
    errorMessage,
    remainingAttempts,
    isSessionInvalidated,
    sessionInvalidatedMessage: ERROR_TOO_MANY_ATTEMPTS,
    reset,
  };
}
