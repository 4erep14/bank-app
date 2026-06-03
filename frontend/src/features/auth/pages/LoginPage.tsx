// Story: US-002
import LoginForm from '../components/LoginForm';

/**
 * Route container for /login.
 *
 * Renders the page chrome (NorthBank branding, heading, subtitle)
 * around LoginForm. Matches the card layout of RegistrationPage.
 */
export default function LoginPage() {
  return (
    <main
      className="flex min-h-screen items-start justify-center bg-gray-50 px-4 py-12 sm:items-center"
      aria-labelledby="login-heading"
    >
      <div className="w-full max-w-[440px] rounded-xl bg-white p-6 shadow-md sm:p-8">
        {/* ── NorthBank brand header ─────────────────────────────────────── */}
        <div className="mb-6 flex flex-col items-center gap-2">
          {/* Logo mark */}
          <div
            aria-hidden="true"
            className="flex h-10 w-10 items-center justify-center rounded-full bg-brand-600"
          >
            <svg
              className="h-6 w-6 text-white"
              fill="none"
              stroke="currentColor"
              strokeWidth={2}
              viewBox="0 0 24 24"
              aria-hidden="true"
            >
              {/* Building / bank icon */}
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                d="M12 3L2 9h20L12 3ZM4 9v9m4-9v9m4-9v9m4-9v9M2 18h20"
              />
            </svg>
          </div>

          {/* App name */}
          <span className="text-lg font-bold tracking-tight text-gray-900">
            NorthBank
          </span>
        </div>

        {/* ── Page heading ───────────────────────────────────────────────── */}
        <div className="mb-6 text-center">
          <h1
            id="login-heading"
            className="text-2xl font-bold text-gray-900"
          >
            Sign in to your account
          </h1>
          <p className="mt-1 text-sm text-gray-500">
            Enter your credentials to continue.
          </p>
        </div>

        {/* ── Form ───────────────────────────────────────────────────────── */}
        <LoginForm />
      </div>
    </main>
  );
}
