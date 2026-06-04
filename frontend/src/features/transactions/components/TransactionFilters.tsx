// Story: US-011 | US-013
import type { FormEvent } from 'react';
import type { TransactionFilters, TransactionStatus } from '../types/transaction.types';

interface Props {
  filters: TransactionFilters;
  showCustomer?: boolean;
  onChange: (filters: TransactionFilters) => void;
}

const statuses: TransactionStatus[] = ['COMPLETED', 'BLOCKED', 'PENDING_EVALUATION'];

export function TransactionFiltersForm({ filters, showCustomer = false, onChange }: Props) {
  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const formData = new FormData(event.currentTarget);
    onChange({
      status: optional(formData.get('status')) as TransactionStatus | undefined,
      accountId: optional(formData.get('accountId')),
      customerId: showCustomer ? optional(formData.get('customerId')) : undefined,
      from: toIsoStart(optional(formData.get('from'))),
      to: toIsoEnd(optional(formData.get('to'))),
      page: 0,
      size: filters.size ?? 20,
    });
  }

  return (
    <form onSubmit={handleSubmit} className="mb-5 grid gap-3 rounded-md border border-slate-200 bg-white p-4 shadow-sm md:grid-cols-5">
      <label className="text-sm font-medium text-slate-700">
        Status
        <select name="status" defaultValue={filters.status ?? ''} className="mt-1 w-full rounded-md border border-slate-300 px-3 py-2 text-sm">
          <option value="">All</option>
          {statuses.map((status) => (
            <option key={status} value={status}>{status.replace('_', ' ')}</option>
          ))}
        </select>
      </label>

      <label className="text-sm font-medium text-slate-700">
        Account ID
        <input name="accountId" defaultValue={filters.accountId ?? ''} className="mt-1 w-full rounded-md border border-slate-300 px-3 py-2 text-sm" />
      </label>

      {showCustomer && (
        <label className="text-sm font-medium text-slate-700">
          Customer ID
          <input name="customerId" defaultValue={filters.customerId ?? ''} className="mt-1 w-full rounded-md border border-slate-300 px-3 py-2 text-sm" />
        </label>
      )}

      <label className="text-sm font-medium text-slate-700">
        From
        <input name="from" type="date" defaultValue={dateOnly(filters.from)} className="mt-1 w-full rounded-md border border-slate-300 px-3 py-2 text-sm" />
      </label>

      <label className="text-sm font-medium text-slate-700">
        To
        <input name="to" type="date" defaultValue={dateOnly(filters.to)} className="mt-1 w-full rounded-md border border-slate-300 px-3 py-2 text-sm" />
      </label>

      <div className="flex items-end gap-2 md:col-span-5">
        <button type="submit" className="btn btn-primary btn--sm">Apply</button>
        <button type="button" className="btn btn-ghost btn--sm" onClick={() => onChange({ page: 0, size: filters.size ?? 20 })}>
          Clear
        </button>
      </div>
    </form>
  );
}

function optional(value: FormDataEntryValue | null): string | undefined {
  return typeof value === 'string' && value.trim() ? value.trim() : undefined;
}

function toIsoStart(value?: string): string | undefined {
  return value ? `${value}T00:00:00Z` : undefined;
}

function toIsoEnd(value?: string): string | undefined {
  return value ? `${value}T23:59:59Z` : undefined;
}

function dateOnly(value?: string): string {
  return value ? value.slice(0, 10) : '';
}
