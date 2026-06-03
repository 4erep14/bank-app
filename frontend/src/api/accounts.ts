// Story: US-006
import { apiClient } from '@/lib/apiClient';
import type { AccountType, OpenAccountResponse } from '@/types/account';

/**
 * Calls POST /api/v1/accounts to open a new bank account.
 *
 * @param type  CHECKING or SAVINGS
 * @returns     The created account details (AC4 response shape)
 * @throws      Error with the ProblemDetail message on 4xx/5xx
 */
export async function openAccount(type: AccountType): Promise<OpenAccountResponse> {
  const res = await apiClient.post<OpenAccountResponse>('/api/v1/accounts', { type });

  if (!res.ok) {
    const problem = await res.json().catch(() => ({}));
    throw new Error(problem.detail ?? 'Failed to open account');
  }

  return res.json();
}
