// Story: US-006, US-007, US-008

/** Discriminated union of supported account types. */
export type AccountType = 'CHECKING' | 'SAVINGS';

/** Possible lifecycle states of a bank account. */
export type AccountStatus = 'ACTIVE' | 'CLOSED' | 'FROZEN';

// ── US-006 ───────────────────────────────────────────────────────────────────

/** Response payload after opening a new account (POST /api/v1/accounts). */
export interface OpenAccountResponse {
  id: string;
  accountNumber: string;
  type: AccountType;
  balance: string;   // BigDecimal serialised as string to avoid float precision issues
  status: AccountStatus;
  createdAt: string; // ISO-8601
}

// ── US-007 ───────────────────────────────────────────────────────────────────

/** Summary item returned in the account list (GET /api/v1/accounts). */
export interface AccountSummaryItem {
  id: string;
  accountNumber: string;
  type: AccountType;
  balance: string;
  status: AccountStatus;
}

// ── US-008 ───────────────────────────────────────────────────────────────────

/** Full detail payload for a single account (GET /api/v1/accounts/{id}). */
export interface AccountDetailResponse {
  id: string;
  accountNumber: string;
  type: AccountType;
  balance: string;
  status: AccountStatus;
  createdAt: string; // ISO-8601
}
