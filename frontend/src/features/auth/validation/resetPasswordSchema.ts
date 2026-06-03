// Story: US-004
import { z } from 'zod';

/**
 * Regex for the five password-complexity rules (mirrors backend bean-validation).
 * Kept here so the PasswordChecklist and Zod schema share the same definitions.
 */
export const PASSWORD_RULES = {
  minLength: 8,
  uppercase: /[A-Z]/,
  lowercase: /[a-z]/,
  digit: /[0-9]/,
  special: /[!?@#$%^&*()\-_=+[\]{};:'",.<>/\\|`~]/,
} as const;

/**
 * Zod schema for the Reset Password form.
 *
 * AC3: newPassword must satisfy all 5 complexity rules.
 * Confirm password must match newPassword exactly.
 */
export const resetPasswordSchema = z
  .object({
    newPassword: z
      .string()
      .min(1, 'Password is required.')
      .min(PASSWORD_RULES.minLength, 'Password must be at least 8 characters.')
      .regex(PASSWORD_RULES.uppercase, 'Password must contain at least one uppercase letter.')
      .regex(PASSWORD_RULES.lowercase, 'Password must contain at least one lowercase letter.')
      .regex(PASSWORD_RULES.digit, 'Password must contain at least one number.')
      .regex(PASSWORD_RULES.special, 'Password must contain at least one special character.'),
    confirmPassword: z.string().min(1, 'Please confirm your password.'),
  })
  .refine((data) => data.newPassword === data.confirmPassword, {
    message: 'Passwords do not match.',
    path: ['confirmPassword'],
  });

export type ResetPasswordFormValues = z.infer<typeof resetPasswordSchema>;
