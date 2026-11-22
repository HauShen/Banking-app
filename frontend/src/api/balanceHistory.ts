import { useQuery } from '@tanstack/react-query';
import { apiFetch } from './client';
import { queryKeys } from './queryKeys';

/** BalanceHistoryResponseBody.java: one closing balance per day, oldest first, ending today. */
export interface BalanceHistory {
  accountNumber: string;
  currency: string;
  /** Where the days begin and end, e.g. "Asia/Kuala_Lumpur". */
  timeZone: string;
  days: DailyBalance[];
}

export interface DailyBalance {
  /** Calendar day in the bank's time zone, "2026-11-20". */
  date: string;
  /** A BigDecimal: display it, never calculate with it. */
  closingBalance: number;
}

/** GET /api/transfers/account/{accountNumber}/balance-history?days= (owner or admin; 1–366 days). */
export const getBalanceHistory = (accountNumber: string, days: number): Promise<BalanceHistory> =>
  apiFetch<BalanceHistory>(
    `/api/transfers/account/${encodeURIComponent(accountNumber)}/balance-history?days=${days}`,
  );

/**
 * Keeps showing the previous range while another range loads (no flicker when switching 7 / 30 / 90 days),
 * but only for the same account: another account's line must never be shown under this account's name.
 */
export const useBalanceHistory = (accountNumber: string, days: number) =>
  useQuery({
    queryKey: queryKeys.accounts.balanceHistory(accountNumber, days),
    queryFn: () => getBalanceHistory(accountNumber, days),
    placeholderData: (previous) =>
      previous?.accountNumber === accountNumber ? previous : undefined,
  });
