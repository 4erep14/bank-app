// Story: US-006
import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { openAccount } from '@/api/accounts';
import type { AccountType } from '@/types/account';

interface Props {
  onClose: () => void;
}

const ACCOUNT_TYPES: { value: AccountType; label: string; description: string }[] = [
  {
    value: 'CHECKING',
    label: 'Checking Account',
    description: 'For everyday spending and bill payments',
  },
  {
    value: 'SAVINGS',
    label: 'Savings Account',
    description: 'Grow your funds with interest',
  },
];

/**
 * Modal dialog for US-006: Open a New Bank Account.
 *
 * The customer selects CHECKING or SAVINGS and confirms.
 * On success, the accounts query is invalidated so the account list refreshes automatically.
 * On 409, a friendly message is shown without crashing the form.
 */
export function OpenAccountModal({ onClose }: Props) {
  const [selected, setSelected] = useState<AccountType | null>(null);
  const queryClient = useQueryClient();

  const mutation = useMutation({
    mutationFn: (type: AccountType) => openAccount(type),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['accounts'] });
      onClose();
    },
  });

  const isDuplicate =
    mutation.isError &&
    (mutation.error as Error).message === 'Account of this type already exists';

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!selected) return;
    mutation.mutate(selected);
  };

  return (
    <div
      className="modal-overlay"
      role="dialog"
      aria-modal="true"
      aria-labelledby="open-account-title"
    >
      <div className="modal">
        {/* ── Header ── */}
        <header className="modal-header">
          <h2 id="open-account-title" className="modal-title">
            Open a New Account
          </h2>
          <button
            type="button"
            className="btn-icon"
            onClick={onClose}
            aria-label="Close dialog"
          >
            &times;
          </button>
        </header>

        {/* ── Form ── */}
        <form onSubmit={handleSubmit} noValidate>
          <p className="modal-subtitle">
            Choose the type of account you’d like to open.
          </p>

          <div
            className="account-type-grid"
            role="radiogroup"
            aria-label="Select account type"
          >
            {ACCOUNT_TYPES.map(({ value, label, description }) => (
              <label
                key={value}
                className={`account-type-card${
                  selected === value ? ' account-type-card--selected' : ''
                }`}
              >
                <input
                  type="radio"
                  name="accountType"
                  value={value}
                  checked={selected === value}
                  onChange={() => setSelected(value)}
                  className="sr-only"
                />
                <span className="account-type-label">{label}</span>
                <span className="account-type-desc">{description}</span>
              </label>
            ))}
          </div>

          {/* ── Error feedback ── */}
          {mutation.isError && (
            <p className="error-inline" role="alert">
              {isDuplicate
                ? `You already have a ${selected?.toLowerCase()} account.`
                : 'Something went wrong. Please try again.'}
            </p>
          )}

          {/* ── Actions ── */}
          <footer className="modal-footer">
            <button
              type="button"
              className="btn btn-ghost"
              onClick={onClose}
              disabled={mutation.isPending}
            >
              Cancel
            </button>
            <button
              type="submit"
              className="btn btn-primary"
              disabled={!selected || mutation.isPending}
            >
              {mutation.isPending ? 'Opening…' : 'Open Account'}
            </button>
          </footer>
        </form>
      </div>
    </div>
  );
}
