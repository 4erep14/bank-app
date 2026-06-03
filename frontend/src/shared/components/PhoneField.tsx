// Story: US-001
import type { InputHTMLAttributes } from 'react';
import TextField from './TextField';

export interface PhoneFieldProps
  extends Omit<InputHTMLAttributes<HTMLInputElement>, 'id' | 'type'> {
  id: string;
  label: string;
  error?: string;
}

/**
 * Tel input that surfaces E.164 helper text.
 * Delegates rendering to TextField with type="tel".
 */
export default function PhoneField({
  id,
  label,
  error,
  ...rest
}: PhoneFieldProps) {
  return (
    <TextField
      id={id}
      label={label}
      type="tel"
      autoComplete="tel"
      inputMode="tel"
      placeholder="+447911123456"
      helperText="Use international format, e.g. +447911123456"
      error={error}
      {...rest}
    />
  );
}
