// Story: US-006 / US-007 / US-008 / US-009

/** Discriminated union of supported account types. */
export type AccountType = 'CHECKING' | 'SAVINGS';

/** Possible lifecycle states of a bank account. */
export type AccountStatus = 'ACTIVE' | 'INACTIVE' | 'CLOSED' | 'FROZEN';

// ── US-006 ───────────────────────────────────────────────────────────────────

/** Response payload after opening a new account (POST /api/v1/accounts). */
export interface OpenAccountResponse {
  id:            string;
  accountNumber: string;
  type:          AccountType;
  balance:       string;
  status:        AccountStatus;
  createdAt:     string;
}

// ── US-007 ───────────────────────────────────────────────────────────────────

/** Summary item in the customer account list (GET /api/v1/accounts). */
export interface AccountSummaryItem {
  id:            string;
  accountNumber: string;
  type:          AccountType;
  balance:       string;
  status:        AccountStatus;
}

// ── US-008 ───────────────────────────────────────────────────────────────────

/** Full detail payload for a single account (GET /api/v1/accounts/{id}). */
export interface AccountDetailResponse {
  id:            string;
  accountNumber: string;
  type:          AccountType;
  balance:       string;
  status:        AccountStatus;
  createdAt:     string;
}

// ── US-009 ───────────────────────────────────────────────────────────────────

/** Admin-facing account row (GET /api/v1/admin/accounts). Extends summary with owner PII. */
export interface AdminAccountSummary {
  id:            string;
  accountNumber: string;
  type:          AccountType;
  balance:       string;
  status:        AccountStatus;
  ownerFullName: string;
  ownerEmail:    string;
}
