// Story: US-014
import { FormEvent, useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { createFraudRule, deleteFraudRule, getFraudRules, updateFraudRule } from '../api/fraudApi';
import type { FraudRuleConditionType } from '../types/fraud.types';

const conditionTypes: FraudRuleConditionType[] = ['AMOUNT_EXCEEDS', 'TRANSACTION_FREQUENCY', 'UNUSUAL_HOUR'];

export default function FraudRulesPage() {
  const queryClient = useQueryClient();
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const { data, isLoading, isError } = useQuery({
    queryKey: ['fraud-rules'],
    queryFn: () => getFraudRules(),
  });

  const createMutation = useMutation({
    mutationFn: createFraudRule,
    onSuccess: () => {
      setErrorMessage(null);
      queryClient.invalidateQueries({ queryKey: ['fraud-rules'] });
    },
    onError: (error) => setErrorMessage(getProblemDetail(error, 'Could not create fraud rule')),
  });

  const updateMutation = useMutation({
    mutationFn: ({ id, active }: { id: string; active: boolean }) => updateFraudRule(id, { active }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['fraud-rules'] }),
    onError: (error) => setErrorMessage(getProblemDetail(error, 'Could not update fraud rule')),
  });

  const deleteMutation = useMutation({
    mutationFn: deleteFraudRule,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['fraud-rules'] }),
    onError: (error) => setErrorMessage(getProblemDetail(error, 'Could not delete fraud rule')),
  });

  function handleCreate(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const form = new FormData(event.currentTarget);
    createMutation.mutate({
      name: String(form.get('name') ?? ''),
      conditionType: String(form.get('conditionType') ?? 'AMOUNT_EXCEEDS') as FraudRuleConditionType,
      thresholdValue: String(form.get('thresholdValue') ?? ''),
      active: form.get('active') === 'on',
    });
    event.currentTarget.reset();
  }

  return (
    <main className="mx-auto max-w-6xl px-4 py-8 sm:px-6 lg:px-8" aria-labelledby="fraud-rules-heading">
      <div className="mb-6 flex flex-wrap items-center justify-between gap-3">
        <div>
          <p className="text-sm font-semibold uppercase tracking-normal text-slate-500">Fraud analyst</p>
          <h1 id="fraud-rules-heading" className="text-3xl font-semibold text-slate-950">Fraud rules</h1>
        </div>
        <Link to="/fraud/alerts" className="btn btn-ghost btn--sm">Alerts</Link>
      </div>

      {errorMessage && (
        <div className="mb-4 rounded-md border border-red-200 bg-red-50 p-3 text-sm text-red-800" role="alert">
          {errorMessage}
        </div>
      )}

      <form onSubmit={handleCreate} className="mb-6 grid gap-3 rounded-md border border-slate-200 bg-white p-4 shadow-sm md:grid-cols-[1fr_220px_180px_auto]">
        <label className="text-sm font-medium text-slate-700">
          Name
          <input name="name" required maxLength={120} className="mt-1 w-full rounded-md border border-slate-300 px-3 py-2 text-sm" />
        </label>
        <label className="text-sm font-medium text-slate-700">
          Condition
          <select name="conditionType" className="mt-1 w-full rounded-md border border-slate-300 px-3 py-2 text-sm">
            {conditionTypes.map((type) => <option key={type} value={type}>{type.replace('_', ' ')}</option>)}
          </select>
        </label>
        <label className="text-sm font-medium text-slate-700">
          Threshold
          <input name="thresholdValue" required maxLength={40} placeholder="1000 or 23:00-05:00" className="mt-1 w-full rounded-md border border-slate-300 px-3 py-2 text-sm" />
        </label>
        <label className="flex items-end gap-2 text-sm font-medium text-slate-700">
          <input name="active" type="checkbox" defaultChecked className="mb-3 h-4 w-4" />
          <span className="mb-2">Active</span>
        </label>
        <button type="submit" className="btn btn-primary btn--sm md:col-span-4" disabled={createMutation.isPending}>
          Add rule
        </button>
      </form>

      {isLoading && <div className="h-64 animate-pulse rounded-md bg-slate-100" />}
      {isError && <div className="rounded-md border border-red-200 bg-red-50 p-4 text-sm text-red-800">Could not load fraud rules.</div>}
      {data && (
        <div className="overflow-hidden rounded-md border border-slate-200 bg-white shadow-sm">
          <table className="min-w-full divide-y divide-slate-200 text-sm">
            <thead className="bg-slate-50 text-left text-xs font-semibold uppercase tracking-normal text-slate-600">
              <tr>
                <th className="px-4 py-3">Name</th>
                <th className="px-4 py-3">Condition</th>
                <th className="px-4 py-3">Threshold</th>
                <th className="px-4 py-3">Active</th>
                <th className="px-4 py-3 text-right">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {data.content.map((rule) => (
                <tr key={rule.id}>
                  <td className="px-4 py-3 font-semibold text-slate-950">{rule.name}</td>
                  <td className="px-4 py-3 text-slate-700">{rule.conditionType.replace('_', ' ')}</td>
                  <td className="px-4 py-3 font-mono text-xs text-slate-700">{rule.thresholdValue}</td>
                  <td className="px-4 py-3">{rule.active ? 'Yes' : 'No'}</td>
                  <td className="space-x-2 px-4 py-3 text-right">
                    <button className="font-semibold text-blue-700" onClick={() => updateMutation.mutate({ id: rule.id, active: !rule.active })}>
                      {rule.active ? 'Disable' : 'Enable'}
                    </button>
                    <button className="font-semibold text-red-700" onClick={() => deleteMutation.mutate(rule.id)}>
                      Delete
                    </button>
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

function getProblemDetail(error: unknown, fallback: string): string {
  if (typeof error === 'object' && error !== null && 'response' in error) {
    const response = (error as { response?: { data?: { detail?: string } } }).response;
    return response?.data?.detail ?? fallback;
  }
  return fallback;
}
