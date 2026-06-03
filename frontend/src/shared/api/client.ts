// Story: US-001 | US-005
import axios from 'axios';
import { ACCESS_TOKEN_KEY } from '@/features/auth/types/auth.types';

/**
 * Shared Axios instance.
 * Base URL is injected via VITE_API_BASE_URL at build time.
 */
const apiClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080',
  headers: {
    'Content-Type': 'application/json',
  },
});

/**
 * Request interceptor — attach Bearer token from localStorage when present.
 * Token is stored under ACCESS_TOKEN_KEY ('access_token') by useVerifyOtp (US-003).
 * Auth endpoints (login, OTP, reset-password) are called without a token;
 * the interceptor safely skips adding the header when localStorage is empty.
 */
apiClient.interceptors.request.use((config) => {
  const token = localStorage.getItem(ACCESS_TOKEN_KEY);
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

export default apiClient;
