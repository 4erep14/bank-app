// Story: US-001
import { useRef } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import {
  registrationSchema,
  type RegistrationFormValues,
} from '../validation/registrationSchema';
import { useRegisterCustomer } from '../hooks/useRegisterCustomer';
import type { FieldError } from '../types/registration.types';

import TextField from '@/shared/components/TextField';
import PhoneField from '@/shared/components/PhoneField';
import DateField from '@/shared/components/DateField';
import PasswordField from '@/shared/components/PasswordField';
import PasswordChecklist from '@/shared/components/PasswordChecklist';
import FormErrorBanner from '@/shared/components/FormErrorBanner';
import SubmitButton from '@/shared/components/SubmitButton';

/**
 * Registration form component.
 *
 * - Validates on blur; re-validates on change once an error is shown.
 * - Maps server 400/409 errors to react-hook-form field errors.
 * - Shows FormErrorBanner on server errors; moves focus to first error field.
 * - Disables all fields while submitting.
 */
export default function RegistrationForm() {
  const firstErrorRef = useRef<HTMLInputElement | null>(null);

  const {
    register,
    handleSubmit,
    watch,
    setError,
    formState: { errors, isSubmitting },
  } = useForm<RegistrationFormValues>({
    resolver: zodResolver(registrationSchema),
    mode: 'onBlur',
    reValidateMode: 'onChange',
  });

  /** Called by useRegisterCustomer when the server returns field-level errors */
  const handleServerFieldErrors = (fieldErrors: FieldError[]) => {
    fieldErrors.forEach(({ field, message }) => {
      setError(field as keyof RegistrationFormValues, {
        type: 'server',
        message,
      });
    });
    // Move focus to the first errored field after React re-renders
    requestAnimationFrame(() => {
      const firstErrorEl = document.querySelector<HTMLInputElement>(
        '[aria-invalid="true"]',
      );
      firstErrorEl?.focus();
    });
  };

  const { mutate, isPending, networkError, reset: resetMutation } =
    useRegisterCustomer(handleServerFieldErrors);

  const loading = isSubmitting || isPending;

  const passwordValue = watch('password') ?? '';

  // Show banner when server errors exist
  const hasServerErrors =
    networkError !== null ||
    Object.values(errors).some((e) => e?.type === 'server');

  const onSubmit = (data: RegistrationFormValues) => {
    resetMutation();
    mutate(data);
  };

  // Suppress unused variable warning – ref is assigned via callback
  void firstErrorRef;

  return (
    <form
      onSubmit={handleSubmit(onSubmit)}
      noValidate
      aria-busy={loading}
      aria-label="Create account form"
      className="flex flex-col gap-5"
    >
      {/* Error banner */}
      {(hasServerErrors || networkError) && (
        <FormErrorBanner
          message={
            networkError ?? 'Please fix the highlighted fields below.'
          }
        />
      )}

      {/* First name + Last name — two columns on md+ */}
      <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
        <TextField
          id="firstName"
          label="First name"
          autoComplete="given-name"
          disabled={loading}
          error={errors.firstName?.message}
          {...register('firstName')}
        />
        <TextField
          id="lastName"
          label="Last name"
          autoComplete="family-name"
          disabled={loading}
          error={errors.lastName?.message}
          {...register('lastName')}
        />
      </div>

      {/* Email */}
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

      {/* Phone */}
      <PhoneField
        id="phoneNumber"
        label="Mobile number"
        disabled={loading}
        error={errors.phoneNumber?.message}
        {...register('phoneNumber')}
      />

      {/* Date of birth */}
      <DateField
        id="dateOfBirth"
        label="Date of birth"
        disabled={loading}
        error={errors.dateOfBirth?.message}
        {...register('dateOfBirth')}
      />

      {/* Password + live checklist */}
      <div className="flex flex-col gap-1">
        <PasswordField
          id="password"
          label="Password"
          disabled={loading}
          error={errors.password?.message}
          {...register('password')}
        />
        <PasswordChecklist password={passwordValue} />
      </div>

      {/* Submit */}
      <SubmitButton
        loading={loading}
        loadingLabel="Creating account…"
      >
        Create account
      </SubmitButton>
    </form>
  );
}
