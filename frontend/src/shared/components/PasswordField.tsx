import { forwardRef, useState, type InputHTMLAttributes } from 'react';

export interface PasswordFieldProps
  extends Omit<InputHTMLAttributes<HTMLInputElement>, 'id' | 'type'> {
  id: string;
  label: string;
  error?: string;
}

const PasswordField = forwardRef<HTMLInputElement, PasswordFieldProps>(
  function PasswordField(
    {
      id,
      label,
      error,
      disabled,
      className = '',
      ...rest
    },
    ref,
  ) {
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
            ref={ref}
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

          <button
            type="button"
            aria-pressed={visible}
            aria-label={visible ? 'Hide password' : 'Show password'}
            disabled={disabled}
            onClick={() => setVisible((v) => !v)}
            className="absolute inset-y-0 right-0 flex items-center rounded-r-md px-3 text-gray-500 hover:text-gray-700 disabled:cursor-not-allowed disabled:opacity-50 focus:outline-none focus-visible:ring-2 focus-visible:ring-brand-500"
          >
            {visible ? '🙈' : '👁'}
          </button>
        </div>

        {error && (
          <p
            id={errorId}
            role="alert"
            className="flex items-center gap-1 text-xs text-red-600"
          >
            {error}
          </p>
        )}
      </div>
    );
  },
);

export default PasswordField;