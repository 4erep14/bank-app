// Story: US-007
import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { getAccounts } from '@/api/accounts';
import { OpenAccountModal } from './OpenAccountModal';
import type { AccountSummaryItem } from '@/types/account';

const TYPE_LABEL: Record<string, string> = {
  CHECKING: 'Checking',
  SAVINGS:  'Savings',
};

const STATUS_CLASSES: Record<string, string> = {
  ACTIVE: 'badge badge--active',
  CLOSED: 'badge badge--closed',
  FROZEN: 'badge badge--frozen',
};

/**
 * US-007: Displays the authenticated customer's account list with balances.
 *
 * States handled:
 * - Loading   → shimmer skeleton (matches real card shape)
 * - Error     → inline error with retry
 * - Empty     → warm empty state with CTA to open first account (AC4)
 * - Populated → list of AccountCard items
 */
export function AccountList() {
  const [isModalOpen, setModalOpen] = useState(false);

  const { data: accounts = [], isLoading, isError, refetch } = useQuery({
    queryKey: ['accounts'],
    queryFn:  getAccounts,
    staleTime: 30_000,
  });

  // ── Loading skeleton ──────────────────────────────────────────────────────
  if (isLoading) {
    return (
      <section aria-label="Loading accounts" className="account-list">
        {[0, 1].map((n) => (
          <div key={n} className="account-card account-card--skeleton" aria-hidden="true">
            <div className="skeleton skeleton-text" style={{ width: '40%', height: '1rem', marginBottom: '0.75rem' }} />
            <div className="skeleton skeleton-text" style={{ width: '60%', height: '0.875rem', marginBottom: '0.5rem' }} />
            <div className="skeleton skeleton-text" style={{ width: '35%', height: '1.5rem' }} />
          </div>
        ))}
      </section>
    );
  }

  // ── Error state ───────────────────────────────────────────────────────────
  if (isError) {
    return (
      <div className="empty-state" role="alert">
        <svg
          aria-hidden="true"
          className="empty-state__icon"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          strokeWidth={1.5}
        >
          <path strokeLinecap="round" strokeLinejoin="round"
            d="M12 9v3.75m-9.303 3.376c-.866 1.5.217 3.374 1.948 3.374h14.71c1.73 0 2.813-1.874 1.948-3.374L13.949 3.378c-.866-1.5-3.032-1.5-3.898 0L2.697 16.126ZM12 15.75h.007v.008H12v-.008Z" />
        </svg>
        <h3 className="empty-state__title">Could not load accounts</h3>
        <p className="empty-state__desc">Check your connection and try again.</p>
        <button className="btn btn-ghost" onClick={() => refetch()}>
          Retry
        </button>
      </div>
    );
  }

  // ── AC4: Empty state ──────────────────────────────────────────────────────
  if (accounts.length === 0) {
    return (
      <>
        <div className="empty-state">
          <svg
            aria-hidden="true"
            className="empty-state__icon"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            strokeWidth={1.5}
          >
            <path strokeLinecap="round" strokeLinejoin="round"
              d="M12 3L2 9h20L12 3ZM4 9v9m4-9v9m4-9v9m4-9v9M2 18h20" />
          </svg>
          <h3 className="empty-state__title">No accounts yet</h3>
          <p className="empty-state__desc">
            Open a Checking or Savings account to get started.
          </p>
          <button
            className="btn btn-primary"
            onClick={() => setModalOpen(true)}
          >
            Open an Account
          </button>
        </div>

        {isModalOpen && (
          <OpenAccountModal onClose={() => setModalOpen(false)} />
        )}
      </>
    );
  }

  // ── Populated list ────────────────────────────────────────────────────────
  return (
    <>
      <section aria-labelledby="accounts-heading" className="account-list-section">
        <div className="account-list-header">
          <h2 id="accounts-heading" className="account-list__title">
            My Accounts
          </h2>
          <button
            className="btn btn-primary btn--sm"
            onClick={() => setModalOpen(true)}
            aria-label="Open a new bank account"
          >
            + Open Account
          </button>
        </div>

        <ul className="account-list" role="list">
          {accounts.map((account) => (
            <AccountCard key={account.accountNumber} account={account} />
          ))}
        </ul>
      </section>

      {isModalOpen && (
        <OpenAccountModal onClose={() => setModalOpen(false)} />
      )}
    </>
  );
}

// ── AccountCard ───────────────────────────────────────────────────────────────

function AccountCard({ account }: { account: AccountSummaryItem }) {
  // AC5: locale-aware currency format, always 2 decimal places
  const formattedBalance = new Intl.NumberFormat('en-US', {
    style:                 'currency',
    currency:              'USD',
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  }).format(account.balance);

  return (
    <li className="account-card">
      <div className="account-card__header">
        <span className="account-card__type">
          {TYPE_LABEL[account.type] ?? account.type}
        </span>
        <span className={STATUS_CLASSES[account.status] ?? 'badge'}>
          {account.status.charAt(0) + account.status.slice(1).toLowerCase()}
        </span>
      </div>

      <p className="account-card__number" aria-label="Account number (last 4 digits shown)">
        &bull;&bull;&bull;&bull; {account.accountNumber.slice(-4)}
      </p>

      <p className="account-card__balance" aria-label="Current balance">
        {formattedBalance}
      </p>
    </li>
  );
}
