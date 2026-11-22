import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { apiFetch } from './client';
import { queryKeys } from './queryKeys';
import type { Account, AccountSummary } from './types';

/** GET /api/accounts/summary: the signed-in user's accounts and their total balance. */
export const getAccountSummary = (): Promise<AccountSummary> =>
  apiFetch<AccountSummary>('/api/accounts/summary');

export const useAccountSummary = () =>
  useQuery({ queryKey: queryKeys.accounts.summary(), queryFn: getAccountSummary });

export interface TopUpRequest {
  accountId: number;
  /** API string, e.g. "100.00" (from normalizeAmountInput). */
  amount: string;
}

/**
 * PATCH /api/accounts/{id}/topup?amount= : a simulated cash deposit into one of the user's own accounts.
 * Returns the account with its new balance.
 */
export const topUpAccount = ({ accountId, amount }: TopUpRequest): Promise<Account> =>
  apiFetch<Account>(`/api/accounts/${accountId}/topup?amount=${encodeURIComponent(amount)}`, {
    method: 'PATCH',
  });

/**
 * Deposits, then re-fetches balances and histories. Never retried automatically: the endpoint has no
 * idempotency key, so a repeated request would deposit twice.
 */
export const useTopUp = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: topUpAccount,
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: queryKeys.accounts.all });
      void queryClient.invalidateQueries({ queryKey: queryKeys.transactions.all });
    },
  });
};
