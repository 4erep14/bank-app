// Story: US-010
import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import { getAccounts } from '@/api/accounts';
import { TransferForm } from '../components/TransferForm';
import { TransferResult } from '../components/TransferResult';
import type { CreateTransferResponse } from '../types/transaction.types';

export default function TransferPage() {
  const navigate = useNavigate();
  const [result, setResult] = useState<CreateTransferResponse | null>(null);
  const { data: accounts = [], isLoading, isError, refetch } = useQuery({
    queryKey: ['accounts'],
    queryFn: getAccounts,
    staleTime: 30_000,
  });

  const activeAccountCount = accounts.filter((account) => account.status === 'ACTIVE').length;

  return (
    <main className="mx-auto max-w-5xl px-4 py-8 sm:px-6 lg:px-8" aria-labelledby="transfer-heading">
      <button
        type="button"
        className="btn btn-ghost btn--sm mb-6"
        onClick={() => navigate('/dashboard')}
        aria-label="Back to dashboard"
      >
        Back to dashboard
      </button>

      {result ? (
        <TransferResult
          result={result}
          onReset={() => setResult(null)}
          onDashboard={() => navigate('/dashboard')}
        />
      ) : (
        <>
          <header className="mb-6">
            <h1 id="transfer-heading" className="text-3xl font-semibold text-slate-950">
              Transfer money
            </h1>
            <p className="mt-2 text-sm text-slate-600">
              Move funds between your active NorthBank accounts.
            </p>
          </header>

          {isLoading && <TransferSkeleton />}

          {isError && (
            <section className="rounded-md border border-red-200 bg-red-50 p-4" role="alert">
              <h2 className="text-base font-semibold text-red-900">Could not load your accounts</h2>
              <p className="mt-1 text-sm text-red-800">Check your connection and try again.</p>
              <button type="button" className="btn btn-ghost mt-4" onClick={() => refetch()}>
                Retry
              </button>
            </section>
          )}

          {!isLoading && !isError && activeAccountCount < 2 && (
            <section className="rounded-md border border-slate-200 bg-white p-6 shadow-sm">
              <h2 className="text-lg font-semibold text-slate-950">
                You need two active accounts to make a transfer
              </h2>
              <p className="mt-2 text-sm text-slate-600">
                Open another account or choose from active accounts once they are available.
              </p>
              <button type="button" className="btn btn-primary mt-5" onClick={() => navigate('/dashboard')}>
                Back to accounts
              </button>
            </section>
          )}

          {!isLoading && !isError && activeAccountCount >= 2 && (
            <TransferForm accounts={accounts} onSuccess={setResult} />
          )}
        </>
      )}
    </main>
  );
}

function TransferSkeleton() {
  return (
    <section className="grid gap-6 lg:grid-cols-[minmax(0,1fr)_360px]" aria-busy="true" aria-label="Loading transfer form">
      <div className="space-y-5">
        <div className="h-12 animate-pulse rounded-md bg-slate-200" />
        <div className="h-12 animate-pulse rounded-md bg-slate-200" />
        <div className="h-12 animate-pulse rounded-md bg-slate-200" />
        <div className="h-12 animate-pulse rounded-md bg-slate-200" />
      </div>
      <div className="h-56 animate-pulse rounded-md bg-slate-100" />
    </section>
  );
}
