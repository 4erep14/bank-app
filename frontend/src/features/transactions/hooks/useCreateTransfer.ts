// Story: US-010
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postTransfer } from '../api/transactionsApi';
import type {
  CreateTransferRequest,
  CreateTransferResponse,
} from '../types/transaction.types';

export function useCreateTransfer() {
  const queryClient = useQueryClient();

  return useMutation<CreateTransferResponse, Error, CreateTransferRequest>({
    mutationFn: postTransfer,
    onSuccess: (_response, request) => {
      queryClient.invalidateQueries({ queryKey: ['accounts'] });
      queryClient.invalidateQueries({ queryKey: ['account', request.sourceAccountId] });
      queryClient.invalidateQueries({ queryKey: ['account', request.destinationAccountId] });
      queryClient.invalidateQueries({ queryKey: ['transactions'] });
    },
  });
}
