// Story: US-006 | US-007
import apiClient from '@/shared/api/client';
import type { AccountSummaryItem, AccountType, OpenAccountResponse } from '@/types/account';

/**
 * US-006: Opens a new bank account.
 *
 * @param type  CHECKING or SAVINGS
 * @returns     The created account (AC4 shape)
 * @throws      Error with ProblemDetail.detail message on 4xx/5xx
 */
export async function openAccount(type: AccountType): Promise<OpenAccountResponse> {
  const res = await apiClient.post<OpenAccountResponse>('/api/v1/accounts', { type });
  return res.data;
}

/**
 * US-007: Fetches all accounts for the authenticated customer.
 *
 * @returns List of account summaries, or [] when the customer has no accounts (AC4)
 */
export async function getAccounts(): Promise<AccountSummaryItem[]> {
  const res = await apiClient.get<AccountSummaryItem[]>('/api/v1/accounts');
  return res.data;
}
