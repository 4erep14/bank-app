// Story: US-004
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { Link } from 'react-router-dom';

import { useForgotPassword } from '../hooks/useForgotPassword';
import TextField from '@/shared/components/TextField';
import SubmitButton from '@/shared/components/SubmitButton';
import FormErrorBanner from '@/shared/components/FormErrorBanner';

// ── Local Zod schema ──────────────────────────────────────────────────────────

const forgotPasswordSchema = z.object({
  email: z
    .string()
    .min(1, 'Email is required.')
    .email('Please enter a valid email address.'),
});

type ForgotPasswordFormValues = z.infer<typeof forgotPasswordSchema>;

// ── Page ──────────────────────────────────────────────────────────────────────

/**
 * ForgotPasswordPage — /forgot-password
 *
 * Screen 1 of the Password Reset flow (US-004).
 *
 * AC1: Submits { email } to POST /api/v1/auth/forgot-password.
 *      The API always returns 200 (anti-enumeration).
 * On success → navigates to /forgot-password/sent (handled by useForgotPassword).
 */
export default function ForgotPasswordPage() {
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<ForgotPasswordFormValues>({
    resolver: zodResolver(forgotPasswordSchema),
    mode: 'onBlur',
    reValidateMode: 'onChange',
  });

  const { submit, isPending, bannerError } = useForgotPassword();

  const loading = isSubmitting || isPending;

  const onSubmit = (data: ForgotPasswordFormValues) => {
    submit({ email: data.email });
  };

  return (
    <main
      className="flex min-h-screen items-start justify-center bg-gray-50 px-4 py-12 sm:items-center"
      aria-labelledby="forgot-password-heading"
    >
      <div className="w-full max-w-[440px] rounded-xl bg-white p-6 shadow-md sm:p-8">
        {/* ── NorthBank brand header ──────────────────────────────────────── */}
        <BrandHeader />

        {/* ── Page heading ────────────────────────────────────────────────── */}
        <div className="mb-6 text-center">
          <h1
            id="forgot-password-heading"
            className="text-2xl font-bold text-gray-900"
          >
            Forgot your password?
          </h1>
          <p className="mt-1 text-sm text-gray-500">
            Enter your email and we&rsquo;ll send you a reset link.
          </p>
        </div>

        {/* ── Form ────────────────────────────────────────────────────────── */}
        <form
          onSubmit={handleSubmit(onSubmit)}
          noValidate
          aria-busy={loading}
          aria-label="Forgot password form"
          className="flex flex-col gap-5"
        >
          {/* Server-side unexpected error banner */}
          {bannerError !== null && <FormErrorBanner message={bannerError} />}

          <TextField
            id="forgot-email"
            label="Email address"
            type="email"
            autoComplete="email"
            inputMode="email"
            disabled={loading}
            error={errors.email?.message}
            {...register('email')}
          />

          <SubmitButton loading={loading} loadingLabel="Sending…">
            Send reset link
          </SubmitButton>
        </form>

        {/* ── Trust badge ─────────────────────────────────────────────────── */}
        <p className="mt-4 flex items-center justify-center gap-1.5 text-xs text-gray-400">
          <svg
            aria-hidden="true"
            className="h-3.5 w-3.5 flex-shrink-0"
            viewBox="0 0 20 20"
            fill="currentColor"
          >
            <path
              fillRule="evenodd"
              d="M10 1a4.5 4.5 0 0 0-4.5 4.5V9H5a2 2 0 0 0-2 2v6a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2v-6a2 2 0 0 0-2-2h-.5V5.5A4.5 4.5 0 0 0 10 1Zm3 8V5.5a3 3 0 1 0-6 0V9h6Z"
              clipRule="evenodd"
            />
          </svg>
          We never share your email address.
        </p>

        {/* ── Back to sign in ─────────────────────────────────────────────── */}
        <div className="mt-4 text-center">
          <Link
            to="/login"
            aria-label="Back to sign in page"
            className="text-xs text-gray-400 hover:text-gray-600 hover:underline focus:outline-none focus-visible:ring-2 focus-visible:ring-brand-500 rounded"
          >
            ← Back to sign in
          </Link>
        </div>
      </div>
    </main>
  );
}

// ── Shared sub-component ──────────────────────────────────────────────────────

function BrandHeader() {
  return (
    <div className="mb-6 flex flex-col items-center gap-2">
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
          <path
            strokeLinecap="round"
            strokeLinejoin="round"
            d="M12 3L2 9h20L12 3ZM4 9v9m4-9v9m4-9v9m4-9v9M2 18h20"
          />
        </svg>
      </div>
      <span className="text-lg font-bold tracking-tight text-gray-900">
        NorthBank
      </span>
    </div>
  );
}
