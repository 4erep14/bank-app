// Story: US-001
import { useMutation } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import { isAxiosError } from 'axios';
import { registerCustomer } from '../api/registrationApi';
import type {
  RegisterRequest,
  FieldError,
  ApiValidationError,
} from '../types/registration.types';

export interface UseRegisterCustomerResult {
  mutate: (data: RegisterRequest) => void;
  isPending: boolean;
  serverFieldErrors: FieldError[];
  networkError: string | null;
  reset: () => void;
}

/**
 * Wraps the POST /api/v1/customers call.
 *
 * - 201 → navigate to /register/success with firstName in router state
 * - 400 → expose field-level errors via serverFieldErrors
 * - 409 → map to { field: 'email', message: '...' }
 * - other → expose generic networkError message
 */
export function useRegisterCustomer(
  onFieldErrors: (errors: FieldError[]) => void,
): UseRegisterCustomerResult {
  const navigate = useNavigate();

  const {
    mutate: rawMutate,
    isPending,
    error,
    reset,
  } = useMutation<
    Awaited<ReturnType<typeof registerCustomer>>,
    unknown,
    RegisterRequest
  >({
    mutationFn: registerCustomer,
    onSuccess: (_data, variables) => {
      navigate('/register/success', {
        state: { firstName: variables.firstName },
      });
    },
    onError: (err) => {
      if (!isAxiosError(err)) return;

      const status = err.response?.status;

      if (status === 409) {
        onFieldErrors([
          {
            field: 'email',
            message:
              'This email is already registered. Try signing in instead.',
          },
        ]);
        return;
      }

      if (status === 400) {
        const body = err.response?.data as ApiValidationError;
        if (Array.isArray(body?.errors)) {
          onFieldErrors(body.errors);
        }
        return;
      }
    },
  });

  /** Derive networkError: only for non-400/409 Axios errors or non-Axios errors */
  let networkError: string | null = null;
  let serverFieldErrors: FieldError[] = [];

  if (error) {
    if (isAxiosError(error)) {
      const status = error.response?.status;
      if (status === 409) {
        serverFieldErrors = [
          {
            field: 'email',
            message:
              'This email is already registered. Try signing in instead.',
          },
        ];
      } else if (status === 400) {
        const body = error.response?.data as ApiValidationError;
        serverFieldErrors = Array.isArray(body?.errors) ? body.errors : [];
      } else {
        networkError =
          'Something went wrong on our end. Please try again.';
      }
    } else {
      networkError = 'Something went wrong on our end. Please try again.';
    }
  }

  return {
    mutate: rawMutate,
    isPending,
    serverFieldErrors,
    networkError,
    reset,
  };
}
