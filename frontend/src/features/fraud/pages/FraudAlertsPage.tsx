// Story: US-017
import { FormEvent, useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { getFraudAlerts } from '../api/fraudApi';
import type { FraudAlertFilters, FraudAlertReviewStatus, FraudRuleConditionType } from '../types/fraud.types';

const reviewStatuses: FraudAlertReviewStatus[] = ['PENDING_REVIEW', 'REVIEWED'];
const ruleTypes: FraudRuleConditionType[] = ['AMOUNT_EXCEEDS', 'TRANSACTION_FREQUENCY', 'UNUSUAL_HOUR'];

export default function FraudAlertsPage() {
  const [filters, setFilters] = useState<FraudAlertFilters>({ page: 0, size: 20 });
  const { data, isLoading, isError } = useQuery({
    queryKey: ['fraud-alerts', filters],
    queryFn: () => getFraudAlerts(filters),
  });

  function handleFilter(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const form = new FormData(event.currentTarget);
    setFilters({
      reviewStatus: optional(form.get('reviewStatus')) as FraudAlertReviewStatus | undefined,
      ruleConditionType: optional(form.get('ruleConditionType')) as FraudRuleConditionType | undefined,
      dateFrom: toIsoStart(optional(form.get('dateFrom'))),
      dateTo: toIsoEnd(optional(form.get('dateTo'))),
      page: 0,
      size: 20,
    });
  }

  return (
    <main className="mx-auto max-w-7xl px-4 py-8 sm:px-6 lg:px-8" aria-labelledby="fraud-alerts-heading">
      <div className="mb-6 flex flex-wrap items-center justify-between gap-3">
        <div>
          <p className="text-sm font-semibold uppercase tracking-normal text-slate-500">Fraud analyst</p>
          <h1 id="fraud-alerts-heading" className="text-3xl font-semibold text-slate-950">Fraud alerts</h1>
        </div>
        <Link to="/fraud/rules" className="btn btn-ghost btn--sm">Rules</Link>
      </div>

      <form onSubmit={handleFilter} className="mb-5 grid gap-3 rounded-md border border-slate-200 bg-white p-4 shadow-sm md:grid-cols-5">
        <label className="text-sm font-medium text-slate-700">
          Review
          <select name="reviewStatus" className="mt-1 w-full rounded-md border border-slate-300 px-3 py-2 text-sm">
            <option value="">All</option>
            {reviewStatuses.map((status) => <option key={status} value={status}>{status.replace('_', ' ')}</option>)}
          </select>
        </label>
        <label className="text-sm font-medium text-slate-700">
          Rule
          <select name="ruleConditionType" className="mt-1 w-full rounded-md border border-slate-300 px-3 py-2 text-sm">
            <option value="">All</option>
            {ruleTypes.map((type) => <option key={type} value={type}>{type.replace('_', ' ')}</option>)}
          </select>
        </label>
        <label className="text-sm font-medium text-slate-700">
          From
          <input name="dateFrom" type="date" className="mt-1 w-full rounded-md border border-slate-300 px-3 py-2 text-sm" />
        </label>
        <label className="text-sm font-medium text-slate-700">
          To
          <input name="dateTo" type="date" className="mt-1 w-full rounded-md border border-slate-300 px-3 py-2 text-sm" />
        </label>
        <div className="flex items-end gap-2">
          <button className="btn btn-primary btn--sm" type="submit">Apply</button>
          <button className="btn btn-ghost btn--sm" type="button" onClick={() => setFilters({ page: 0, size: 20 })}>Clear</button>
        </div>
      </form>

      {isLoading && <div className="h-64 animate-pulse rounded-md bg-slate-100" />}
      {isError && <div className="rounded-md border border-red-200 bg-red-50 p-4 text-sm text-red-800">Could not load fraud alerts.</div>}
      {data && (
        <div className="overflow-hidden rounded-md border border-slate-200 bg-white shadow-sm">
          <table className="min-w-full divide-y divide-slate-200 text-sm">
            <thead className="bg-slate-50 text-left text-xs font-semibold uppercase tracking-normal text-slate-600">
              <tr>
                <th className="px-4 py-3">Time</th>
                <th className="px-4 py-3">Customer</th>
                <th className="px-4 py-3">Account</th>
                <th className="px-4 py-3">Amount</th>
                <th className="px-4 py-3">Rule</th>
                <th className="px-4 py-3">Status</th>
                <th className="px-4 py-3 text-right">Details</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {data.content.map((alert) => (
                <tr key={alert.alertId}>
                  <td className="whitespace-nowrap px-4 py-3 text-slate-700">{new Date(alert.timestamp).toLocaleString()}</td>
                  <td className="px-4 py-3 font-semibold text-slate-950">{alert.customerFullName}</td>
                  <td className="px-4 py-3 font-mono text-xs text-slate-700">{alert.accountNumber}</td>
                  <td className="px-4 py-3 font-semibold text-slate-950">${Number(alert.amount).toFixed(2)}</td>
                  <td className="px-4 py-3 text-slate-700">{alert.triggeredRuleName}</td>
                  <td className="px-4 py-3">{alert.reviewStatus.replace('_', ' ')}</td>
                  <td className="px-4 py-3 text-right">
                    <Link className="font-semibold text-blue-700" to={`/fraud/alerts/${alert.alertId}`}>Review</Link>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </main>
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
