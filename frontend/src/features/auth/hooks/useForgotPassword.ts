// Story: US-004
import { useMutation } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import { forgotPassword } from '../api/authApi';
import { FORGOT_PASSWORD_EMAIL_KEY } from '../types/auth.types';
import type { ForgotPasswordRequest, ForgotPasswordResponse } from '../types/auth.types';

// ── Public interface ──────────────────────────────────────────────────────────

export interface UseForgotPasswordResult {
  /** Trigger the forgot-password mutation with the user's email address */
  submit: (data: ForgotPasswordRequest) => void;
  /** True while the network request is in-flight */
  isPending: boolean;
  /**
   * True when the mutation has completed successfully.
   * Used by ForgotPasswordSentPage to show resend confirmation feedback.
   */
  isSuccess: boolean;
  /**
   * Human-readable banner error for unexpected network/server errors.
   * null when there is no error (the API always returns 200 for valid requests).
   */
  bannerError: string | null;
  /** Clear mutation state */
  reset: () => void;
}

// ── Hook ─────────────────────────────────────────────────────────────────────

/**
 * Wraps POST /api/v1/auth/forgot-password.
 *
 * AC1: always gets 200 — no enumeration risk.
 * On success: stores the submitted email in sessionStorage so
 *             ForgotPasswordSentPage can offer a "Resend email" action,
 *             then navigates to /forgot-password/sent.
 */
export function useForgotPassword(): UseForgotPasswordResult {
  const navigate = useNavigate();

  const {
    mutate: rawMutate,
    isPending,
    isSuccess,
    error,
    reset,
  } = useMutation<ForgotPasswordResponse, unknown, ForgotPasswordRequest>({
    mutationFn: forgotPassword,
    onSuccess: (_data, variables) => {
      // Persist email for the resend action on the sent page (AC1/AC2)
      sessionStorage.setItem(FORGOT_PASSWORD_EMAIL_KEY, variables.email);
      navigate('/forgot-password/sent');
    },
  });

  // ── Derive error state ────────────────────────────────────────────────────
  // The forgot-password endpoint always returns 200, so errors here are
  // unexpected network failures or misconfigured environments.
  const bannerError: string | null =
    error !== null && error !== undefined
      ? 'An unexpected error occurred. Please try again.'
      : null;

  return {
    submit: rawMutate,
    isPending,
    isSuccess,
    bannerError,
    reset,
  };
}
