// Story: US-002
import { useMutation } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import { isAxiosError } from 'axios';
import { loginCustomer } from '../api/authApi';
import { SESSION_TOKEN_KEY } from '../types/auth.types';
import type { LoginRequest } from '../types/auth.types';

// ── Fallback error messages ───────────────────────────────────────────────────

/** AC3: shown on 401; generic to prevent user-enumeration attacks */
const ERROR_INVALID_CREDENTIALS = 'Invalid email or password.';

/**
 * AC4/AC5 fallback: used only when the 423 RFC 7807 ProblemDetail body
 * does not contain a `detail` field.
 */
const ERROR_ACCOUNT_LOCKED =
  'Account locked due to too many failed attempts.';

const ERROR_UNEXPECTED =
  'An unexpected error occurred. Please try again.';

// ── Public interface ──────────────────────────────────────────────────────────

export interface UseLoginResult {
  /** Trigger the login mutation with email + password */
  mutate: (data: LoginRequest) => void;
  /** True while the network request is in-flight */
  isPending: boolean;
  /**
   * Human-readable banner error message for 401 / unexpected errors.
   * null when there is no error, or when isLocked is true (locked state
   * is surfaced via its own dedicated component).
   */
  bannerError: string | null;
  /**
   * True when the server responded 423 (AC4 / AC5).
   * When true, the form should be replaced by LockedAccountNotice.
   */
  isLocked: boolean;
  /**
   * The human-readable locked reason extracted from the RFC 7807
   * ProblemDetail `detail` field (AC4/AC5). Only meaningful when
   * isLocked is true.
   */
  lockedMessage: string;
  /** Clear mutation state (e.g. when user clicks "Back to sign in") */
  reset: () => void;
}

// ── Helper: safely extract `detail` from an RFC 7807 ProblemDetail body ──────

/**
 * Extracts the `detail` string from a ProblemDetail-shaped response body.
 * Returns null if the body is missing or the field is absent / not a string.
 */
function extractProblemDetail(
  data: unknown,
): string | null {
  if (
    data !== null &&
    typeof data === 'object' &&
    'detail' in data &&
    typeof (data as Record<string, unknown>).detail === 'string'
  ) {
    return (data as Record<string, unknown>).detail as string;
  }
  return null;
}

// ── Hook ─────────────────────────────────────────────────────────────────────

/**
 * Wraps POST /api/v1/auth/login.
 *
 * AC1: submits { email, password }
 * AC2: on 200 + 2FA_REQUIRED → stores sessionToken in sessionStorage,
 *       then navigates to /verify-otp (US-003 route)
 * AC3: on 401 → surfaces "Invalid email or password" as a form banner
 * AC4/AC5: on 423 → sets isLocked=true + extracts detail from RFC 7807
 *           ProblemDetail body; consumers should render LockedAccountNotice
 */
export function useLogin(): UseLoginResult {
  const navigate = useNavigate();

  const { mutate: rawMutate, isPending, error, reset } = useMutation<
    Awaited<ReturnType<typeof loginCustomer>>,
    unknown,
    LoginRequest
  >({
    mutationFn: loginCustomer,
    onSuccess: (data) => {
      // AC2: persist short-lived token then proceed to OTP step
      sessionStorage.setItem(SESSION_TOKEN_KEY, data.sessionToken);
      navigate('/verify-otp');
    },
    // onError intentionally omitted — error state is derived below so it
    // remains in sync with isPending across re-renders.
  });

  // ── Derive error state from mutation error ────────────────────────────────
  let bannerError: string | null = null;
  let isLocked = false;
  let lockedMessage = ERROR_ACCOUNT_LOCKED;

  if (error !== null && error !== undefined) {
    if (isAxiosError(error)) {
      const status = error.response?.status;

      if (status === 401) {
        // AC3: generic credential error (do NOT expose which field was wrong)
        bannerError = ERROR_INVALID_CREDENTIALS;
      } else if (status === 423) {
        // AC4 / AC5: account locked — extract detail from RFC 7807 body
        isLocked = true;
        const extracted = extractProblemDetail(error.response?.data);
        lockedMessage = extracted ?? ERROR_ACCOUNT_LOCKED;
      } else {
        bannerError = ERROR_UNEXPECTED;
      }
    } else {
      bannerError = ERROR_UNEXPECTED;
    }
  }

  return {
    mutate: rawMutate,
    isPending,
    bannerError,
    isLocked,
    lockedMessage,
    reset,
  };
}
