import { useQuery } from '@tanstack/react-query';
import { apiFetch } from './client';
import { queryKeys } from './queryKeys';
import type { AccountSummary } from './types';

/** GET /api/accounts/summary: the signed-in user's accounts and their total balance. */
export const getAccountSummary = (): Promise<AccountSummary> =>
  apiFetch<AccountSummary>('/api/accounts/summary');

export const useAccountSummary = () =>
  useQuery({ queryKey: queryKeys.accounts.summary(), queryFn: getAccountSummary });
