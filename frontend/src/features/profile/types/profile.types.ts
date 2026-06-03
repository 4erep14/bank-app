// Story: US-005

/**
 * Shape of the profile response returned by GET /api/v1/profile
 * and PATCH /api/v1/profile (200 in both cases).
 */
export interface ProfileResponse {
  firstName: string;
  lastName: string;
  email: string;
  phoneNumber: string;
  /** ISO date string "YYYY-MM-DD" */
  dateOfBirth: string;
}

/**
 * Request body for PATCH /api/v1/profile.
 * Only editable fields are included; email and dateOfBirth are NOT sent.
 */
export interface UpdateProfileRequest {
  firstName?: string;
  lastName?: string;
  phoneNumber?: string;
}

/**
 * RFC 7807 error shape for a single detail message.
 * Returned by PATCH /api/v1/profile on 400 when a field is not editable (AC3).
 */
export interface ApiProfileDetailError {
  detail: string;
}

/**
 * RFC 7807 error shape for field-level validation errors.
 * Returned by PATCH /api/v1/profile on 400 when phone number is invalid (AC4).
 */
export interface ApiProfileValidationError {
  errors: string[];
}
