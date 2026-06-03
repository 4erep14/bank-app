// Story: US-001
import type { ButtonHTMLAttributes } from 'react';

export interface SubmitButtonProps
  extends Omit<ButtonHTMLAttributes<HTMLButtonElement>, 'type'> {
  loading?: boolean;
  loadingLabel?: string;
}

/**
 * Primary submit button with loading spinner.
 * Disables itself and sets aria-disabled during loading.
 */
export default function SubmitButton({
  children,
  loading = false,
  loadingLabel = 'Loading…',
  disabled,
  className = '',
  ...rest
}: SubmitButtonProps) {
  const isDisabled = disabled || loading;

  return (
    <button
      type="submit"
      disabled={isDisabled}
      aria-disabled={isDisabled}
      aria-label={loading ? loadingLabel : undefined}
      className={[
        'flex w-full items-center justify-center gap-2 rounded-md px-4 py-2.5',
        'bg-brand-600 text-sm font-semibold text-white shadow-sm',
        'transition hover:bg-brand-700 focus:outline-none focus:ring-2 focus:ring-brand-500 focus:ring-offset-2',
        'disabled:cursor-not-allowed disabled:opacity-60',
        className,
      ]
        .filter(Boolean)
        .join(' ')}
      {...rest}
    >
      {loading && (
        <svg
          aria-hidden="true"
          className="h-4 w-4 animate-spin"
          fill="none"
          viewBox="0 0 24 24"
        >
          <circle
            className="opacity-25"
            cx="12"
            cy="12"
            r="10"
            stroke="currentColor"
            strokeWidth="4"
          />
          <path
            className="opacity-75"
            fill="currentColor"
            d="M4 12a8 8 0 0 1 8-8V0C5.373 0 0 5.373 0 12h4Z"
          />
        </svg>
      )}
      {loading ? loadingLabel : children}
    </button>
  );
}
