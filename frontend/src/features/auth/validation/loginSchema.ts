// Story: US-002
import { z } from 'zod';

/**
 * Client-side validation schema for the login form.
 * Intentionally minimal — the backend is the authoritative validator.
 * We only guard against obviously empty/malformed input to give the user
 * immediate feedback without a round-trip.
 */
export const loginSchema = z.object({
  email: z
    .string()
    .min(1, 'Email is required.')
    .email('Please enter a valid email address.'),

  password: z
    .string()
    .min(1, 'Password is required.'),
});

export type LoginFormValues = z.infer<typeof loginSchema>;
