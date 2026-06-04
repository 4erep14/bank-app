// Story: US-011 | US-013
import { Link } from 'react-router-dom';
import type { TransactionSummary } from '../types/transaction.types';

interface Props {
  transactions: TransactionSummary[];
  detailBasePath: string;
  showCustomer?: boolean;
}

const currencyFormatter = new Intl.NumberFormat('en-US', {
  style: 'currency',
  currency: 'USD',
  minimumFractionDigits: 2,
  maximumFractionDigits: 2,
});

const dateFormatter = new Intl.DateTimeFormat('en-US', {
  month: 'short',
  day: '2-digit',
  year: 'numeric',
  hour: '2-digit',
  minute: '2-digit',
});

const statusClasses: Record<string, string> = {
  COMPLETED: 'bg-emerald-100 text-emerald-800',
  BLOCKED: 'bg-red-100 text-red-800',
  PENDING_EVALUATION: 'bg-amber-100 text-amber-800',
};

export function TransactionTable({ transactions, detailBasePath, showCustomer = false }: Props) {
  if (transactions.length === 0) {
    return (
      <section className="rounded-md border border-slate-200 bg-white p-6 text-center shadow-sm">
        <h2 className="text-base font-semibold text-slate-950">No transactions found</h2>
        <p className="mt-1 text-sm text-slate-600">Adjust the filters or check back after new activity.</p>
      </section>
    );
  }

  return (
    <div className="overflow-hidden rounded-md border border-slate-200 bg-white shadow-sm">
      <div className="overflow-x-auto">
        <table className="min-w-full divide-y divide-slate-200 text-sm">
          <thead className="bg-slate-50 text-left text-xs font-semibold uppercase tracking-normal text-slate-600">
            <tr>
              <th scope="col" className="px-4 py-3">Date</th>
              <th scope="col" className="px-4 py-3">Type</th>
              <th scope="col" className="px-4 py-3">Amount</th>
              <th scope="col" className="px-4 py-3">Status</th>
              {showCustomer && <th scope="col" className="px-4 py-3">Customer</th>}
              <th scope="col" className="px-4 py-3">Description</th>
              <th scope="col" className="px-4 py-3 text-right">Details</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-100">
            {transactions.map((transaction) => (
              <tr key={transaction.id} className="hover:bg-slate-50">
                <td className="whitespace-nowrap px-4 py-3 text-slate-700">
                  {dateFormatter.format(new Date(transaction.timestamp))}
                </td>
                <td className="px-4 py-3 font-medium text-slate-950">{transaction.type}</td>
                <td className="whitespace-nowrap px-4 py-3 font-semibold text-slate-950">
                  {currencyFormatter.format(Number(transaction.amount))}
                </td>
                <td className="px-4 py-3">
                  <span className={`rounded-full px-2.5 py-1 text-xs font-semibold ${statusClasses[transaction.status] ?? 'bg-slate-100 text-slate-700'}`}>
                    {transaction.status.replace('_', ' ')}
                  </span>
                </td>
                {showCustomer && (
                  <td className="max-w-[12rem] truncate px-4 py-3 font-mono text-xs text-slate-600">
                    {transaction.customerId}
                  </td>
                )}
                <td className="max-w-[16rem] truncate px-4 py-3 text-slate-600">
                  {transaction.description || 'Internal transfer'}
                </td>
                <td className="px-4 py-3 text-right">
                  <Link className="font-semibold text-blue-700 hover:text-blue-900" to={`${detailBasePath}/${transaction.id}`}>
                    View
                  </Link>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}
