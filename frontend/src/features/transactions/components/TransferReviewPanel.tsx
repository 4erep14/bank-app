// Story: US-010
import type { AccountSummaryItem } from '@/types/account';

interface TransferReviewPanelProps {
  source?: AccountSummaryItem;
  destination?: AccountSummaryItem;
  amount: string;
  description?: string;
}

const currencyFormatter = new Intl.NumberFormat('en-US', {
  style: 'currency',
  currency: 'USD',
});

export function TransferReviewPanel({
  source,
  destination,
  amount,
  description,
}: TransferReviewPanelProps) {
  const numericAmount = Number(amount || 0);
  const canProject = source && destination && numericAmount > 0;

  return (
    <aside
      className="rounded-md border border-slate-200 bg-slate-50 p-4"
      aria-labelledby="transfer-review-heading"
    >
      <h2 id="transfer-review-heading" className="text-base font-semibold text-slate-900">
        Review
      </h2>
      <dl className="mt-4 space-y-3 text-sm">
        <ReviewRow label="From" value={source ? accountLabel(source) : 'Not selected'} />
        <ReviewRow label="To" value={destination ? accountLabel(destination) : 'Not selected'} />
        <ReviewRow
          label="Amount"
          value={numericAmount > 0 ? currencyFormatter.format(numericAmount) : '$0.00'}
        />
        {description && <ReviewRow label="Description" value={description} />}
      </dl>

      {canProject && (
        <div className="mt-4 border-t border-slate-200 pt-4 text-sm text-slate-700">
          <p>
            Source after transfer:{' '}
            <strong className="text-slate-950">
              {currencyFormatter.format(Number(source.balance) - numericAmount)}
            </strong>
          </p>
          <p className="mt-1">
            Destination after transfer:{' '}
            <strong className="text-slate-950">
              {currencyFormatter.format(Number(destination.balance) + numericAmount)}
            </strong>
          </p>
        </div>
      )}
    </aside>
  );
}

function ReviewRow({ label, value }: { label: string; value: string }) {
  return (
    <div className="flex justify-between gap-4">
      <dt className="text-slate-500">{label}</dt>
      <dd className="text-right font-medium text-slate-900">{value}</dd>
    </div>
  );
}

function accountLabel(account: AccountSummaryItem) {
  return `${account.type.charAt(0) + account.type.slice(1).toLowerCase()} •••• ${account.accountNumber.slice(-4)}`;
}
