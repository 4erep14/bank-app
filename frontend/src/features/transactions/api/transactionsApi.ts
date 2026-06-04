// Story: US-010 | US-011 | US-012 | US-013
import apiClient from '@/shared/api/client';
import type {
  ApiProblem,
  CreateTransferRequest,
  CreateTransferResponse,
  PageResponse,
  TransactionDetail,
  TransactionFilters,
  TransactionSummary,
} from '../types/transaction.types';

export class TransferApiError extends Error {
  status?: number;
  errors?: { field: string; message: string }[];

  constructor(problem: ApiProblem, fallback: string) {
    super(problem.detail ?? fallback);
    this.status = problem.status;
    this.errors = problem.errors;
  }
}

export async function postTransfer(
  request: CreateTransferRequest,
): Promise<CreateTransferResponse> {
  try {
    const { data } = await apiClient.post<CreateTransferResponse>(
      '/api/v1/transactions/transfer',
      request,
    );
    return data;
  } catch (error) {
    if (isAxiosLikeError(error)) {
      const data = error.response?.data as ApiProblem | undefined;
      throw new TransferApiError(
        {
          ...data,
          status: error.response?.status,
        },
        'Unable to complete transfer',
      );
    }
    throw error;
  }
}

export async function getTransactions(
  filters: TransactionFilters = {},
): Promise<PageResponse<TransactionSummary>> {
  const { data } = await apiClient.get<PageResponse<TransactionSummary>>(
    '/api/v1/transactions',
    { params: buildParams(filters) },
  );
  return data;
}

export async function getTransaction(id: string): Promise<TransactionDetail> {
  const { data } = await apiClient.get<TransactionDetail>(`/api/v1/transactions/${id}`);
  return data;
}

export async function getAdminTransactions(
  filters: TransactionFilters = {},
): Promise<PageResponse<TransactionSummary>> {
  const { data } = await apiClient.get<PageResponse<TransactionSummary>>(
    '/api/v1/admin/transactions',
    { params: buildParams(filters) },
  );
  return data;
}

export async function getAdminTransaction(id: string): Promise<TransactionDetail> {
  const { data } = await apiClient.get<TransactionDetail>(`/api/v1/admin/transactions/${id}`);
  return data;
}

function isAxiosLikeError(
  error: unknown,
): error is { response?: { status?: number; data?: unknown } } {
  return typeof error === 'object' && error !== null && 'response' in error;
}

function buildParams(filters: TransactionFilters): Record<string, string | number> {
  return Object.entries(filters).reduce<Record<string, string | number>>((params, [key, value]) => {
    if (value !== undefined && value !== null && value !== '') {
      params[key] = value;
    }
    return params;
  }, {});
}
