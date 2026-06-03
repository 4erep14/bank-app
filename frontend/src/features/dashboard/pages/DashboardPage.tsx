// Story: US-005
import { Link } from 'react-router-dom';

/**
 * DashboardPage — minimal authenticated landing page.
 *
 * US-003 (useVerifyOtp) navigates to /dashboard on successful OTP verification.
 * This page serves as the entry point for authenticated customers,
 * with a prominent link to the Profile page (US-005).
 */
export default function DashboardPage() {
  return (
    <main
      className="flex min-h-screen flex-col items-center justify-center bg-gray-50 px-4"
      aria-labelledby="dashboard-heading"
    >
      {/* NorthBank brand mark */}
      <div
        aria-hidden="true"
        className="mb-5 flex h-14 w-14 items-center justify-center rounded-full bg-brand-600"
      >
        <svg
          className="h-8 w-8 text-white"
          fill="none"
          stroke="currentColor"
          strokeWidth={2}
          viewBox="0 0 24 24"
          aria-hidden="true"
        >
          <path
            strokeLinecap="round"
            strokeLinejoin="round"
            d="M12 3L2 9h20L12 3ZM4 9v9m4-9v9m4-9v9m4-9v9M2 18h20"
          />
        </svg>
      </div>

      <h1
        id="dashboard-heading"
        className="text-2xl font-bold text-gray-900 sm:text-3xl"
      >
        Welcome to NorthBank
      </h1>

      <p className="mt-2 text-center text-sm text-gray-500">
        You're now securely signed in to your account.
      </p>

      <div className="mt-8 flex flex-col items-center gap-4 sm:flex-row">
        <Link
          to="/profile"
          aria-label="View and update your profile"
          className="inline-flex items-center gap-2 rounded-md bg-brand-600 px-5 py-2.5 text-sm font-semibold text-white shadow-sm transition hover:bg-brand-700 focus:outline-none focus:ring-2 focus:ring-brand-500 focus:ring-offset-2"
        >
          {/* User icon */}
          <svg
            aria-hidden="true"
            className="h-4 w-4"
            viewBox="0 0 20 20"
            fill="currentColor"
          >
            <path d="M10 8a3 3 0 1 0 0-6 3 3 0 0 0 0 6ZM3.465 14.493a1.23 1.23 0 0 0 .41 1.412A9.957 9.957 0 0 0 10 18c2.31 0 4.438-.784 6.131-2.1.43-.333.604-.903.408-1.41a7.002 7.002 0 0 0-13.074.003Z" />
          </svg>
          My Profile
        </Link>
      </div>
    </main>
  );
}
