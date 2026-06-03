// Story: US-001
import { z } from 'zod';

/** Returns true when the value is a date in the past */
const isPastDate = (value: string): boolean => {
  if (!value) return false;
  const parsed = new Date(value);
  if (isNaN(parsed.getTime())) return false;
  return parsed < new Date();
};

export const registrationSchema = z.object({
  firstName: z
    .string()
    .min(1, 'Enter your first name.')
    .max(100, 'First name must be 100 characters or fewer.'),

  lastName: z
    .string()
    .min(1, 'Enter your last name.')
    .max(100, 'Last name must be 100 characters or fewer.'),

  email: z
    .string()
    .min(1, 'Enter your email address.')
    .email('Enter a valid email address, like name@example.com.'),

  phoneNumber: z
    .string()
    .min(1, 'Enter your mobile number.')
    .regex(
      /^\+[1-9]\d{1,14}$/,
      'Enter your number in international format, starting with + (e.g. +447911123456).',
    ),

  dateOfBirth: z
    .string()
    .min(1, 'Enter your date of birth.')
    .refine(isPastDate, { message: 'Enter a valid date of birth in the past.' }),

  password: z
    .string()
    .min(1, 'Enter a password.')
    .min(8, 'Password must be at least 8 characters.')
    .refine((val) => /[A-Z]/.test(val), {
      message: 'Add at least one uppercase letter (A–Z).',
    })
    .refine((val) => /[a-z]/.test(val), {
      message: 'Add at least one lowercase letter (a–z).',
    })
    .refine((val) => /[0-9]/.test(val), {
      message: 'Add at least one number (0–9).',
    })
    .refine((val) => /[!?@#$%^&*()\-_=+[\]{};:'",.<>/\\|`~]/.test(val), {
      message: 'Add at least one special character (! ? @ # etc.).',
    }),
});

export type RegistrationFormValues = z.infer<typeof registrationSchema>;
