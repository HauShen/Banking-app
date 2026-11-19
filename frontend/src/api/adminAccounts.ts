import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { apiFetch } from './client';
import { queryKeys } from './queryKeys';
import type { Account, AccountStatus, AccountType } from './types';

export interface AccountFilter {
  /** Only this user's accounts (GET /get_all/{user_id}); otherwise every account (GET /get_all). */
  userId?: string;
  /** Every account only: ACTIVE or LOCKED; absent = both. */
  status?: AccountStatus;
}

/** The account list endpoints return plain lists (no paging): the table pages them in the browser. */
export const listAccounts = ({ userId, status }: AccountFilter): Promise<Account[]> =>
  userId
    ? apiFetch<Account[]>(`/api/accounts/get_all/${encodeURIComponent(userId)}`)
    : apiFetch<Account[]>(`/api/accounts/get_all${status ? `?status=${status}` : ''}`);

export const useAdminAccounts = (filter: AccountFilter) =>
  useQuery({
    queryKey: queryKeys.admin.accounts.list({ status: filter.status, userId: filter.userId }),
    queryFn: () => listAccounts(filter),
  });

/** POST /api/accounts/create → 201: a new ACTIVE MYR account with the opening balance (RM 20.00). */
export const openAccount = (request: { userId: string; accountType: AccountType }) =>
  apiFetch<Account>('/api/accounts/create', { method: 'POST', body: request });

/** PATCH /api/accounts/{id}/status?status= */
export const changeAccountStatus = ({
  accountId,
  status,
}: {
  accountId: number;
  status: AccountStatus;
}) => apiFetch<Account>(`/api/accounts/${accountId}/status?status=${status}`, { method: 'PATCH' });

/** DELETE /api/accounts/{id} → 204; 409 when the account has transactions. */
export const deleteAccount = (accountId: number) =>
  apiFetch<void>(`/api/accounts/${accountId}`, { method: 'DELETE' });

/** Every account change re-fetches the admin lists, and the signed-in user's own summary (it may be theirs). */
const useAccountMutation = <TArgs, TResult>(mutationFn: (args: TArgs) => Promise<TResult>) => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn,
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: queryKeys.admin.accounts.all });
      void queryClient.invalidateQueries({ queryKey: queryKeys.accounts.all });
    },
  });
};

export const useOpenAccount = () => useAccountMutation(openAccount);
export const useChangeAccountStatus = () => useAccountMutation(changeAccountStatus);
export const useDeleteAccount = () => useAccountMutation(deleteAccount);
