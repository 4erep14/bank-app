// Story: US-010
import { z } from 'zod';

const MONEY_PATTERN = /^\d+(\.\d{1,2})?$/;

export const transferSchema = z
  .object({
    sourceAccountId: z.string().min(1, 'Choose the account to transfer from.'),
    destinationAccountId: z.string().min(1, 'Choose the account to transfer to.'),
    amount: z
      .string()
      .trim()
      .min(1, 'Enter an amount.')
      .refine((value) => MONEY_PATTERN.test(value), 'Use dollars and cents only.')
      .refine((value) => Number(value) > 0, 'Amount must be greater than $0.00.')
      .refine((value) => Number(value) <= 10000, 'Transfer limit is $10,000.00.'),
    description: z.string().max(255, 'Description must be 255 characters or fewer.').optional(),
  })
  .refine((values) => values.sourceAccountId !== values.destinationAccountId, {
    path: ['destinationAccountId'],
    message: 'Choose two different accounts.',
  });

export type TransferFormValues = z.infer<typeof transferSchema>;
