import { forwardRef, type InputHTMLAttributes } from 'react';
import TextField from './TextField';

export interface PhoneFieldProps
  extends Omit<InputHTMLAttributes<HTMLInputElement>, 'id' | 'type'> {
  id: string;
  label: string;
  error?: string;
}

const PhoneField = forwardRef<HTMLInputElement, PhoneFieldProps>(
  function PhoneField({ id, label, error, ...rest }, ref) {
    return (
      <TextField
        ref={ref}
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
  },
);

export default PhoneField;