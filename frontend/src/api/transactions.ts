import { useQuery } from '@tanstack/react-query';
import { apiFetch } from './client';
import { queryKeys } from './queryKeys';
import type { Page, Transaction } from './types';

/** GET /api/transfers/account/{accountNumber}?page&size: newest first; owner or admin only. */
export const getAccountTransactions = (
  accountNumber: string,
  page: number,
  size: number,
): Promise<Page<Transaction>> =>
  apiFetch<Page<Transaction>>(
    `/api/transfers/account/${encodeURIComponent(accountNumber)}?page=${page}&size=${size}`,
  );

/** Disabled while there is no account number (e.g. a user without accounts). */
export const useAccountTransactions = (accountNumber: string | undefined, page = 0, size = 10) =>
  useQuery({
    queryKey: queryKeys.transactions.byAccount(accountNumber ?? '', page, size),
    queryFn: () => getAccountTransactions(accountNumber as string, page, size),
    enabled: Boolean(accountNumber),
    // While the next page of the SAME account loads, keep showing the current page. Never show another
    // account's rows: their in/out direction would be wrong for this account.
    placeholderData: (previous, previousQuery) =>
      previousQuery?.queryKey[2] === accountNumber ? previous : undefined,
  });
