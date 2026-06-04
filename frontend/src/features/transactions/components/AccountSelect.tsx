// Story: US-010
import type { AccountSummaryItem } from '@/types/account';

interface AccountSelectProps {
  id: string;
  label: string;
  accounts: AccountSummaryItem[];
  value: string;
  excludedAccountId?: string;
  error?: string;
  onChange: (value: string) => void;
}

const currencyFormatter = new Intl.NumberFormat('en-US', {
  style: 'currency',
  currency: 'USD',
});

export function AccountSelect({
  id,
  label,
  accounts,
  value,
  excludedAccountId,
  error,
  onChange,
}: AccountSelectProps) {
  return (
    <div className="space-y-2">
      <label htmlFor={id} className="block text-sm font-medium text-slate-800">
        {label}
      </label>
      <select
        id={id}
        value={value}
        onChange={(event) => onChange(event.target.value)}
        aria-describedby={error ? `${id}-error` : undefined}
        className="w-full rounded-md border border-slate-300 bg-white px-3 py-3 text-sm text-slate-900 shadow-sm focus:border-blue-600 focus:outline-none focus:ring-2 focus:ring-blue-100"
      >
        <option value="">Choose account</option>
        {accounts.map((account) => {
          const disabled = account.status !== 'ACTIVE' || account.id === excludedAccountId;
          const balance = currencyFormatter.format(Number(account.balance));
          const labelText = `${formatType(account.type)} •••• ${account.accountNumber.slice(-4)} · ${balance}`;
          return (
            <option key={account.id} value={account.id} disabled={disabled}>
              {disabled && account.status !== 'ACTIVE' ? `${labelText} (${account.status})` : labelText}
            </option>
          );
        })}
      </select>
      {error && (
        <p id={`${id}-error`} className="text-sm text-red-700" role="alert">
          {error}
        </p>
      )}
    </div>
  );
}

function formatType(type: string) {
  return type.charAt(0) + type.slice(1).toLowerCase();
}
