// Story: US-010
import apiClient from '@/shared/api/client';
import type {
  ApiProblem,
  CreateTransferRequest,
  CreateTransferResponse,
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

function isAxiosLikeError(
  error: unknown,
): error is { response?: { status?: number; data?: unknown } } {
  return typeof error === 'object' && error !== null && 'response' in error;
}
