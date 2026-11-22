import { QueryClient } from '@tanstack/react-query';
import { ApiError } from './client';

/** A 4xx won't change on retry; network errors and 5xx get up to 2 more tries. */
const shouldRetry = (failureCount: number, error: unknown): boolean =>
  !(error instanceof ApiError && error.status < 500) && failureCount < 2;

export const createQueryClient = (): QueryClient =>
  new QueryClient({
    defaultOptions: {
      queries: {
        retry: shouldRetry,
        staleTime: 30_000, // balances and history are re-fetched after 30 s, or at once after a mutation
      },
      mutations: {
        retry: false, // never repeat a money movement automatically
      },
    },
  });

export const queryClient = createQueryClient();
