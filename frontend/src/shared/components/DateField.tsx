import { forwardRef, type InputHTMLAttributes } from 'react';
import TextField from './TextField';

export interface DateFieldProps
  extends Omit<InputHTMLAttributes<HTMLInputElement>, 'id' | 'type'> {
  id: string;
  label: string;
  error?: string;
}

const DateField = forwardRef<HTMLInputElement, DateFieldProps>(
  function DateField({ id, label, error, min = '1900-01-01', ...rest }, ref) {
    const today = new Date().toISOString().split('T')[0];

    return (
      <TextField
        ref={ref}
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
  },
);

export default DateField;