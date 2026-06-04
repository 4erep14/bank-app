// Story: US-020
import { useMemo, useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { getAuditLogs } from '../api/adminApi';
import type { AuditActionType } from '../types/admin.types';

const actionTypes: AuditActionType[] = [
  'LOGIN_SUCCESS',
  'LOGIN_FAILURE',
  'ACCOUNT_OPENED',
  'ACCOUNT_DEACTIVATED',
  'ACCOUNT_ACTIVATED',
  'TRANSFER_SUBMITTED',
  'TRANSFER_BLOCKED',
  'TRANSFER_COMPLETED',
  'FRAUD_RULE_CREATED',
  'FRAUD_RULE_UPDATED',
  'FRAUD_RULE_DELETED',
  'TRANSACTION_UNBLOCKED',
  'TRANSACTION_REJECTED',
  'CUSTOMER_DEACTIVATED',
  'CUSTOMER_UNLOCKED',
];

export default function AdminAuditLogsPage() {
  const [actorId, setActorId] = useState('');
  const [actionType, setActionType] = useState<AuditActionType | ''>('');
  const [dateFrom, setDateFrom] = useState('');
  const [dateTo, setDateTo] = useState('');
  const [page, setPage] = useState(0);
  const filters = useMemo(() => ({
    actorId: actorId.trim(),
    actionType,
    dateFrom: toOffsetDateTime(dateFrom),
    dateTo: toOffsetDateTime(dateTo),
    page,
    size: 50,
  }), [actorId, actionType, dateFrom, dateTo, page]);

  const { data, isLoading, isError, refetch } = useQuery({
    queryKey: ['admin-audit-logs', filters],
    queryFn: () => getAuditLogs(filters),
  });

  const pageMeta = normalizePage(data);

  return (
    <main className="mx-auto max-w-7xl px-4 py-8 sm:px-6 lg:px-8" aria-labelledby="audit-log-heading">
      <div className="mb-6 flex flex-wrap items-center justify-between gap-3">
        <div>
          <p className="text-sm font-semibold uppercase tracking-normal text-slate-500">Admin</p>
          <h1 id="audit-log-heading" className="text-3xl font-semibold text-slate-950">Audit log</h1>
        </div>
        <div className="flex gap-2">
          <Link to="/admin/customers" className="btn btn-ghost btn--sm">Customers</Link>
          <Link to="/dashboard" className="btn btn-ghost btn--sm">Dashboard</Link>
        </div>
      </div>

      <section className="mb-5 grid gap-3 md:grid-cols-[minmax(260px,1fr)_220px_190px_190px]" aria-label="Audit log filters">
        <label className="text-sm font-medium text-slate-700">
          Actor ID
          <input
            className="mt-1 block w-full rounded-md border border-slate-300 px-3 py-2 text-sm"
            value={actorId}
            onChange={(event) => {
              setActorId(event.target.value);
              setPage(0);
            }}
          />
        </label>
        <label className="text-sm font-medium text-slate-700">
          Action
          <select
            className="mt-1 block w-full rounded-md border border-slate-300 bg-white px-3 py-2 text-sm"
            value={actionType}
            onChange={(event) => {
              setActionType(event.target.value as AuditActionType | '');
              setPage(0);
            }}
          >
            <option value="">All actions</option>
            {actionTypes.map((value) => (
              <option key={value} value={value}>{formatLabel(value)}</option>
            ))}
          </select>
        </label>
        <label className="text-sm font-medium text-slate-700">
          From
          <input
            type="datetime-local"
            className="mt-1 block w-full rounded-md border border-slate-300 px-3 py-2 text-sm"
            value={dateFrom}
            onChange={(event) => {
              setDateFrom(event.target.value);
              setPage(0);
            }}
          />
        </label>
        <label className="text-sm font-medium text-slate-700">
          To
          <input
            type="datetime-local"
            className="mt-1 block w-full rounded-md border border-slate-300 px-3 py-2 text-sm"
            value={dateTo}
            onChange={(event) => {
              setDateTo(event.target.value);
              setPage(0);
            }}
          />
        </label>
      </section>

      {isLoading && <div className="h-64 animate-pulse rounded-md bg-slate-100" aria-label="Loading audit log" />}
      {isError && (
        <section className="rounded-md border border-red-200 bg-red-50 p-4" role="alert">
          <h2 className="text-base font-semibold text-red-900">Could not load audit log</h2>
          <p className="mt-1 text-sm text-red-800">Check the filter values and admin permissions.</p>
          <button type="button" className="btn btn-ghost mt-4" onClick={() => refetch()}>Retry</button>
        </section>
      )}

      {data && (
        <>
          <div className="overflow-hidden rounded-md border border-slate-200 bg-white shadow-sm">
            <table className="min-w-full divide-y divide-slate-200 text-sm">
              <thead className="bg-slate-50 text-left text-xs font-semibold uppercase tracking-normal text-slate-600">
                <tr>
                  <th className="px-4 py-3">Timestamp</th>
                  <th className="px-4 py-3">Actor</th>
                  <th className="px-4 py-3">Role</th>
                  <th className="px-4 py-3">Action</th>
                  <th className="px-4 py-3">Target</th>
                  <th className="px-4 py-3">IP</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {data.content.length === 0 && (
                  <tr>
                    <td colSpan={6} className="px-4 py-8 text-center text-slate-500">No audit entries match the current filters.</td>
                  </tr>
                )}
                {data.content.map((entry) => (
                  <tr key={entry.id}>
                    <td className="whitespace-nowrap px-4 py-3 text-slate-700">{formatDate(entry.timestamp)}</td>
                    <td className="max-w-[220px] truncate px-4 py-3 font-mono text-xs text-slate-700">{entry.actorId ?? 'SYSTEM'}</td>
                    <td className="px-4 py-3 text-slate-700">{entry.actorRole}</td>
                    <td className="px-4 py-3 font-semibold text-slate-950">{formatLabel(entry.actionType)}</td>
                    <td className="max-w-[260px] truncate px-4 py-3 font-mono text-xs text-slate-700">
                      {entry.targetEntityType}:{entry.targetEntityId ?? 'n/a'}
                    </td>
                    <td className="px-4 py-3 font-mono text-xs text-slate-600">{entry.ipAddress ?? 'n/a'}</td>
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

function toOffsetDateTime(value: string) {
  return value ? new Date(value).toISOString() : '';
}

function formatDate(value: string) {
  return new Intl.DateTimeFormat('en-US', { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(value));
}

function formatLabel(value: string) {
  return value.replace(/_/g, ' ').toLowerCase().replace(/\b\w/g, (letter: string) => letter.toUpperCase());
}
