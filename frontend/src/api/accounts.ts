// Story: US-006, US-007, US-008
import type { AccountType, AccountDetailResponse, OpenAccountResponse, AccountSummaryItem } from '@/types/account';

const BASE_URL = import.meta.env.VITE_API_BASE_URL ?? '';

/** Shared fetch wrapper that attaches the JWT from sessionStorage. */
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

// ── US-006: Open a new account ───────────────────────────────────────────────

/**
 * POST /api/v1/accounts
 * Opens a new CHECKING or SAVINGS account for the authenticated customer.
 */
export async function openAccount(type: AccountType): Promise<OpenAccountResponse> {
  const res = await apiFetch('/api/v1/accounts', {
    method: 'POST',
    body: JSON.stringify({ type }),
  });
  if (!res.ok) {
    const problem = await res.json().catch(() => ({}));
    throw Object.assign(new Error(problem.detail ?? 'Failed to open account'), {
      status: res.status,
    });
  }
  return res.json();
}

// ── US-007: List all accounts ────────────────────────────────────────────────

/**
 * GET /api/v1/accounts
 * Returns all accounts for the authenticated customer, newest first.
 */
export async function getAccounts(): Promise<AccountSummaryItem[]> {
  const res = await apiFetch('/api/v1/accounts');
  if (!res.ok) {
    const problem = await res.json().catch(() => ({}));
    throw Object.assign(new Error(problem.detail ?? 'Failed to load accounts'), {
      status: res.status,
    });
  }
  return res.json();
}

// ── US-008: Get single account detail ────────────────────────────────────────

/**
 * GET /api/v1/accounts/{id}
 * Returns full details of a specific account.
 * Throws with {@code status} 403 if the account belongs to another customer,
 * 404 if the account does not exist.
 */
export async function getAccountDetail(id: string): Promise<AccountDetailResponse> {
  const res = await apiFetch(`/api/v1/accounts/${id}`);
  if (!res.ok) {
    const problem = await res.json().catch(() => ({}));
    throw Object.assign(
      new Error(problem.detail ?? 'Failed to load account details'),
      { status: res.status }
    );
  }
  return res.json();
}
