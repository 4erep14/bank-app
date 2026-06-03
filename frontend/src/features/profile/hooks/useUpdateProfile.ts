// Story: US-005
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { isAxiosError } from 'axios';
import { updateProfile } from '../api/profileApi';
import type { UpdateProfileRequest, ProfileResponse } from '../types/profile.types';

// ── Structured error state exposed to consumers ───────────────────────────────

export interface UseUpdateProfileResult {
  /** Trigger the PATCH /api/v1/profile mutation */
  mutate: (req: UpdateProfileRequest) => void;
  /** True while the network request is in-flight */
  isPending: boolean;
  /** True when the last mutation completed successfully */
  isSuccess: boolean;
  /**
   * Single banner-level error message.
   * Set on: 400 { detail }, 401, or unexpected errors.
   * null when there is no error.
   */
  serverError: string | null;
  /**
   * Array of field-level validation error strings from 400 { errors: string[] }.
   * Empty when there are no field errors.
   * AC4: phone number E.164 validation failure.
   */
  fieldErrors: string[];
  /** Reset mutation state (clears errors and success flag) */
  reset: () => void;
}

// ── Helper: extract RFC 7807 error data from response body ────────────────────

interface ProblemDetailBody {
  detail?: unknown;
  errors?: unknown;
}

function parseProblemDetail(data: unknown): { detail: string | null; errors: string[] } {
  if (data !== null && typeof data === 'object') {
    const body = data as ProblemDetailBody;
    const detail = typeof body.detail === 'string' ? body.detail : null;
    const errors = Array.isArray(body.errors)
      ? (body.errors as unknown[])
          .filter((e): e is string => typeof e === 'string')
      : [];
    return { detail, errors };
  }
  return { detail: null, errors: [] };
}

// ── Hook ──────────────────────────────────────────────────────────────────────

/**
 * Wraps PATCH /api/v1/profile.
 *
 * AC2: sends { firstName?, lastName?, phoneNumber? }
 * AC3: 400 { detail: "Field is not editable" } → serverError (defensive)
 * AC4: 400 { errors: string[] } → fieldErrors (phone E.164 violation)
 * AC5: on success → invalidates ['profile'] query so view reflects changes
 * AC6: 401 is exposed as serverError (redirect handled by useProfile)
 */
export function useUpdateProfile(): UseUpdateProfileResult {
  const queryClient = useQueryClient();

  const { mutate, isPending, isSuccess, error, reset } = useMutation<
    ProfileResponse,
    unknown,
    UpdateProfileRequest
  >({
    mutationFn: updateProfile,
    onSuccess: () => {
      // AC5: invalidate so the GET /api/v1/profile re-fetches fresh data
      void queryClient.invalidateQueries({ queryKey: ['profile'] });
    },
  });

  // ── Derive structured error state ─────────────────────────────────────────
  let serverError: string | null = null;
  let fieldErrors: string[] = [];

  if (error !== null && error !== undefined) {
    if (isAxiosError(error)) {
      const status = error.response?.status;

      if (status === 400) {
        const { detail, errors } = parseProblemDetail(error.response?.data);
        if (detail) {
          // AC3: "Field is not editable" (defensive — shouldn't happen in normal usage)
          serverError = detail;
        } else if (errors.length > 0) {
          // AC4: field-level validation failures (e.g. E.164 phone format)
          fieldErrors = errors;
        } else {
          serverError = 'An error occurred while updating your profile. Please try again.';
        }
      } else if (status === 401) {
        serverError = 'Your session has expired. Please sign in again.';
      } else {
        serverError = 'An unexpected error occurred. Please try again.';
      }
    } else {
      serverError = 'An unexpected error occurred. Please try again.';
    }
  }

  return { mutate, isPending, isSuccess, serverError, fieldErrors, reset };
}
