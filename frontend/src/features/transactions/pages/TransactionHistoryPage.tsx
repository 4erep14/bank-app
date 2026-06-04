// Story: US-011
import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { getTransactions } from '../api/transactionsApi';
import { TransactionFiltersForm } from '../components/TransactionFilters';
import { TransactionTable } from '../components/TransactionTable';
import type { TransactionFilters } from '../types/transaction.types';

export default function TransactionHistoryPage() {
  const [filters, setFilters] = useState<TransactionFilters>({ page: 0, size: 20 });
  const { data, isLoading, isError, refetch } = useQuery({
    queryKey: ['transactions', filters],
    queryFn: () => getTransactions(filters),
    staleTime: 30_000,
  });

  return (
    <main className="mx-auto max-w-6xl px-4 py-8 sm:px-6 lg:px-8" aria-labelledby="transactions-heading">
      <div className="mb-6 flex flex-wrap items-center justify-between gap-3">
        <div>
          <h1 id="transactions-heading" className="text-3xl font-semibold text-slate-950">Transaction history</h1>
          <p className="mt-1 text-sm text-slate-600">Review transfers across your NorthBank accounts.</p>
        </div>
        <div className="flex gap-2">
          <Link to="/transfer" className="btn btn-primary btn--sm">Transfer</Link>
          <Link to="/dashboard" className="btn btn-ghost btn--sm">Dashboard</Link>
        </div>
      </div>

      <TransactionFiltersForm filters={filters} onChange={setFilters} />

      {isLoading && <HistorySkeleton />}
      {isError && <ErrorState onRetry={() => refetch()} />}
      {data && (
        <>
          <TransactionTable transactions={data.content} detailBasePath="/transactions" />
          <Pagination
            page={data.number}
            totalPages={data.totalPages}
            onPageChange={(page) => setFilters((current) => ({ ...current, page }))}
          />
        </>
      )}
    </main>
  );
}

function HistorySkeleton() {
  return (
    <section className="space-y-2" aria-busy="true" aria-label="Loading transaction history">
      {[0, 1, 2].map((item) => (
        <div key={item} className="h-14 animate-pulse rounded-md bg-slate-100" />
      ))}
    </section>
  );
}

function ErrorState({ onRetry }: { onRetry: () => void }) {
  return (
    <section className="rounded-md border border-red-200 bg-red-50 p-4" role="alert">
      <h2 className="text-base font-semibold text-red-900">Could not load transactions</h2>
      <p className="mt-1 text-sm text-red-800">Check your connection and try again.</p>
      <button type="button" className="btn btn-ghost mt-4" onClick={onRetry}>Retry</button>
    </section>
  );
}

function Pagination({ page, totalPages, onPageChange }: { page: number; totalPages: number; onPageChange: (page: number) => void }) {
  if (totalPages <= 1) return null;
  return (
    <nav className="mt-4 flex items-center justify-between" aria-label="Transaction history pages">
      <button className="btn btn-ghost btn--sm" disabled={page === 0} onClick={() => onPageChange(page - 1)}>
        Previous
      </button>
      <span className="text-sm text-slate-600">Page {page + 1} of {totalPages}</span>
      <button className="btn btn-ghost btn--sm" disabled={page >= totalPages - 1} onClick={() => onPageChange(page + 1)}>
        Next
      </button>
    </nav>
  );
}
