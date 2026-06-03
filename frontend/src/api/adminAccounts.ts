// Story: US-009
import type { AdminAccountSummary, AccountType, AccountStatus } from '@/types/account';

const BASE_URL = import.meta.env.VITE_API_BASE_URL ?? '';

interface PageResponse<T> {
  content: T[];
  page: {
    size:          number;
    totalElements: number;
    totalPages:    number;
    number:        number;
  };
}

interface ListParams {
  customerId?: string;
  type?:       AccountType;
  status?:     AccountStatus;
  page?:       number;
  size?:       number;
}

async function apiFetch(path: string, options: RequestInit = {}): Promise<Response> {
  const token = sessionStorage.getItem('access_token');
  return fetch(`${BASE_URL}${path}`, {
    ...options,
    headers: {
      'Content-Type': 'application/json',
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...options.headers,
    },
  });
}

/**
 * GET /api/v1/admin/accounts
 * Paginated list of all accounts (AC1, AC3).
 */
export async function listAdminAccounts(
  params: ListParams = {}
): Promise<PageResponse<AdminAccountSummary>> {
  const query = new URLSearchParams();
  if (params.customerId) query.set('customerId', params.customerId);
  if (params.type)       query.set('type',       params.type);
  if (params.status)     query.set('status',     params.status);
  if (params.page !== undefined) query.set('page', String(params.page));
  if (params.size !== undefined) query.set('size', String(params.size));

  const res = await apiFetch(`/api/v1/admin/accounts?${query.toString()}`);
  if (!res.ok) {
    const problem = await res.json().catch(() => ({}));
    throw Object.assign(new Error(problem.detail ?? 'Failed to load accounts'), {
      status: res.status,
    });
  }
  return res.json();
}

/**
 * PATCH /api/v1/admin/accounts/{id}/deactivate
 * Sets account status to INACTIVE (AC4).
 */
export async function deactivateAccount(id: string): Promise<AdminAccountSummary> {
  const res = await apiFetch(`/api/v1/admin/accounts/${id}/deactivate`, { method: 'PATCH' });
  if (!res.ok) {
    const problem = await res.json().catch(() => ({}));
    throw Object.assign(new Error(problem.detail ?? 'Failed to deactivate account'), {
      status: res.status,
    });
  }
  return res.json();
}

/**
 * PATCH /api/v1/admin/accounts/{id}/activate
 * Sets account status back to ACTIVE (AC5).
 */
export async function activateAccount(id: string): Promise<AdminAccountSummary> {
  const res = await apiFetch(`/api/v1/admin/accounts/${id}/activate`, { method: 'PATCH' });
  if (!res.ok) {
    const problem = await res.json().catch(() => ({}));
    throw Object.assign(new Error(problem.detail ?? 'Failed to activate account'), {
      status: res.status,
    });
  }
  return res.json();
}
