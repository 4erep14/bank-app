// Story: US-008
import { useParams, useNavigate } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { getAccountDetail } from '@/api/accounts';
import type { AccountDetailResponse } from '@/types/account';

/**
 * Account Detail Page — US-008
 *
 * Renders full details for a single account belonging to the
 * authenticated customer. Handles:
 *   - Loading → skeleton
 *   - 403     → access denied message + back button
 *   - 404     → not found message + back button
 *   - 200     → account detail card
 */
export function AccountDetailPage() {
  const { accountId } = useParams<{ accountId: string }>();
  const navigate = useNavigate();

  const { data, isLoading, isError, error } = useQuery<AccountDetailResponse, ApiError>({
    queryKey: ['account', accountId],
    queryFn: () => getAccountDetail(accountId!),
    retry: false,
    enabled: !!accountId,
  });

  if (isLoading) return <AccountDetailSkeleton />;

  if (isError) {
    const status = (error as ApiError)?.status;
    return (
      <div className="empty-state">
        {status === 403 && (
          <>
            <h3>Access Denied</h3>
            <p>You don&apos;t have permission to view this account.</p>
          </>
        )}
        {status === 404 && (
          <>
            <h3>Account Not Found</h3>
            <p>This account doesn&apos;t exist or may have been removed.</p>
          </>
        )}
        {status !== 403 && status !== 404 && (
          <>
            <h3>Something went wrong</h3>
            <p>Unable to load account details. Please try again.</p>
          </>
        )}
        <button className="btn btn-ghost" onClick={() => navigate('/dashboard')}>
          Back to dashboard
        </button>
      </div>
    );
  }

  const currencyFmt = new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency: 'USD',
  });

  const dateFmt = new Intl.DateTimeFormat('en-US', {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
  });

  return (
    <main className="page-content">
      <button
        className="btn btn-ghost back-btn"
        onClick={() => navigate('/dashboard')}
        aria-label="Back to accounts list"
      >
        Accounts
      </button>

      <div className="account-detail-card" role="region" aria-label="Account details">
        <header className="account-detail-header">
          <span className={`badge badge-${data!.type.toLowerCase()}`}>
            {data!.type === 'CHECKING' ? 'Checking Account' : 'Savings Account'}
          </span>
          <span
            className={`status-pill status-${data!.status.toLowerCase()}`}
            role="status"
            aria-label={`Account status: ${data!.status}`}
          >
            {data!.status}
          </span>
        </header>

        <div className="account-detail-balance">
          <span className="balance-label">Current Balance</span>
          <span className="balance-amount" aria-label={`Balance: ${currencyFmt.format(Number(data!.balance))}`}>
            {currencyFmt.format(Number(data!.balance))}
          </span>
        </div>

        <dl className="account-detail-meta">
          <div className="meta-row">
            <dt>Account Number</dt>
            <dd>{data!.accountNumber}</dd>
          </div>
          <div className="meta-row">
            <dt>Account ID</dt>
            <dd className="meta-id">{data!.id}</dd>
          </div>
          <div className="meta-row">
            <dt>Opened On</dt>
            <dd>{dateFmt.format(new Date(data!.createdAt))}</dd>
          </div>
        </dl>
      </div>
    </main>
  );
}

function AccountDetailSkeleton() {
  return (
    <div className="account-detail-card skeleton-wrapper" aria-busy="true" aria-label="Loading account details">
      <div className="skeleton skeleton-heading" style={{ width: '40%' }} />
      <div className="skeleton" style={{ height: '3rem', width: '60%', margin: '1.5rem 0' }} />
      <div className="skeleton skeleton-text" />
      <div className="skeleton skeleton-text" style={{ width: '70%' }} />
      <div className="skeleton skeleton-text" style={{ width: '50%' }} />
    </div>
  );
}

interface ApiError extends Error {
  status?: number;
}
