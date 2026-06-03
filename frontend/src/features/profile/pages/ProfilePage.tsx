// Story: US-005
import { useEffect, useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { useProfile } from '../hooks/useProfile';
import { useUpdateProfile } from '../hooks/useUpdateProfile';
import { profileSchema } from '../validation/profileSchema';
import type { ProfileFormValues } from '../validation/profileSchema';
import ReadOnlyField from '../components/ReadOnlyField';
import TextField from '@/shared/components/TextField';
import SubmitButton from '@/shared/components/SubmitButton';
import FormErrorBanner from '@/shared/components/FormErrorBanner';

// ── Loading skeleton ─────────────────────────────────────────────────────────

function ProfileSkeleton() {
  return (
    <div
      aria-busy="true"
      aria-label="Loading profile…"
      className="space-y-5"
    >
      {[1, 2, 3, 4, 5].map((n) => (
        <div key={n} className="flex flex-col gap-1">
          <div className="h-4 w-24 animate-pulse rounded bg-gray-200" />
          <div className="h-10 w-full animate-pulse rounded-md bg-gray-200" />
        </div>
      ))}
    </div>
  );
}

// ── Success banner ────────────────────────────────────────────────────────────

interface SuccessBannerProps {
  message: string;
  onDismiss: () => void;
}

function SuccessBanner({ message, onDismiss }: SuccessBannerProps) {
  return (
    <div
      role="status"
      aria-live="polite"
      className="flex items-center justify-between gap-3 rounded-md border border-green-300 bg-green-50 px-4 py-3 text-sm text-green-800"
    >
      <div className="flex items-center gap-2">
        <svg
          aria-hidden="true"
          className="h-4 w-4 flex-shrink-0 text-green-600"
          viewBox="0 0 20 20"
          fill="currentColor"
        >
          <path
            fillRule="evenodd"
            d="M10 18a8 8 0 1 0 0-16 8 8 0 0 0 0 16Zm3.857-9.809a.75.75 0 0 0-1.214-.882l-3.483 4.79-1.88-1.88a.75.75 0 1 0-1.06 1.061l2.5 2.5a.75.75 0 0 0 1.137-.089l4-5.5Z"
            clipRule="evenodd"
          />
        </svg>
        <span>{message}</span>
      </div>
      <button
        type="button"
        onClick={onDismiss}
        aria-label="Dismiss success message"
        className="rounded text-green-600 hover:text-green-800 focus:outline-none focus:ring-2 focus:ring-green-500"
      >
        <svg aria-hidden="true" className="h-4 w-4" viewBox="0 0 20 20" fill="currentColor">
          <path d="M6.28 5.22a.75.75 0 0 0-1.06 1.06L8.94 10l-3.72 3.72a.75.75 0 1 0 1.06 1.06L10 11.06l3.72 3.72a.75.75 0 1 0 1.06-1.06L11.06 10l3.72-3.72a.75.75 0 0 0-1.06-1.06L10 8.94 6.28 5.22Z" />
        </svg>
      </button>
    </div>
  );
}

// ── Server field-errors list ──────────────────────────────────────────────────

interface FieldErrorListProps {
  errors: string[];
}

function FieldErrorList({ errors }: FieldErrorListProps) {
  return (
    <div
      role="alert"
      aria-live="assertive"
      className="flex items-start gap-3 rounded-md border border-red-300 bg-red-50 px-4 py-3 text-sm text-red-700"
    >
      <svg
        aria-hidden="true"
        className="mt-0.5 h-4 w-4 flex-shrink-0"
        viewBox="0 0 20 20"
        fill="currentColor"
      >
        <path
          fillRule="evenodd"
          d="M8.485 2.495c.673-1.167 2.357-1.167 3.03 0l6.28 10.875c.673 1.167-.17 2.625-1.516 2.625H3.72c-1.347 0-2.189-1.458-1.515-2.625L8.485 2.495ZM10 5a.75.75 0 0 1 .75.75v3.5a.75.75 0 0 1-1.5 0v-3.5A.75.75 0 0 1 10 5Zm0 9a1 1 0 1 0 0-2 1 1 0 0 0 0 2Z"
          clipRule="evenodd"
        />
      </svg>
      <ul className="list-disc list-inside space-y-1">
        {errors.map((e) => (
          <li key={e}>{e}</li>
        ))}
      </ul>
    </div>
  );
}

// ── Main page ─────────────────────────────────────────────────────────────────

type PageMode = 'view' | 'edit';

/**
 * ProfilePage — View & Update Customer Profile (US-005).
 *
 * View mode (default):
 *   AC1: displays firstName, lastName, email, phoneNumber, dateOfBirth
 *
 * Edit mode (entered via "Edit profile" button):
 *   AC2: allows updating firstName, lastName, phoneNumber
 *   AC3: email and dateOfBirth shown as read-only (locked)
 *   AC4: phoneNumber validated client-side (E.164) and server-side
 *   AC5: on save success — data re-fetched, success banner shown
 *   AC6: 401 → handled by useProfile (redirect to /login)
 */
export default function ProfilePage() {
  const [mode, setMode] = useState<PageMode>('view');
  const [successMessage, setSuccessMessage] = useState<string | null>(null);

  const profileQuery = useProfile();
  const updateMutation = useUpdateProfile();

  const { register, handleSubmit, formState: { errors: formErrors }, reset: resetForm } =
    useForm<ProfileFormValues>({
      resolver: zodResolver(profileSchema),
    });

  // Pre-fill form when entering edit mode
  useEffect(() => {
    if (mode === 'edit' && profileQuery.data) {
      resetForm({
        firstName: profileQuery.data.firstName,
        lastName: profileQuery.data.lastName,
        phoneNumber: profileQuery.data.phoneNumber,
      });
    }
  }, [mode, profileQuery.data, resetForm]);

  // AC5: on successful save → switch back to view, show success banner
  useEffect(() => {
    if (updateMutation.isSuccess) {
      setMode('view');
      setSuccessMessage('Profile updated successfully.');
    }
  }, [updateMutation.isSuccess]);

  const handleEditClick = () => {
    updateMutation.reset();
    setSuccessMessage(null);
    setMode('edit');
  };

  const handleCancel = () => {
    updateMutation.reset();
    setMode('view');
  };

  const onSubmit = (values: ProfileFormValues) => {
    updateMutation.reset();
    updateMutation.mutate({
      firstName: values.firstName,
      lastName: values.lastName,
      phoneNumber: values.phoneNumber,
    });
  };

  // ── Render: loading state ────────────────────────────────────────────────

  const isLoadingProfile = profileQuery.isLoading;
  const isProfileError = profileQuery.isError && !profileQuery.isLoading;

  // ── Format dateOfBirth for display (YYYY-MM-DD → DD/MM/YYYY) ─────────────
  const formatDate = (iso: string): string => {
    const [year, month, day] = iso.split('-');
    if (!year || !month || !day) return iso;
    return `${day}/${month}/${year}`;
  };

  return (
    <main
      className="flex min-h-screen items-start justify-center bg-gray-50 px-4 py-10 sm:py-14"
      aria-labelledby="profile-heading"
    >
      <div className="w-full max-w-lg">
        {/* ── NorthBank brand header ──────────────────────────────────────── */}
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

        {/* ── Card ─────────────────────────────────────────────────────────── */}
        <div className="rounded-xl bg-white p-6 shadow-md sm:p-8">
          {/* Page heading + action button */}
          <div className="mb-6 flex items-center justify-between">
            <h1
              id="profile-heading"
              className="text-xl font-bold text-gray-900 sm:text-2xl"
            >
              {mode === 'view' ? 'My Profile' : 'Edit Profile'}
            </h1>
            {mode === 'view' && !isLoadingProfile && !isProfileError && (
              <button
                type="button"
                onClick={handleEditClick}
                aria-label="Edit your profile"
                className="rounded-md bg-brand-600 px-4 py-2 text-sm font-semibold text-white shadow-sm transition hover:bg-brand-700 focus:outline-none focus:ring-2 focus:ring-brand-500 focus:ring-offset-2"
              >
                Edit profile
              </button>
            )}
          </div>

          {/* ── Success banner ──────────────────────────────────────────────── */}
          {successMessage && (
            <div className="mb-5">
              <SuccessBanner
                message={successMessage}
                onDismiss={() => setSuccessMessage(null)}
              />
            </div>
          )}

          {/* ── Loading state ───────────────────────────────────────────────── */}
          {isLoadingProfile && <ProfileSkeleton />}

          {/* ── Error state (non-401; 401 redirects via useProfile) ─────────── */}
          {isProfileError && !isLoadingProfile && (
            <div role="alert" className="text-center text-sm text-red-600">
              <p>Failed to load your profile. Please refresh the page.</p>
              <button
                type="button"
                onClick={() => void profileQuery.refetch()}
                className="mt-3 rounded-md border border-red-300 px-4 py-2 text-sm font-medium text-red-600 transition hover:bg-red-50 focus:outline-none focus:ring-2 focus:ring-red-400"
              >
                Try again
              </button>
            </div>
          )}

          {/* ── View mode ───────────────────────────────────────────────────── */}
          {!isLoadingProfile && !isProfileError && profileQuery.data && mode === 'view' && (
            <div className="space-y-5" aria-label="Profile details">
              <ReadOnlyField
                id="view-firstName"
                label="First name"
                value={profileQuery.data.firstName}
                locked={false}
              />
              <ReadOnlyField
                id="view-lastName"
                label="Last name"
                value={profileQuery.data.lastName}
                locked={false}
              />
              <ReadOnlyField
                id="view-email"
                label="Email address"
                value={profileQuery.data.email}
                locked={true}
              />
              <ReadOnlyField
                id="view-phoneNumber"
                label="Phone number"
                value={profileQuery.data.phoneNumber}
                locked={false}
              />
              <ReadOnlyField
                id="view-dateOfBirth"
                label="Date of birth"
                value={formatDate(profileQuery.data.dateOfBirth)}
                locked={true}
              />
            </div>
          )}

          {/* ── Edit mode ───────────────────────────────────────────────────── */}
          {!isLoadingProfile && !isProfileError && profileQuery.data && mode === 'edit' && (
            <form
              onSubmit={handleSubmit(onSubmit)}
              noValidate
              aria-label="Edit profile form"
            >
              <div className="space-y-5">
                {/* Server-level banner errors */}
                {updateMutation.serverError && (
                  <FormErrorBanner message={updateMutation.serverError} />
                )}

                {/* AC4: server field-level errors list */}
                {updateMutation.fieldErrors.length > 0 && (
                  <FieldErrorList errors={updateMutation.fieldErrors} />
                )}

                {/* Editable: First name */}
                <TextField
                  id="edit-firstName"
                  label="First name"
                  autoComplete="given-name"
                  error={formErrors.firstName?.message}
                  {...register('firstName')}
                />

                {/* Editable: Last name */}
                <TextField
                  id="edit-lastName"
                  label="Last name"
                  autoComplete="family-name"
                  error={formErrors.lastName?.message}
                  {...register('lastName')}
                />

                {/* Read-only: Email (AC3) */}
                <ReadOnlyField
                  id="edit-email"
                  label="Email address"
                  value={profileQuery.data.email}
                  locked={true}
                />

                {/* Editable: Phone number */}
                <TextField
                  id="edit-phoneNumber"
                  label="Phone number"
                  type="tel"
                  autoComplete="tel"
                  helperText="Format: +[country code][number], e.g. +44123456789"
                  error={formErrors.phoneNumber?.message}
                  {...register('phoneNumber')}
                />

                {/* Read-only: Date of birth (AC3) */}
                <ReadOnlyField
                  id="edit-dateOfBirth"
                  label="Date of birth"
                  value={formatDate(profileQuery.data.dateOfBirth)}
                  locked={true}
                />

                {/* Action buttons */}
                <div className="flex flex-col-reverse gap-3 pt-2 sm:flex-row sm:justify-end">
                  <button
                    type="button"
                    onClick={handleCancel}
                    disabled={updateMutation.isPending}
                    aria-label="Cancel editing and return to view mode"
                    className="w-full rounded-md border border-gray-300 bg-white px-4 py-2.5 text-sm font-semibold text-gray-700 shadow-sm transition hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-brand-500 focus:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-60 sm:w-auto"
                  >
                    Cancel
                  </button>
                  <SubmitButton
                    loading={updateMutation.isPending}
                    loadingLabel="Saving…"
                    className="sm:w-auto"
                  >
                    Save changes
                  </SubmitButton>
                </div>
              </div>
            </form>
          )}
        </div>
      </div>
    </main>
  );
}
