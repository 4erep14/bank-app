// Story: US-019
import { useMemo, useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { deactivateCustomer, getAdminCustomers, unlockCustomer } from '../api/adminApi';
import type { CustomerStatus } from '../types/admin.types';

const customerStatuses: CustomerStatus[] = ['ACTIVE', 'LOCKED', 'INACTIVE'];

export default function AdminCustomersPage() {
  const queryClient = useQueryClient();
  const [status, setStatus] = useState<CustomerStatus | ''>('');
  const [page, setPage] = useState(0);
  const filters = useMemo(() => ({ status, page, size: 20 }), [status, page]);

  const { data, isLoading, isError, refetch } = useQuery({
    queryKey: ['admin-customers', filters],
    queryFn: () => getAdminCustomers(filters),
  });

  const deactivateMutation = useMutation({
    mutationFn: deactivateCustomer,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['admin-customers'] }),
  });

  const unlockMutation = useMutation({
    mutationFn: unlockCustomer,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['admin-customers'] }),
  });

  const pageMeta = normalizePage(data);

  return (
    <main className="mx-auto max-w-7xl px-4 py-8 sm:px-6 lg:px-8" aria-labelledby="admin-customers-heading">
      <div className="mb-6 flex flex-wrap items-center justify-between gap-3">
        <div>
          <p className="text-sm font-semibold uppercase tracking-normal text-slate-500">Admin</p>
          <h1 id="admin-customers-heading" className="text-3xl font-semibold text-slate-950">Customer accounts</h1>
        </div>
        <div className="flex gap-2">
          <Link to="/admin/audit-logs" className="btn btn-ghost btn--sm">Audit log</Link>
          <Link to="/dashboard" className="btn btn-ghost btn--sm">Dashboard</Link>
        </div>
      </div>

      <section className="mb-5 flex flex-wrap items-end gap-3" aria-label="Customer filters">
        <label className="text-sm font-medium text-slate-700">
          Status
          <select
            className="mt-1 block w-56 rounded-md border border-slate-300 bg-white px-3 py-2 text-sm"
            value={status}
            onChange={(event) => {
              setStatus(event.target.value as CustomerStatus | '');
              setPage(0);
            }}
          >
            <option value="">All statuses</option>
            {customerStatuses.map((value) => (
              <option key={value} value={value}>{formatLabel(value)}</option>
            ))}
          </select>
        </label>
      </section>

      {isLoading && <div className="h-64 animate-pulse rounded-md bg-slate-100" aria-label="Loading customers" />}
      {isError && (
        <section className="rounded-md border border-red-200 bg-red-50 p-4" role="alert">
          <h2 className="text-base font-semibold text-red-900">Could not load customers</h2>
          <p className="mt-1 text-sm text-red-800">Admin access may be required.</p>
          <button type="button" className="btn btn-ghost mt-4" onClick={() => refetch()}>Retry</button>
        </section>
      )}

      {data && (
        <>
          <div className="overflow-hidden rounded-md border border-slate-200 bg-white shadow-sm">
            <table className="min-w-full divide-y divide-slate-200 text-sm">
              <thead className="bg-slate-50 text-left text-xs font-semibold uppercase tracking-normal text-slate-600">
                <tr>
                  <th className="px-4 py-3">Name</th>
                  <th className="px-4 py-3">Email</th>
                  <th className="px-4 py-3">Phone</th>
                  <th className="px-4 py-3">Status</th>
                  <th className="px-4 py-3">Created</th>
                  <th className="px-4 py-3 text-right">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {data.content.length === 0 && (
                  <tr>
                    <td colSpan={6} className="px-4 py-8 text-center text-slate-500">No customers match the current filters.</td>
                  </tr>
                )}
                {data.content.map((customer) => (
                  <tr key={customer.id}>
                    <td className="px-4 py-3 font-semibold text-slate-950">{customer.fullName}</td>
                    <td className="px-4 py-3 text-slate-700">{customer.email}</td>
                    <td className="px-4 py-3 font-mono text-xs text-slate-700">{customer.phone}</td>
                    <td className="px-4 py-3">
                      <span className="rounded-full bg-slate-100 px-2.5 py-1 text-xs font-semibold text-slate-700">
                        {formatLabel(customer.status)}
                      </span>
                    </td>
                    <td className="px-4 py-3 text-slate-600">{formatDate(customer.createdAt)}</td>
                    <td className="space-x-2 px-4 py-3 text-right">
                      {customer.status !== 'INACTIVE' && (
                        <button
                          type="button"
                          className="font-semibold text-red-700"
                          disabled={deactivateMutation.isPending}
                          onClick={() => deactivateMutation.mutate(customer.id)}
                        >
                          Deactivate
                        </button>
                      )}
                      {customer.status === 'LOCKED' && (
                        <button
                          type="button"
                          className="font-semibold text-blue-700"
                          disabled={unlockMutation.isPending}
                          onClick={() => unlockMutation.mutate(customer.id)}
                        >
                          Unlock
                        </button>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          <div className="mt-4 flex items-center justify-between text-sm text-slate-600">
            <span>Page {pageMeta.number + 1} of {Math.max(pageMeta.totalPages, 1)} ({pageMeta.totalElements} total)</span>
            <div className="flex gap-2">
              <button type="button" className="btn btn-ghost btn--sm" disabled={pageMeta.number === 0} onClick={() => setPage((value) => Math.max(0, value - 1))}>Previous</button>
              <button type="button" className="btn btn-ghost btn--sm" disabled={pageMeta.number + 1 >= pageMeta.totalPages} onClick={() => setPage((value) => value + 1)}>Next</button>
            </div>
          </div>
        </>
      )}
    </main>
  );
}

function normalizePage(data?: { number?: number; totalPages?: number; totalElements?: number; page?: { number: number; totalPages: number; totalElements: number } }) {
  return {
    number: data?.number ?? data?.page?.number ?? 0,
    totalPages: data?.totalPages ?? data?.page?.totalPages ?? 1,
    totalElements: data?.totalElements ?? data?.page?.totalElements ?? 0,
  };
}

function formatDate(value: string) {
  return new Intl.DateTimeFormat('en-US', { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(value));
}

function formatLabel(value: string) {
  return value.replace(/_/g, ' ').toLowerCase().replace(/\b\w/g, (letter: string) => letter.toUpperCase());
}
