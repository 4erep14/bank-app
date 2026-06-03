// Story: US-001
import apiClient from '@/shared/api/client';
import type {
  RegisterRequest,
  RegisterResponse,
} from '../types/registration.types';

/**
 * POST /api/v1/customers
 * Throws AxiosError on non-2xx responses so the mutation hook can inspect
 * error.response.status and error.response.data.
 */
export async function registerCustomer(
  data: RegisterRequest,
): Promise<RegisterResponse> {
  const response = await apiClient.post<RegisterResponse>(
    '/api/v1/customers',
    data,
  );
  return response.data;
}
