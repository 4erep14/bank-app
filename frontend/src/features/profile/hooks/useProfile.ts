// Story: US-005
import { useEffect } from 'react';
import { useQuery } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import { isAxiosError } from 'axios';
import { getProfile } from '../api/profileApi';
import {
  ACCESS_TOKEN_KEY,
  REFRESH_TOKEN_KEY,
} from '@/features/auth/types/auth.types';
import type { ProfileResponse } from '../types/profile.types';

/**
 * Fetches the authenticated customer's profile via GET /api/v1/profile.
 *
 * AC1: exposes { firstName, lastName, email, phoneNumber, dateOfBirth }
 * AC6: on 401 response — clears stored tokens and redirects to /login
 */
export function useProfile() {
  const navigate = useNavigate();

  const query = useQuery<ProfileResponse, unknown>({
    queryKey: ['profile'],
    queryFn: getProfile,
    // Do not retry on 401 — it means the session is gone
    retry: (failureCount, error) => {
      if (isAxiosError(error) && error.response?.status === 401) {
        return false;
      }
      return failureCount < 1;
    },
  });

  // AC6: 401 → clear tokens and redirect to /login
  useEffect(() => {
    if (
      query.isError &&
      isAxiosError(query.error) &&
      query.error.response?.status === 401
    ) {
      localStorage.removeItem(ACCESS_TOKEN_KEY);
      localStorage.removeItem(REFRESH_TOKEN_KEY);
      navigate('/login', { replace: true });
    }
  }, [query.isError, query.error, navigate]);

  return query;
}
