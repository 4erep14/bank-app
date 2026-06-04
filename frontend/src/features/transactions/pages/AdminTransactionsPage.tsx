// Story: US-013
import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { getAdminTransactions } from '../api/transactionsApi';
import { TransactionFiltersForm } from '../components/TransactionFilters';
import { TransactionTable } from '../components/TransactionTable';
import type { TransactionFilters } from '../types/transaction.types';

export default function AdminTransactionsPage() {
  const [filters, setFilters] = useState<TransactionFilters>({ page: 0, size: 50 });
  const { data, isLoading, isError, refetch } = useQuery({
    queryKey: ['admin-transactions', filters],
    queryFn: () => getAdminTransactions(filters),
    staleTime: 20_000,
  });

  return (
    <main className="mx-auto max-w-7xl px-4 py-8 sm:px-6 lg:px-8" aria-labelledby="admin-transactions-heading">
      <div className="mb-6 flex flex-wrap items-center justify-between gap-3">
        <div>
          <p className="text-sm font-semibold uppercase tracking-normal text-slate-500">Admin</p>
          <h1 id="admin-transactions-heading" className="text-3xl font-semibold text-slate-950">Transaction overview</h1>
        </div>
        <Link to="/dashboard" className="btn btn-ghost btn--sm">Dashboard</Link>
      </div>

      <TransactionFiltersForm filters={filters} showCustomer onChange={setFilters} />

      {isLoading && <div className="h-64 animate-pulse rounded-md bg-slate-100" aria-label="Loading admin transaction overview" />}
      {isError && (
        <section className="rounded-md border border-red-200 bg-red-50 p-4" role="alert">
          <h2 className="text-base font-semibold text-red-900">Could not load transaction overview</h2>
          <p className="mt-1 text-sm text-red-800">Admin access may be required.</p>
          <button type="button" className="btn btn-ghost mt-4" onClick={() => refetch()}>Retry</button>
        </section>
      )}
      {data && (
        <>
          <TransactionTable transactions={data.content} detailBasePath="/admin/transactions" showCustomer />
          <div className="mt-4 text-sm text-slate-600">
            Showing page {data.number + 1} of {Math.max(data.totalPages, 1)} ({data.totalElements} total)
          </div>
        </>
      )}
    </main>
  );
}
