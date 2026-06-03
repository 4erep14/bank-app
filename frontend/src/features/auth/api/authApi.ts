// Story: US-002 | US-003 | US-004
import apiClient from '@/shared/api/client';
import type {
  LoginRequest,
  LoginResponse,
  VerifyOtpRequest,
  VerifyOtpResponse,
  ResendOtpRequest,
  ResendOtpResponse,
  ForgotPasswordRequest,
  ForgotPasswordResponse,
  ResetPasswordRequest,
  ResetPasswordResponse,
} from '../types/auth.types';

/**
 * POST /api/v1/auth/login
 *
 * Sends email + password credentials.
 * On success the backend responds 200 with { status: "2FA_REQUIRED", sessionToken }.
 * On failure it throws an AxiosError so the mutation hook can inspect
 * error.response.status and error.response.data.detail.
 *
 * 200 → LoginResponse
 * 401 → { detail: "Invalid email or password" }
 * 423 → { detail: "Account locked due to too many failed login attempts" }
 */
export async function loginCustomer(data: LoginRequest): Promise<LoginResponse> {
  const response = await apiClient.post<LoginResponse>('/api/v1/auth/login', data);
  return response.data;
}

// Story: US-003

/**
 * POST /api/v1/auth/verify-otp
 *
 * Submits the 6-digit OTP for verification.
 * 200 → { accessToken, refreshToken }
 * 401 → RFC 7807 { detail: "Invalid or expired OTP", remainingAttempts: number }
 */
export async function verifyOtp(req: VerifyOtpRequest): Promise<VerifyOtpResponse> {
  const response = await apiClient.post<VerifyOtpResponse>('/api/v1/auth/verify-otp', req);
  return response.data;
}

/**
 * POST /api/v1/auth/resend-otp
 *
 * Requests a new OTP SMS for the current session.
 * 200 → { message: "OTP sent" }
 * 429 → RFC 7807 with Retry-After header
 * 401 → if session is invalid
 */
export async function resendOtp(req: ResendOtpRequest): Promise<ResendOtpResponse> {
  const response = await apiClient.post<ResendOtpResponse>('/api/v1/auth/resend-otp', req);
  return response.data;
}

// ─── US-004: Password Reset ────────────────────────────────────────────────────

/**
 * POST /api/v1/auth/forgot-password
 *
 * Submits the user's email to trigger a password-reset email.
 * The backend ALWAYS returns 200 to prevent user enumeration (AC1).
 * 200 → ForgotPasswordResponse { message }
 */
export async function forgotPassword(
  req: ForgotPasswordRequest,
): Promise<ForgotPasswordResponse> {
  const response = await apiClient.post<ForgotPasswordResponse>(
    '/api/v1/auth/forgot-password',
    req,
  );
  return response.data;
}

/**
 * POST /api/v1/auth/reset-password
 *
 * Submits the one-time reset token + new password.
 * 200 → ResetPasswordResponse { message }                (AC4)
 * 400 → RFC 7807 { detail: "Invalid or expired reset token" }  (AC6/AC7)
 * 400 → RFC 7807 { errors: string[] }                         (AC3 – weak password)
 */
export async function resetPassword(
  req: ResetPasswordRequest,
): Promise<ResetPasswordResponse> {
  const response = await apiClient.post<ResetPasswordResponse>(
    '/api/v1/auth/reset-password',
    req,
  );
  return response.data;
}
