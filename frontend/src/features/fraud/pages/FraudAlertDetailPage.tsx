// Story: US-017 | US-018
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Link, useParams } from 'react-router-dom';
import { approveFraudAlert, getFraudAlert, rejectFraudAlert } from '../api/fraudApi';
import { TransactionDetailPanel } from '@/features/transactions/components/TransactionDetailPanel';

export default function FraudAlertDetailPage() {
  const { alertId } = useParams<{ alertId: string }>();
  const queryClient = useQueryClient();
  const { data, isLoading, isError, error } = useQuery({
    queryKey: ['fraud-alert', alertId],
    queryFn: () => getFraudAlert(alertId!),
    enabled: !!alertId,
    retry: false,
  });

  const approveMutation = useMutation({
    mutationFn: () => approveFraudAlert(alertId!),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['fraud-alert', alertId] });
      queryClient.invalidateQueries({ queryKey: ['fraud-alerts'] });
    },
  });

  const rejectMutation = useMutation({
    mutationFn: () => rejectFraudAlert(alertId!),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['fraud-alert', alertId] });
      queryClient.invalidateQueries({ queryKey: ['fraud-alerts'] });
    },
  });

  const mutationError = approveMutation.error ?? rejectMutation.error ?? error;

  return (
    <main className="mx-auto max-w-5xl px-4 py-8 sm:px-6 lg:px-8">
      <Link to="/fraud/alerts" className="btn btn-ghost btn--sm mb-6">Back to alerts</Link>
      {isLoading && <div className="h-72 animate-pulse rounded-md bg-slate-100" />}
      {isError && <ErrorBox message={getProblemDetail(mutationError, 'Could not load fraud alert.')} />}
      {mutationError && !isError && <ErrorBox message={getProblemDetail(mutationError, 'Could not complete review action.')} />}
      {data && (
        <div className="space-y-5">
          <section className="rounded-md border border-slate-200 bg-white p-6 shadow-sm">
            <div className="flex flex-wrap items-start justify-between gap-4">
              <div>
                <p className="text-sm font-semibold uppercase tracking-normal text-slate-500">Fraud alert</p>
                <h1 className="mt-1 text-2xl font-semibold text-slate-950">{data.summary.triggeredRuleName}</h1>
                <p className="mt-1 text-sm text-slate-600">
                  {data.summary.ruleConditionType.replace('_', ' ')} matched {data.actualValue} against {data.thresholdValue}
                </p>
              </div>
              <span className="rounded-full bg-slate-100 px-3 py-1 text-sm font-semibold text-slate-800">
                {data.summary.reviewStatus.replace('_', ' ')}
              </span>
            </div>
            {data.transaction.status === 'BLOCKED' && (
              <div className="mt-5 flex gap-2">
                <button className="btn btn-primary btn--sm" onClick={() => approveMutation.mutate()} disabled={approveMutation.isPending}>
                  Approve transfer
                </button>
                <button className="btn btn-ghost btn--sm" onClick={() => rejectMutation.mutate()} disabled={rejectMutation.isPending}>
                  Reject transfer
                </button>
              </div>
            )}
          </section>
          <TransactionDetailPanel transaction={data.transaction} showCustomer />
        </div>
      )}
    </main>
  );
}

function ErrorBox({ message }: { message: string }) {
  return <div className="mb-4 rounded-md border border-red-200 bg-red-50 p-4 text-sm text-red-800" role="alert">{message}</div>;
}

function getProblemDetail(error: unknown, fallback: string): string {
  if (typeof error === 'object' && error !== null && 'response' in error) {
    const response = (error as { response?: { data?: { detail?: string } } }).response;
    return response?.data?.detail ?? fallback;
  }
  return fallback;
}
