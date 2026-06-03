// Story: US-010
export type TransactionStatus = 'PENDING_EVALUATION' | 'COMPLETED' | 'BLOCKED';

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

export interface ApiProblem {
  status?: number;
  detail?: string;
  errors?: { field: string; message: string }[];
}
