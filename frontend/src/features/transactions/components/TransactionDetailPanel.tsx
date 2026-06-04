// Story: US-012 | US-013
import type { TransactionDetail } from '../types/transaction.types';

interface Props {
  transaction: TransactionDetail;
  showCustomer?: boolean;
}

const currencyFormatter = new Intl.NumberFormat('en-US', {
  style: 'currency',
  currency: 'USD',
  minimumFractionDigits: 2,
  maximumFractionDigits: 2,
});

const dateFormatter = new Intl.DateTimeFormat('en-US', {
  dateStyle: 'medium',
  timeStyle: 'short',
});

export function TransactionDetailPanel({ transaction, showCustomer = false }: Props) {
  return (
    <section className="rounded-md border border-slate-200 bg-white p-6 shadow-sm" aria-label="Transaction details">
      <div className="flex flex-wrap items-start justify-between gap-4 border-b border-slate-100 pb-5">
        <div>
          <p className="text-sm font-medium text-slate-600">Transaction</p>
          <h1 className="mt-1 text-2xl font-semibold text-slate-950">{currencyFormatter.format(Number(transaction.amount))}</h1>
          <p className="mt-1 text-sm text-slate-600">{transaction.description || 'Internal transfer'}</p>
        </div>
        <span className="rounded-full bg-slate-100 px-3 py-1 text-sm font-semibold text-slate-800">
          {transaction.status.replace('_', ' ')}
        </span>
      </div>

      <dl className="mt-5 grid gap-4 md:grid-cols-2">
        {showCustomer && <Detail label="Customer ID" value={transaction.customerId} mono />}
        <Detail label="Transaction ID" value={transaction.id} mono />
        <Detail label="Type" value={transaction.type} />
        <Detail label="Timestamp" value={dateFormatter.format(new Date(transaction.timestamp))} />
        <Detail label="Source Account" value={transaction.sourceAccountId} mono />
        <Detail label="Destination Account" value={transaction.destinationAccountId} mono />
        <Detail label="Created" value={dateFormatter.format(new Date(transaction.createdAt))} />
        <Detail label="Updated" value={dateFormatter.format(new Date(transaction.updatedAt))} />
      </dl>
    </section>
  );
}

function Detail({ label, value, mono = false }: { label: string; value: string; mono?: boolean }) {
  return (
    <div>
      <dt className="text-xs font-semibold uppercase tracking-normal text-slate-500">{label}</dt>
      <dd className={`mt-1 break-words text-sm text-slate-900 ${mono ? 'font-mono' : ''}`}>{value}</dd>
    </div>
  );
}
