// Story: US-005
import apiClient from '@/shared/api/client';
import type { ProfileResponse, UpdateProfileRequest } from '../types/profile.types';

/**
 * GET /api/v1/profile
 *
 * Fetches the authenticated customer's profile.
 * Bearer token is attached automatically by the Axios request interceptor
 * in client.ts (added for US-005).
 *
 * 200 → ProfileResponse
 * 401 → RFC 7807 — handled by useProfile hook (AC6)
 */
export async function getProfile(): Promise<ProfileResponse> {
  const { data } = await apiClient.get<ProfileResponse>('/api/v1/profile');
  return data;
}

/**
 * PATCH /api/v1/profile
 *
 * Updates the authenticated customer's editable profile fields.
 * Only firstName, lastName, and phoneNumber may be changed (AC2).
 * Email and dateOfBirth are read-only on the backend (AC3).
 *
 * 200  → ProfileResponse (updated)
 * 400  → RFC 7807 { detail: "Field is not editable" }    (AC3 defensive)
 * 400  → RFC 7807 { errors: string[] }                   (AC4 — E.164 violation)
 * 401  → RFC 7807                                         (AC6)
 */
export async function updateProfile(
  req: UpdateProfileRequest,
): Promise<ProfileResponse> {
  const { data } = await apiClient.patch<ProfileResponse>('/api/v1/profile', req);
  return data;
}
