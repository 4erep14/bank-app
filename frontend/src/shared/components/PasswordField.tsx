// Story: US-001
import { useState } from 'react';
import type { InputHTMLAttributes } from 'react';

export interface PasswordFieldProps
  extends Omit<InputHTMLAttributes<HTMLInputElement>, 'id' | 'type'> {
  id: string;
  label: string;
  error?: string;
}

/**
 * Password input with show/hide toggle.
 * Toggle button: aria-pressed, aria-label for screen-reader users.
 */
export default function PasswordField({
  id,
  label,
  error,
  disabled,
  className = '',
  ...rest
}: PasswordFieldProps) {
  const [visible, setVisible] = useState(false);

  const errorId = `${id}-error`;
  const describedBy = error ? errorId : undefined;

  return (
    <div className="flex flex-col gap-1">
      <label htmlFor={id} className="text-sm font-medium text-gray-700">
        {label}
      </label>

      <div className="relative">
        <input
          id={id}
          type={visible ? 'text' : 'password'}
          autoComplete="new-password"
          aria-invalid={!!error}
          aria-describedby={describedBy}
          disabled={disabled}
          className={[
            'w-full rounded-md border px-3 py-2 pr-10 text-sm shadow-sm transition',
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

        {/* Show / hide toggle */}
        <button
          type="button"
          aria-pressed={visible}
          aria-label={visible ? 'Hide password' : 'Show password'}
          disabled={disabled}
          onClick={() => setVisible((v) => !v)}
          className={[
            'absolute inset-y-0 right-0 flex items-center px-3',
            'text-gray-500 hover:text-gray-700',
            'disabled:cursor-not-allowed disabled:opacity-50',
            'focus:outline-none focus-visible:ring-2 focus-visible:ring-brand-500 rounded-r-md',
          ].join(' ')}
        >
          {visible ? (
            /* Eye-slash icon */
            <svg
              aria-hidden="true"
              className="h-4 w-4"
              fill="none"
              stroke="currentColor"
              strokeWidth={2}
              viewBox="0 0 24 24"
            >
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                d="M3.98 8.223A10.477 10.477 0 0 0 1.934 12C3.226 16.338 7.244 19.5 12 19.5c.993 0 1.953-.138 2.863-.395M6.228 6.228A10.451 10.451 0 0 1 12 4.5c4.756 0 8.773 3.162 10.065 7.498a10.522 10.522 0 0 1-4.293 5.774M6.228 6.228 3 3m3.228 3.228 3.65 3.65m7.894 7.894L21 21m-3.228-3.228-3.65-3.65m0 0a3 3 0 1 0-4.243-4.243m4.242 4.242L9.88 9.88"
              />
            </svg>
          ) : (
            /* Eye icon */
            <svg
              aria-hidden="true"
              className="h-4 w-4"
              fill="none"
              stroke="currentColor"
              strokeWidth={2}
              viewBox="0 0 24 24"
            >
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                d="M2.036 12.322a1.012 1.012 0 0 1 0-.639C3.423 7.51 7.36 4.5 12 4.5c4.638 0 8.573 3.007 9.963 7.178.07.207.07.431 0 .639C20.577 16.49 16.64 19.5 12 19.5c-4.638 0-8.573-3.007-9.963-7.178Z"
              />
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                d="M15 12a3 3 0 1 1-6 0 3 3 0 0 1 6 0Z"
              />
            </svg>
          )}
        </button>
      </div>

      {error && (
        <p
          id={errorId}
          role="alert"
          className="flex items-center gap-1 text-xs text-red-600"
        >
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
    </div>
  );
}
