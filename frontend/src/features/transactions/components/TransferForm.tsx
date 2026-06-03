// Story: US-010
import { zodResolver } from '@hookform/resolvers/zod';
import { useMemo } from 'react';
import { useForm } from 'react-hook-form';
import type { AccountSummaryItem } from '@/types/account';
import { TransferApiError } from '../api/transactionsApi';
import { useCreateTransfer } from '../hooks/useCreateTransfer';
import type { CreateTransferResponse } from '../types/transaction.types';
import { transferSchema, type TransferFormValues } from '../validation/transferSchema';
import { AccountSelect } from './AccountSelect';
import { TransferReviewPanel } from './TransferReviewPanel';

interface TransferFormProps {
  accounts: AccountSummaryItem[];
  onSuccess: (result: CreateTransferResponse) => void;
}

export function TransferForm({ accounts, onSuccess }: TransferFormProps) {
  const transferMutation = useCreateTransfer();
  const {
    register,
    handleSubmit,
    setValue,
    watch,
    formState: { errors },
  } = useForm<TransferFormValues>({
    resolver: zodResolver(transferSchema),
    defaultValues: {
      sourceAccountId: '',
      destinationAccountId: '',
      amount: '',
      description: '',
    },
  });

  const values = watch();
  const activeAccounts = useMemo(
    () => accounts.filter((account) => account.status === 'ACTIVE'),
    [accounts],
  );
  const source = accounts.find((account) => account.id === values.sourceAccountId);
  const destination = accounts.find((account) => account.id === values.destinationAccountId);
  const amount = Number(values.amount || 0);
  const sourceBalance = source ? Number(source.balance) : 0;
  const balanceWarning =
    source && amount > sourceBalance ? 'Insufficient funds.' : undefined;

  const onSubmit = handleSubmit(async (formValues) => {
    if (balanceWarning) {
      return;
    }
    const result = await transferMutation.mutateAsync({
      sourceAccountId: formValues.sourceAccountId,
      destinationAccountId: formValues.destinationAccountId,
      amount: Number(formValues.amount).toFixed(2),
      description: formValues.description?.trim() || undefined,
    });
    onSuccess(result);
  });

  const apiError = transferMutation.error
    ? getApiErrorMessage(transferMutation.error)
    : undefined;

  return (
    <form onSubmit={onSubmit} noValidate className="grid gap-6 lg:grid-cols-[minmax(0,1fr)_360px]">
      <section className="space-y-5" aria-label="Transfer details">
        <AccountSelect
          id="sourceAccountId"
          label="From account"
          accounts={activeAccounts}
          value={values.sourceAccountId}
          excludedAccountId={values.destinationAccountId}
          error={errors.sourceAccountId?.message}
          onChange={(value) => setValue('sourceAccountId', value, { shouldValidate: true })}
        />

        <AccountSelect
          id="destinationAccountId"
          label="To account"
          accounts={activeAccounts}
          value={values.destinationAccountId}
          excludedAccountId={values.sourceAccountId}
          error={errors.destinationAccountId?.message}
          onChange={(value) => setValue('destinationAccountId', value, { shouldValidate: true })}
        />

        <div className="space-y-2">
          <label htmlFor="amount" className="block text-sm font-medium text-slate-800">
            Amount
          </label>
          <div className="flex rounded-md border border-slate-300 bg-white shadow-sm focus-within:border-blue-600 focus-within:ring-2 focus-within:ring-blue-100">
            <span className="border-r border-slate-200 px-3 py-3 text-sm text-slate-500">$</span>
            <input
              id="amount"
              type="text"
              inputMode="decimal"
              autoComplete="off"
              {...register('amount')}
              aria-describedby="amount-error amount-help"
              className="min-w-0 flex-1 rounded-r-md border-0 px-3 py-3 text-sm text-slate-900 focus:outline-none"
            />
          </div>
          <p id="amount-help" className="text-sm text-slate-500">
            Maximum transfer is $10,000.00.
            {source ? ` Available: $${sourceBalance.toFixed(2)}.` : ''}
          </p>
          {(errors.amount?.message || balanceWarning) && (
            <p id="amount-error" className="text-sm text-red-700" role="alert">
              {errors.amount?.message ?? balanceWarning}
            </p>
          )}
        </div>

        <div className="space-y-2">
          <label htmlFor="description" className="block text-sm font-medium text-slate-800">
            Description <span className="font-normal text-slate-500">Optional</span>
          </label>
          <input
            id="description"
            type="text"
            maxLength={255}
            {...register('description')}
            aria-describedby={errors.description ? 'description-error' : undefined}
            className="w-full rounded-md border border-slate-300 bg-white px-3 py-3 text-sm text-slate-900 shadow-sm focus:border-blue-600 focus:outline-none focus:ring-2 focus:ring-blue-100"
          />
          {errors.description && (
            <p id="description-error" className="text-sm text-red-700" role="alert">
              {errors.description.message}
            </p>
          )}
        </div>

        {apiError && (
          <p className="rounded-md border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-800" role="alert">
            {apiError}
          </p>
        )}

        <button
          type="submit"
          className="btn btn-primary w-full sm:w-auto"
          disabled={transferMutation.isPending}
        >
          {transferMutation.isPending ? 'Transferring...' : 'Transfer money'}
        </button>
      </section>

      <TransferReviewPanel
        source={source}
        destination={destination}
        amount={values.amount}
        description={values.description}
      />
    </form>
  );
}

function getApiErrorMessage(error: Error) {
  if (error instanceof TransferApiError) {
    if (error.status === 401) return 'Your session has expired. Please sign in again.';
    if (error.status === 403) return 'One of these accounts cannot be used for this transfer.';
    return error.message;
  }
  return 'We could not complete the transfer right now. Please try again.';
}
