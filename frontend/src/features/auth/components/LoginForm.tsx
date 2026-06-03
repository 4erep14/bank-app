// Story: US-002
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { Link } from 'react-router-dom';

import { loginSchema, type LoginFormValues } from '../validation/loginSchema';
import { useLogin } from '../hooks/useLogin';
import LockedAccountNotice from './LockedAccountNotice';

import TextField from '@/shared/components/TextField';
import PasswordField from '@/shared/components/PasswordField';
import FormErrorBanner from '@/shared/components/FormErrorBanner';
import SubmitButton from '@/shared/components/SubmitButton';

/**
 * Login form component.
 *
 * Responsibilities:
 * - Client-side validation via Zod (email format, non-empty password)
 * - Delegates API call to useLogin hook
 * - Renders FormErrorBanner for 401 / unexpected server errors (AC3)
 * - Renders LockedAccountNotice (replaces the form) on 423 (AC4/AC5)
 * - Passes autoComplete="current-password" to override PasswordField default
 * - Fully accessible: labels, aria-describedby, role="alert", keyboard nav
 *
 * States:
 *   default   — empty form, all fields enabled
 *   loading   — fields disabled, submit button shows spinner + "Signing in…"
 *   error     — FormErrorBanner above form (AC3: 401, or unexpected errors)
 *   locked    — LockedAccountNotice replaces form (AC4/AC5: 423)
 *   success   — hook navigates to /verify-otp; component unmounts (AC2)
 */
export default function LoginForm() {
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<LoginFormValues>({
    resolver: zodResolver(loginSchema),
    mode: 'onBlur',
    reValidateMode: 'onChange',
  });

  const {
    mutate,
    isPending,
    bannerError,
    isLocked,
    lockedMessage,
    reset: resetMutation,
  } = useLogin();

  const loading = isSubmitting || isPending;

  const onSubmit = (data: LoginFormValues) => {
    // Clear any previous error banner before issuing a new request
    resetMutation();
    mutate(data);
  };

  // ── AC4 / AC5: account locked — replace the form entirely ────────────────
  // The email/password inputs are intentionally absent so the user cannot
  // keep hammering a locked account. The LockedAccountNotice provides a
  // clear recovery path (contact support) and a "Back to sign in" escape hatch.
  if (isLocked) {
    return (
      <LockedAccountNotice
        lockedMessage={lockedMessage}
        onBackToSignIn={resetMutation}
      />
    );
  }

  // ── Default / loading / error form ───────────────────────────────────────
  return (
    <form
      onSubmit={handleSubmit(onSubmit)}
      noValidate
      aria-busy={loading}
      aria-label="Sign in form"
      className="flex flex-col gap-5"
    >
      {/* ── Server error banner (AC3: 401, or unexpected errors) ─────────── */}
      {bannerError !== null && (
        <FormErrorBanner message={bannerError} />
      )}

      {/* ── Email ────────────────────────────────────────────────────────── */}
      <TextField
        id="email"
        label="Email address"
        type="email"
        autoComplete="email"
        inputMode="email"
        disabled={loading}
        error={errors.email?.message}
        {...register('email')}
      />

      {/* ── Password ─────────────────────────────────────────────────────── */}
      {/*
        autoComplete="current-password" overrides the PasswordField default of
        "new-password" because {...rest} is spread last inside that component.
      */}
      <div className="flex flex-col gap-1">
        <PasswordField
          id="password"
          label="Password"
          autoComplete="current-password"
          disabled={loading}
          error={errors.password?.message}
          {...register('password')}
        />

        {/* Forgot password link — sits directly under the password field */}
        <div className="flex justify-end">
          <Link
            to="/forgot-password"
            aria-label="Go to forgot password page"
            className="text-xs text-brand-600 hover:text-brand-700 hover:underline focus:outline-none focus-visible:ring-2 focus-visible:ring-brand-500 rounded"
          >
            Forgot password?
          </Link>
        </div>
      </div>

      {/* ── Submit ───────────────────────────────────────────────────────── */}
      <SubmitButton loading={loading} loadingLabel="Signing in…">
        Sign in
      </SubmitButton>

      {/* ── Registration link ─────────────────────────────────────────────── */}
      <p className="text-center text-sm text-gray-500">
        Don&rsquo;t have an account?{' '}
        <Link
          to="/register"
          aria-label="Go to the registration page"
          className="font-medium text-brand-600 hover:text-brand-700 hover:underline focus:outline-none focus-visible:ring-2 focus-visible:ring-brand-500 rounded"
        >
          Register
        </Link>
      </p>
    </form>
  );
}
