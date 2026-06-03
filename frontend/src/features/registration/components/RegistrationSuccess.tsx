// Story: US-001
import { useLocation, useNavigate } from 'react-router-dom';
import type { RegistrationSuccessState } from '../types/registration.types';

/**
 * Confirmation screen shown after successful registration.
 * Receives firstName via router location state.
 * AC5: PENDING_VERIFICATION status is NOT surfaced to the user.
 */
export default function RegistrationSuccess() {
  const location = useLocation();
  const navigate = useNavigate();
  const state = location.state as Partial<RegistrationSuccessState> | null;
  const firstName = state?.firstName ?? 'there';

  return (
    <main
      className="flex min-h-screen items-center justify-center bg-gray-50 px-4 py-12"
      aria-labelledby="success-heading"
    >
      <div className="w-full max-w-md rounded-xl bg-white p-8 shadow-md text-center">
        {/* Success icon */}
        <div
          aria-hidden="true"
          className="mx-auto mb-4 flex h-14 w-14 items-center justify-center rounded-full bg-green-100"
        >
          <svg
            className="h-8 w-8 text-green-600"
            fill="none"
            stroke="currentColor"
            strokeWidth={2}
            viewBox="0 0 24 24"
          >
            <path
              strokeLinecap="round"
              strokeLinejoin="round"
              d="M4.5 12.75l6 6 9-13.5"
            />
          </svg>
        </div>

        <h1
          id="success-heading"
          className="mb-3 text-2xl font-bold text-gray-900"
        >
          Your account is ready
        </h1>

        <p className="mb-6 text-sm text-gray-600">
          Welcome to NorthBank, {firstName}. We&rsquo;ve created your account.
        </p>

        <button
          type="button"
          onClick={() => navigate('/login')}
          className="inline-flex items-center justify-center rounded-md bg-brand-600 px-5 py-2.5 text-sm font-semibold text-white shadow-sm transition hover:bg-brand-700 focus:outline-none focus:ring-2 focus:ring-brand-500 focus:ring-offset-2"
        >
          Continue to sign in
        </button>
      </div>
    </main>
  );
}
