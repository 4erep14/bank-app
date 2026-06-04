// Story: US-010 | US-011 | US-012 | US-013
export type TransactionStatus = 'PENDING_EVALUATION' | 'COMPLETED' | 'BLOCKED' | 'REJECTED';
export type TransactionType = 'TRANSFER';

export interface CreateTransferRequest {
  sourceAccountId: string;
  destinationAccountId: string;
  amount: string;
  description?: string;
}

export interface CreateTransferResponse {
  transactionId: string;
  status: TransactionStatus;
}

export interface TransactionSummary {
  id: string;
  customerId: string;
  type: TransactionType;
  status: TransactionStatus;
  amount: string;
  sourceAccountId: string;
  destinationAccountId: string;
  description?: string | null;
  timestamp: string;
}

export interface TransactionDetail extends TransactionSummary {
  createdAt: string;
  updatedAt: string;
}

export interface TransactionFilters {
  accountId?: string;
  customerId?: string;
  type?: TransactionType;
  status?: TransactionStatus;
  from?: string;
  to?: string;
  page?: number;
  size?: number;
}

export interface PageResponse<T> {
  content: T[];
  number: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
}

export interface ApiProblem {
  status?: number;
  detail?: string;
  errors?: { field: string; message: string }[];
}
