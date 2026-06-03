// Story: US-003
import { useMutation } from '@tanstack/react-query';
import { isAxiosError } from 'axios';
import { resendOtp } from '../api/authApi';
import { SESSION_TOKEN_KEY } from '../types/auth.types';
import type { ResendOtpResponse } from '../types/auth.types';

// ── Public interface ──────────────────────────────────────────────────────────

export interface UseResendOtpResult {
  /** Trigger the resend-OTP mutation */
  resend: () => void;
  /** True while the network request is in-flight */
  isPending: boolean;
  /** True when the resend request succeeded (used by the page to reset countdown) */
  isSuccess: boolean;
  /**
   * Human-readable error message surfaced to the user.
   * null when there is no error.
   * 429 → "Please wait before requesting another code."
   * 401 → "Session expired. Please sign in again."
   */
  errorMessage: string | null;
  /** Clear mutation state (call after consuming isSuccess to reset it) */
  reset: () => void;
}

// ── Hook ─────────────────────────────────────────────────────────────────────

/**
 * Wraps POST /api/v1/auth/resend-otp.
 *
 * On success (200): exposes isSuccess=true so the page can reset the
 *                   60-second countdown timer.
 * On 429: surfaces "Please wait before requesting another code." with optional
 *          Retry-After value from the response header.
 * On 401: session is invalid; surfaces an error with sign-in link instruction.
 */
export function useResendOtp(): UseResendOtpResult {
  // Read sessionToken at render time — stable for the resend page lifecycle.
  const sessionToken = sessionStorage.getItem(SESSION_TOKEN_KEY) ?? '';

  const {
    mutate: rawMutate,
    isPending,
    isSuccess,
    error,
    reset,
  } = useMutation<ResendOtpResponse, unknown, void>({
    mutationFn: () => resendOtp({ sessionToken }),
  });

  // ── Derive error state ────────────────────────────────────────────────────
  let errorMessage: string | null = null;

  if (error !== null && error !== undefined) {
    if (isAxiosError(error)) {
      const status = error.response?.status;

      if (status === 429) {
        // Surface Retry-After from the response header if available
        const retryAfter = error.response?.headers?.['retry-after'] as
          | string
          | undefined;
        errorMessage =
          retryAfter !== undefined
            ? `Please wait ${retryAfter} seconds before requesting another code.`
            : 'Please wait before requesting another code.';
      } else if (status === 401) {
        errorMessage = 'Session expired. Please sign in again.';
      } else {
        errorMessage = 'Failed to resend code. Please try again.';
      }
    } else {
      errorMessage = 'Failed to resend code. Please try again.';
    }
  }

  return {
    resend: rawMutate,
    isPending,
    isSuccess,
    errorMessage,
    reset,
  };
}
