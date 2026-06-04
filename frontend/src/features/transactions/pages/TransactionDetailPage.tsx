// Story: US-012
import { useQuery } from '@tanstack/react-query';
import { Link, useParams } from 'react-router-dom';
import { getTransaction } from '../api/transactionsApi';
import { TransactionDetailPanel } from '../components/TransactionDetailPanel';

export default function TransactionDetailPage() {
  const { transactionId } = useParams<{ transactionId: string }>();
  const { data, isLoading, isError } = useQuery({
    queryKey: ['transaction', transactionId],
    queryFn: () => getTransaction(transactionId!),
    enabled: !!transactionId,
    retry: false,
  });

  return (
    <main className="mx-auto max-w-4xl px-4 py-8 sm:px-6 lg:px-8">
      <Link to="/transactions" className="btn btn-ghost btn--sm mb-6">Back to history</Link>
      {isLoading && <div className="h-72 animate-pulse rounded-md bg-slate-100" aria-label="Loading transaction detail" />}
      {isError && (
        <section className="rounded-md border border-red-200 bg-red-50 p-4" role="alert">
          <h1 className="text-base font-semibold text-red-900">Transaction unavailable</h1>
          <p className="mt-1 text-sm text-red-800">This transaction does not exist or is not available for your profile.</p>
        </section>
      )}
      {data && <TransactionDetailPanel transaction={data} />}
    </main>
  );
}
