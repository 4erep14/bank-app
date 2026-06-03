// Story: US-004
import { useMutation } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import { isAxiosError } from 'axios';
import { resetPassword } from '../api/authApi';
import type { ResetPasswordRequest, ResetPasswordResponse } from '../types/auth.types';

// ── Fallback messages ─────────────────────────────────────────────────────────

const ERROR_UNEXPECTED = 'An unexpected error occurred. Please try again.';
const INVALID_TOKEN_DETAIL = 'invalid or expired';

// ── Public interface ──────────────────────────────────────────────────────────

export interface UseResetPasswordResult {
  /** Trigger the reset-password mutation with token + new password */
  submit: (data: ResetPasswordRequest) => void;
  /** True while the network request is in-flight */
  isPending: boolean;
  /**
   * Array of field-level validation error strings from a 400 { errors: string[] }
   * response (AC3 — weak-password bean validation).
   * Empty when there are no field errors.
   */
  fieldErrors: string[];
  /**
   * Human-readable banner error for unexpected server/network failures.
   * null when there is no error or when navigation to /reset-password/error
   * has already been triggered.
   */
  bannerError: string | null;
  /** Clear mutation state */
  reset: () => void;
}

// ── Helpers ───────────────────────────────────────────────────────────────────

/** Extracts the RFC 7807 `detail` string from an unknown response body. */
function extractDetail(data: unknown): string | null {
  if (data !== null && typeof data === 'object') {
    const d = data as Record<string, unknown>;
    if (typeof d.detail === 'string') return d.detail;
  }
  return null;
}

/**
 * Extracts a `string[]` from the `errors` property of an unknown response body.
 * Non-string items are filtered out so the return type is always `string[]`.
 */
function extractErrors(data: unknown): string[] {
  if (data !== null && typeof data === 'object') {
    const d = data as Record<string, unknown>;
    if (Array.isArray(d.errors)) {
      return d.errors.filter((e): e is string => typeof e === 'string');
    }
  }
  return [];
}

// ── Hook ─────────────────────────────────────────────────────────────────────

/**
 * Wraps POST /api/v1/auth/reset-password.
 *
 * AC4: on 200 → navigates to /reset-password/success.
 * AC6/AC7: on 400 { detail: "Invalid or expired reset token" }
 *           → navigates to /reset-password/error (in onError side-effect).
 * AC3: on 400 { errors: string[] } → exposes fieldErrors for inline display.
 */
export function useResetPassword(): UseResetPasswordResult {
  const navigate = useNavigate();

  const {
    mutate: rawMutate,
    isPending,
    error,
    reset,
  } = useMutation<ResetPasswordResponse, unknown, ResetPasswordRequest>({
    mutationFn: resetPassword,

    // AC4: valid token + compliant password → success route
    onSuccess: () => {
      navigate('/reset-password/success');
    },

    // AC6/AC7: navigate to error page immediately so we don't show an error
    // banner on a form that will be replaced by the error page.
    onError: (err) => {
      if (isAxiosError(err) && err.response?.status === 400) {
        const detail = extractDetail(err.response?.data);
        if (
          detail !== null &&
          detail.toLowerCase().includes(INVALID_TOKEN_DETAIL)
        ) {
          navigate('/reset-password/error');
        }
      }
    },
  });

  // ── Derive UI error state ─────────────────────────────────────────────────
  let fieldErrors: string[] = [];
  let bannerError: string | null = null;

  if (error !== null && error !== undefined) {
    if (isAxiosError(error) && error.response?.status === 400) {
      const detail = extractDetail(error.response?.data);

      if (
        detail !== null &&
        detail.toLowerCase().includes(INVALID_TOKEN_DETAIL)
      ) {
        // Navigation already triggered in onError; suppress UI error here
        // so the banner does not flash before the page unmounts.
      } else {
        // AC3: backend bean-validation errors (weak password)
        const errors = extractErrors(error.response?.data);
        if (errors.length > 0) {
          fieldErrors = errors;
        } else {
          bannerError = ERROR_UNEXPECTED;
        }
      }
    } else {
      bannerError = ERROR_UNEXPECTED;
    }
  }

  return {
    submit: rawMutate,
    isPending,
    fieldErrors,
    bannerError,
    reset,
  };
}
