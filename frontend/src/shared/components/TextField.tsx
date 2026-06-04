import { forwardRef, type InputHTMLAttributes } from 'react';

export interface TextFieldProps
  extends Omit<InputHTMLAttributes<HTMLInputElement>, 'id'> {
  id: string;
  label: string;
  error?: string;
  helperText?: string;
}

const TextField = forwardRef<HTMLInputElement, TextFieldProps>(
  function TextField(
    {
      id,
      label,
      error,
      helperText,
      className = '',
      ...rest
    },
    ref,
  ) {
    const errorId = `${id}-error`;
    const helperId = `${id}-helper`;
    const describedBy =
      [error ? errorId : null, helperText ? helperId : null]
        .filter(Boolean)
        .join(' ') || undefined;

    return (
      <div className="flex flex-col gap-1">
        <label htmlFor={id} className="text-sm font-medium text-gray-700">
          {label}
        </label>

        <input
          ref={ref}
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
  },
);

export default TextField;