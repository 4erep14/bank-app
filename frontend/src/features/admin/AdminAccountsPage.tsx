// Story: US-009
import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { listAdminAccounts, deactivateAccount, activateAccount } from '@/api/adminAccounts';
import type { AdminAccountSummary, AccountStatus, AccountType } from '@/types/account';

/**
 * Admin Accounts Page — US-009
 *
 * Provides a paginated, filterable table of all customer accounts
 * with deactivate/activate actions per row.
 */
export function AdminAccountsPage() {
  const queryClient = useQueryClient();

  // Filter state (AC3)
  const [typeFilter,   setTypeFilter]   = useState<AccountType | ''>('');
  const [statusFilter, setStatusFilter] = useState<AccountStatus | ''>('');
  const [page, setPage] = useState(0);

  const queryKey = ['admin', 'accounts', typeFilter, statusFilter, page];

  const { data, isLoading, isError } = useQuery({
    queryKey,
    queryFn: () => listAdminAccounts({
      type:   typeFilter   || undefined,
      status: statusFilter || undefined,
      page,
      size:   20,
    }),
  });

  const deactivate = useMutation({
    mutationFn: (id: string) => deactivateAccount(id),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['admin', 'accounts'] }),
  });

  const activate = useMutation({
    mutationFn: (id: string) => activateAccount(id),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['admin', 'accounts'] }),
  });

  return (
    <main className="page-content">
      <header className="page-header">
        <h1 className="page-title">Account Management</h1>
        <p className="page-subtitle">View and manage all customer accounts across the platform.</p>
      </header>

      {/* Filters — AC3 */}
      <div className="filter-bar" role="search" aria-label="Account filters">
        <select
          value={typeFilter}
          onChange={e => { setTypeFilter(e.target.value as AccountType | ''); setPage(0); }}
          aria-label="Filter by account type"
          className="filter-select"
        >
          <option value="">All Types</option>
          <option value="CHECKING">Checking</option>
          <option value="SAVINGS">Savings</option>
        </select>

        <select
          value={statusFilter}
          onChange={e => { setStatusFilter(e.target.value as AccountStatus | ''); setPage(0); }}
          aria-label="Filter by account status"
          className="filter-select"
        >
          <option value="">All Statuses</option>
          <option value="ACTIVE">Active</option>
          <option value="INACTIVE">Inactive</option>
          <option value="FROZEN">Frozen</option>
          <option value="CLOSED">Closed</option>
        </select>
      </div>

      {/* Table */}
      {isLoading && <AdminTableSkeleton />}
      {isError   && <p className="error-inline" role="alert">Failed to load accounts.</p>}

      {data && (
        <>
          <div className="table-wrapper" role="region" aria-label="Account list">
            <table className="data-table">
              <thead>
                <tr>
                  <th scope="col">Account #</th>
                  <th scope="col">Type</th>
                  <th scope="col">Balance</th>
                  <th scope="col">Status</th>
                  <th scope="col">Owner</th>
                  <th scope="col">Email</th>
                  <th scope="col">Actions</th>
                </tr>
              </thead>
              <tbody>
                {data.content.length === 0 && (
                  <tr><td colSpan={7} className="empty-row">No accounts match the selected filters.</td></tr>
                )}
                {data.content.map((acc: AdminAccountSummary) => (
                  <tr key={acc.id}>
                    <td className="mono">{acc.accountNumber}</td>
                    <td><span className={`badge badge-${acc.type.toLowerCase()}`}>{acc.type}</span></td>
                    <td className="mono">
                      {new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(Number(acc.balance))}
                    </td>
                    <td>
                      <span className={`status-pill status-${acc.status.toLowerCase()}`}>
                        {acc.status}
                      </span>
                    </td>
                    <td>{acc.ownerFullName}</td>
                    <td className="email-cell">{acc.ownerEmail}</td>
                    <td className="actions-cell">
                      {acc.status === 'ACTIVE' && (
                        <button
                          className="btn btn-sm btn-warning"
                          onClick={() => deactivate.mutate(acc.id)}
                          disabled={deactivate.isPending}
                          aria-label={`Deactivate account ${acc.accountNumber}`}
                        >
                          Deactivate
                        </button>
                      )}
                      {acc.status === 'INACTIVE' && (
                        <button
                          className="btn btn-sm btn-primary"
                          onClick={() => activate.mutate(acc.id)}
                          disabled={activate.isPending}
                          aria-label={`Activate account ${acc.accountNumber}`}
                        >
                          Activate
                        </button>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          {/* Pagination */}
          <div className="pagination" role="navigation" aria-label="Pagination">
            <button
              className="btn btn-ghost"
              onClick={() => setPage(p => Math.max(0, p - 1))}
              disabled={page === 0}
              aria-label="Previous page"
            >
              ← Previous
            </button>
            <span className="pagination-info">
              Page {page + 1} of {data.page?.totalPages ?? 1}
            </span>
            <button
              className="btn btn-ghost"
              onClick={() => setPage(p => p + 1)}
              disabled={page + 1 >= (data.page?.totalPages ?? 1)}
              aria-label="Next page"
            >
              Next →
            </button>
          </div>
        </>
      )}
    </main>
  );
}

function AdminTableSkeleton() {
  return (
    <div className="skeleton-wrapper" aria-busy="true" aria-label="Loading accounts">
      {Array.from({ length: 5 }).map((_, i) => (
        <div key={i} className="skeleton skeleton-text" style={{ marginBottom: '0.75rem' }} />
      ))}
    </div>
  );
}
