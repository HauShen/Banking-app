import { keepPreviousData, useQuery } from '@tanstack/react-query';
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
    placeholderData: keepPreviousData, // keep showing the old list while the next one loads
  });
