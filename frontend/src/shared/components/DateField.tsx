// Story: US-001
import type { InputHTMLAttributes } from 'react';
import TextField from './TextField';

export interface DateFieldProps
  extends Omit<InputHTMLAttributes<HTMLInputElement>, 'id' | 'type'> {
  id: string;
  label: string;
  error?: string;
}

/**
 * Native date input with sensible min/max defaults:
 *   min = 1900-01-01  (no unrealistic birth dates)
 *   max = today       (must be in the past)
 */
export default function DateField({
  id,
  label,
  error,
  min = '1900-01-01',
  ...rest
}: DateFieldProps) {
  const today = new Date().toISOString().split('T')[0];

  return (
    <TextField
      id={id}
      label={label}
      type="date"
      autoComplete="bday"
      min={min}
      max={today}
      error={error}
      {...rest}
    />
  );
}
