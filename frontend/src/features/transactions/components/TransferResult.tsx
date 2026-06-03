// Story: US-010
import type { CreateTransferResponse } from '../types/transaction.types';

interface TransferResultProps {
  result: CreateTransferResponse;
  onReset: () => void;
  onDashboard: () => void;
}

export function TransferResult({ result, onReset, onDashboard }: TransferResultProps) {
  const isBlocked = result.status === 'BLOCKED';

  return (
    <section
      className="rounded-md border border-slate-200 bg-white p-6 shadow-sm"
      aria-labelledby="transfer-result-heading"
      tabIndex={-1}
    >
      <p className={`text-sm font-semibold ${isBlocked ? 'text-amber-700' : 'text-emerald-700'}`}>
        {isBlocked ? 'No money was moved' : 'Balances updated'}
      </p>
      <h1 id="transfer-result-heading" className="mt-2 text-2xl font-semibold text-slate-950">
        {isBlocked ? 'Transfer blocked for review' : 'Transfer completed'}
      </h1>
      <p className="mt-3 text-sm text-slate-600">
        Transaction ID: <span className="font-mono text-slate-900">{result.transactionId}</span>
      </p>
      <div className="mt-6 flex flex-wrap gap-3">
        {!isBlocked && (
          <button type="button" className="btn btn-primary" onClick={onReset}>
            Make another transfer
          </button>
        )}
        <button type="button" className="btn btn-ghost" onClick={onDashboard}>
          Back to dashboard
        </button>
      </div>
    </section>
  );
}
