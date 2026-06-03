// Story: US-004
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { Link, useSearchParams } from 'react-router-dom';

import {
  resetPasswordSchema,
  type ResetPasswordFormValues,
} from '../validation/resetPasswordSchema';
import { useResetPassword } from '../hooks/useResetPassword';
import PasswordField from '@/shared/components/PasswordField';
import PasswordChecklist from '@/shared/components/PasswordChecklist';
import SubmitButton from '@/shared/components/SubmitButton';
import FormErrorBanner from '@/shared/components/FormErrorBanner';
import ResetTokenErrorPage from './ResetTokenErrorPage';

// ── Page (token guard) ────────────────────────────────────────────────────────

/**
 * ResetPasswordPage — /reset-password?token=<value>
 *
 * Screen 3 of the Password Reset flow (US-004).
 *
 * AC: If no `token` query param is present, renders the expired/invalid state
 *     (Screen 5 — ResetTokenErrorPage) inline without navigating.
 */
export default function ResetPasswordPage() {
  const [searchParams] = useSearchParams();
  const token = searchParams.get('token');

  // No token in URL → show expired/invalid page immediately (spec requirement)
  if (!token) {
    return <ResetTokenErrorPage />;
  }

  return <ResetPasswordForm token={token} />;
}

// ── Form (rendered only when token exists) ────────────────────────────────────

interface ResetPasswordFormProps {
  token: string;
}

function ResetPasswordForm({ token }: ResetPasswordFormProps) {
  const {
    register,
    handleSubmit,
    watch,
    formState: { errors, isSubmitting },
  } = useForm<ResetPasswordFormValues>({
    resolver: zodResolver(resetPasswordSchema),
    mode: 'onBlur',
    reValidateMode: 'onChange',
    defaultValues: { newPassword: '', confirmPassword: '' },
  });

  const {
    submit,
    isPending,
    fieldErrors,
    bannerError,
    reset: resetMutation,
  } = useResetPassword();

  const loading = isSubmitting || isPending;

  // Live password value drives the PasswordChecklist
  const newPassword = watch('newPassword');

  const onSubmit = (data: ResetPasswordFormValues) => {
    resetMutation();
    submit({ token, newPassword: data.newPassword });
  };

  return (
    <main
      className="flex min-h-screen items-start justify-center bg-gray-50 px-4 py-12 sm:items-center"
      aria-labelledby="reset-password-heading"
    >
      <div className="w-full max-w-[440px] rounded-xl bg-white p-6 shadow-md sm:p-8">
        {/* ── Brand header ────────────────────────────────────────────────── */}
        <BrandHeader />

        {/* ── Page heading ────────────────────────────────────────────────── */}
        <div className="mb-6 text-center">
          <h1
            id="reset-password-heading"
            className="text-2xl font-bold text-gray-900"
          >
            Reset your password
          </h1>
          <p className="mt-1 text-sm text-gray-500">
            Create a new password for your account.
          </p>
        </div>

        {/* ── Form ────────────────────────────────────────────────────────── */}
        <form
          onSubmit={handleSubmit(onSubmit)}
          noValidate
          aria-busy={loading}
          aria-label="Reset password form"
          className="flex flex-col gap-5"
        >
          {/* Unexpected server error */}
          {bannerError !== null && <FormErrorBanner message={bannerError} />}

          {/* AC3: bean-validation field errors from the backend */}
          {fieldErrors.length > 0 && (
            <div
              role="alert"
              aria-live="assertive"
              className="rounded-md border border-red-300 bg-red-50 px-4 py-3 text-sm text-red-700"
            >
              <ul className="list-inside list-disc space-y-1">
                {fieldErrors.map((msg) => (
                  <li key={msg}>{msg}</li>
                ))}
              </ul>
            </div>
          )}

          {/* ── New password + live checklist ─────────────────────────────── */}
          <div className="flex flex-col gap-1">
            <PasswordField
              id="newPassword"
              label="New password"
              autoComplete="new-password"
              disabled={loading}
              error={errors.newPassword?.message}
              {...register('newPassword')}
            />
            {/* Live PasswordChecklist — always visible while typing */}
            <PasswordChecklist password={newPassword} />
          </div>

          {/* ── Confirm password ─────────────────────────────────────────── */}
          <PasswordField
            id="confirmPassword"
            label="Confirm new password"
            autoComplete="new-password"
            disabled={loading}
            error={errors.confirmPassword?.message}
            {...register('confirmPassword')}
          />

          <SubmitButton loading={loading} loadingLabel="Resetting…">
            Reset password
          </SubmitButton>
        </form>

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
