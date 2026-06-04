// Story: US-016
import { useQuery } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { getNotifications } from '../api/notificationsApi';

export default function NotificationsPage() {
  const { data, isLoading, isError } = useQuery({
    queryKey: ['notifications'],
    queryFn: () => getNotifications(),
  });

  return (
    <main className="mx-auto max-w-5xl px-4 py-8 sm:px-6 lg:px-8" aria-labelledby="notifications-heading">
      <div className="mb-6 flex flex-wrap items-center justify-between gap-3">
        <div>
          <h1 id="notifications-heading" className="text-3xl font-semibold text-slate-950">Notifications</h1>
          <p className="mt-1 text-sm text-slate-600">Fraud and account security messages for your profile.</p>
        </div>
        <Link to="/dashboard" className="btn btn-ghost btn--sm">Dashboard</Link>
      </div>

      {isLoading && <div className="h-52 animate-pulse rounded-md bg-slate-100" />}
      {isError && <div className="rounded-md border border-red-200 bg-red-50 p-4 text-sm text-red-800">Could not load notifications.</div>}
      {data && data.content.length === 0 && (
        <section className="rounded-md border border-slate-200 bg-white p-6 text-center shadow-sm">
          <h2 className="text-base font-semibold text-slate-950">No notifications</h2>
          <p className="mt-1 text-sm text-slate-600">You are all caught up.</p>
        </section>
      )}
      {data && data.content.length > 0 && (
        <ul className="space-y-3">
          {data.content.map((notification) => (
            <li key={notification.id} className="rounded-md border border-slate-200 bg-white p-4 shadow-sm">
              <div className="flex flex-wrap items-start justify-between gap-3">
                <div>
                  <p className="text-sm font-semibold text-slate-950">Transaction blocked</p>
                  <p className="mt-1 text-sm text-slate-600">
                    ${Number(notification.amount).toFixed(2)} was blocked by {notification.triggeredRuleName}.
                  </p>
                  <p className="mt-2 font-mono text-xs text-slate-500">{notification.transactionId}</p>
                </div>
                <span className="text-xs font-semibold uppercase tracking-normal text-slate-500">
                  {new Date(notification.timestamp).toLocaleString()}
                </span>
              </div>
            </li>
          ))}
        </ul>
      )}
    </main>
  );
}
