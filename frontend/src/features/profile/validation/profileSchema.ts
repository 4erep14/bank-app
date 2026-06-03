// Story: US-005
import { z } from 'zod';

/**
 * E.164 phone number pattern: starts with +, country code 1-9, then 6–14 digits.
 * Examples: +44123456789, +12025550100
 * Reference: https://www.itu.int/rec/T-REC-E.164/en
 */
const E164_PATTERN = /^\+[1-9]\d{6,14}$/;

/**
 * Zod schema for the profile edit form.
 *
 * All three fields are required in the form context because they are always
 * pre-filled from the current profile data (AC1/AC2).
 * The API accepts them as optional (PATCH semantics), but the form treats
 * them as required to prevent accidental empty submissions.
 *
 * AC2: only firstName, lastName, phoneNumber are editable
 * AC4: phoneNumber must match E.164 format
 */
export const profileSchema = z.object({
  firstName: z
    .string()
    .min(1, 'First name is required')
    .max(100, 'First name must be 100 characters or fewer'),

  lastName: z
    .string()
    .min(1, 'Last name is required')
    .max(100, 'Last name must be 100 characters or fewer'),

  phoneNumber: z
    .string()
    .min(1, 'Phone number is required')
    .regex(
      E164_PATTERN,
      'Enter a valid phone number in E.164 format (e.g. +44123456789)',
    ),
});

export type ProfileFormValues = z.infer<typeof profileSchema>;
