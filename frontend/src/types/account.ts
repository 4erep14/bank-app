// Story: US-006 | US-007

export type AccountType   = 'CHECKING' | 'SAVINGS';
export type AccountStatus = 'ACTIVE' | 'CLOSED' | 'FROZEN';

/**
 * US-006 AC4 — response shape returned when an account is opened.
 */
export interface OpenAccountResponse {
  id:            string;
  accountNumber: string;
  type:          AccountType;
  balance:       number;
  status:        AccountStatus;
  createdAt:     string; // ISO-8601 OffsetDateTime
}

/**
 * US-007 AC2 — one item in the GET /api/v1/accounts list.
 * Balance is always 2 decimal places (AC5).
 */
export interface AccountSummaryItem {
  accountNumber: string;
  type:          AccountType;
  balance:       number;
  status:        AccountStatus;
}
