// Story: US-005 | US-007 | US-010 | US-011
import { Link } from 'react-router-dom';
import { AccountList } from '@/features/accounts/AccountList';

/**
 * DashboardPage — authenticated landing page.
 *
 * US-005: Entry point after OTP verification; links to Profile.
 * US-007: Embeds AccountList to display the customer's accounts and balances.
 */
export default function DashboardPage() {
  return (
    <main
      className="dashboard"
      aria-labelledby="dashboard-heading"
    >
      {/* ── Top bar ──────────────────────────────────────────────────────── */}
      <header className="dashboard-header">
        <div className="dashboard-brand">
          <svg
            aria-hidden="true"
            className="dashboard-brand__icon"
            fill="none"
            stroke="currentColor"
            strokeWidth={2}
            viewBox="0 0 24 24"
          >
            <path
              strokeLinecap="round"
              strokeLinejoin="round"
              d="M12 3L2 9h20L12 3ZM4 9v9m4-9v9m4-9v9m4-9v9M2 18h20"
            />
          </svg>
          <span className="dashboard-brand__name">NorthBank</span>
        </div>

        <nav aria-label="Primary navigation">
          <div className="flex items-center gap-2">
            <Link
              to="/transfer"
              className="btn btn-primary btn--sm"
              aria-label="Transfer money between your accounts"
            >
              Transfer
            </Link>
            <Link
              to="/transactions"
              className="btn btn-ghost btn--sm"
              aria-label="View transaction history"
            >
              History
            </Link>
            <Link
              to="/profile"
              className="btn btn-ghost btn--sm"
              aria-label="View and update your profile"
            >
              My Profile
            </Link>
          </div>
        </nav>
      </header>

      {/* ── Page content ─────────────────────────────────────────────────── */}
      <div className="dashboard-content">
        <h1 id="dashboard-heading" className="dashboard-welcome">
          Welcome back
        </h1>
        <p className="dashboard-subtitle">
          Here’s an overview of your accounts.
        </p>

        {/* US-007: Account list with live balances */}
        <AccountList />
      </div>
    </main>
  );
}
