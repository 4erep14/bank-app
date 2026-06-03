// Story: US-002

/**
 * Request body sent to POST /api/v1/auth/login
 */
export interface LoginRequest {
  email: string;
  password: string;
}

/**
 * 200 success response from POST /api/v1/auth/login.
 * The backend always returns status "2FA_REQUIRED" at this stage;
 * the sessionToken is passed to the OTP verification step (US-003).
 */
export interface LoginResponse {
  status: '2FA_REQUIRED';
  sessionToken: string;
}

/**
 * Shape of the error body returned for 401 and 423 responses.
 */
export interface ApiLoginError {
  detail: string;
}

/**
 * sessionStorage key used to persist the short-lived session token
 * between the login step and the OTP verification step (US-003).
 */
export const SESSION_TOKEN_KEY = 'session_token' as const;

// Story: US-004
/**
 * sessionStorage key used to persist the submitted email address
 * between ForgotPasswordPage and ForgotPasswordSentPage so the
 * "Resend email" button can re-submit without asking again.
 */
export const FORGOT_PASSWORD_EMAIL_KEY = 'forgot_password_email' as const;

// ─── US-004: Password Reset ───────────────────────────────────────────────────

/** Request body sent to POST /api/v1/auth/forgot-password */
export interface ForgotPasswordRequest {
  email: string;
}

/**
 * 200 response from POST /api/v1/auth/forgot-password.
 * The backend always returns 200 to prevent user-enumeration.
 */
export interface ForgotPasswordResponse {
  message: string;
}

/** Request body sent to POST /api/v1/auth/reset-password */
export interface ResetPasswordRequest {
  token: string;
  newPassword: string;
}

/** 200 response from POST /api/v1/auth/reset-password */
export interface ResetPasswordResponse {
  message: string;
}

/**
 * Single field-level validation error returned in a 400 `errors` array.
 * e.g. { field: "newPassword", message: "Password must be at least 8 characters" }
 */
export interface FieldError {
  field: string;
  message: string;
}

/**
 * 400 error body shape when the API returns field-level validation errors.
 * { errors: [{ field, message }, ...] }
 */
export interface ApiValidationError {
  errors: FieldError[];
}

/**
 * 400 error body shape when the API returns a single plain-text detail.
 * e.g. { detail: "Invalid or expired reset token" }
 */
export interface ApiDetailError {
  detail: string;
}

// ─── US-003: Two-Factor Authentication via SMS ────────────────────────────────

/** Request body sent to POST /api/v1/auth/verify-otp */
export interface VerifyOtpRequest {
  sessionToken: string;
  otp: string;
}

/** 200 response from POST /api/v1/auth/verify-otp */
export interface VerifyOtpResponse {
  accessToken: string;
  refreshToken: string;
}

/** Request body sent to POST /api/v1/auth/resend-otp */
export interface ResendOtpRequest {
  sessionToken: string;
}

/** 200 response from POST /api/v1/auth/resend-otp */
export interface ResendOtpResponse {
  message: string;
}

/**
 * RFC 7807 error shape returned for OTP verification failures.
 * 401: { detail: "Invalid or expired OTP", remainingAttempts: number }
 */
export interface ApiOtpError {
  detail: string;
  remainingAttempts: number;
}

/**
 * localStorage key used to persist the access token after OTP verification (US-003).
 * Set in useVerifyOtp on successful verification.
 */
export const ACCESS_TOKEN_KEY = 'access_token' as const;

/**
 * localStorage key used to persist the refresh token after OTP verification (US-003).
 * Set in useVerifyOtp on successful verification.
 */
export const REFRESH_TOKEN_KEY = 'refresh_token' as const;
