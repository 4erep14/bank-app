// Story: US-001
import type { InputHTMLAttributes } from 'react';

export interface TextFieldProps
  extends Omit<InputHTMLAttributes<HTMLInputElement>, 'id'> {
  /** Unique field id – also used to construct describedby ids */
  id: string;
  label: string;
  error?: string;
  helperText?: string;
}

/**
 * Reusable labelled text input.
 * Renders label → input → helper/error text.
 * Fully accessible: aria-invalid, aria-describedby, role="alert" on errors.
 */
export default function TextField({
  id,
  label,
  error,
  helperText,
  className = '',
  ...rest
}: TextFieldProps) {
  const errorId = `${id}-error`;
  const helperId = `${id}-helper`;
  const describedBy =
    [error ? errorId : null, helperText ? helperId : null]
      .filter(Boolean)
      .join(' ') || undefined;

  return (
    <div className="flex flex-col gap-1">
      <label
        htmlFor={id}
        className="text-sm font-medium text-gray-700"
      >
        {label}
      </label>

      <input
        id={id}
        aria-invalid={!!error}
        aria-describedby={describedBy}
        className={[
          'rounded-md border px-3 py-2 text-sm shadow-sm transition',
          'focus:outline-none focus:ring-2 focus:ring-brand-500',
          'disabled:cursor-not-allowed disabled:bg-gray-100 disabled:text-gray-500',
          error
            ? 'border-red-500 bg-red-50 focus:ring-red-400'
            : 'border-gray-300 bg-white focus:border-brand-500',
          className,
        ]
          .filter(Boolean)
          .join(' ')}
        {...rest}
      />

      {error && (
        <p
          id={errorId}
          role="alert"
          className="flex items-center gap-1 text-xs text-red-600"
        >
          {/* Error icon */}
          <svg
            aria-hidden="true"
            className="h-3.5 w-3.5 flex-shrink-0"
            viewBox="0 0 20 20"
            fill="currentColor"
          >
            <path
              fillRule="evenodd"
              d="M18 10A8 8 0 1 1 2 10a8 8 0 0 1 16 0Zm-8-5a.75.75 0 0 1 .75.75v4.5a.75.75 0 0 1-1.5 0v-4.5A.75.75 0 0 1 10 5Zm0 10a1 1 0 1 0 0-2 1 1 0 0 0 0 2Z"
              clipRule="evenodd"
            />
          </svg>
          {error}
        </p>
      )}

      {helperText && !error && (
        <p id={helperId} className="text-xs text-gray-500">
          {helperText}
        </p>
      )}
    </div>
  );
}
