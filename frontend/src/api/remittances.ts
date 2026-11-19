import { useMutation, useQueryClient } from '@tanstack/react-query';
import { apiFetch } from './client';
import { queryKeys } from './queryKeys';

/** RemittanceRequestBody.java: money arriving from abroad in a foreign currency (admin only). */
export interface RemittanceRequest {
  toAccountNumber: string;
  /** Not MYR (that's a top-up). */
  currency: string;
  /** API string in `currency`, e.g. "100.00" (JPY "1000"). */
  amount: string;
  /** ≤ 100 characters; the history shows "Foreign remittance from <senderName>". */
  senderName: string;
  idempotencyKey: string;
}

/** RemittanceResponseBody.java */
export interface RemittanceResult {
  transactionReference: string;
  status: string;
  createdAt: string;
  /** "Remittance received", or the duplicate-request message when the key was used before. */
  message: string;
  toAccountNumber: string;
  /** MYR credited. */
  amount: number;
  originalCurrency: string;
  originalAmount: number;
  /** The bank's buy rate (MYR per unit). */
  exchangeRate: number;
}

/** POST /api/admin/remittances → 201. */
export const receiveRemittance = (request: RemittanceRequest) =>
  apiFetch<RemittanceResult>('/api/admin/remittances', { method: 'POST', body: request });

/** Credits the account, then re-fetches account lists, balances and histories. Never retried automatically. */
export const useReceiveRemittance = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: receiveRemittance,
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: queryKeys.admin.accounts.all });
      void queryClient.invalidateQueries({ queryKey: queryKeys.accounts.all });
      void queryClient.invalidateQueries({ queryKey: queryKeys.transactions.all });
    },
  });
};
