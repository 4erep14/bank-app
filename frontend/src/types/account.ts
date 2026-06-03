// Story: US-006

export type AccountType   = 'CHECKING' | 'SAVINGS';
export type AccountStatus = 'ACTIVE' | 'CLOSED' | 'FROZEN';

/**
 * Matches the OpenAccountResponse Java record (AC4).
 */
export interface OpenAccountResponse {
  id:            string;
  accountNumber: string;
  type:          AccountType;
  balance:       number;
  status:        AccountStatus;
  createdAt:     string;  // ISO-8601 OffsetDateTime
}
