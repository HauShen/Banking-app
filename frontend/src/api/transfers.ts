import { useMutation, useQueryClient } from '@tanstack/react-query';
import { apiFetch } from './client';
import { queryKeys } from './queryKeys';

/** TransferRequestBody.java. `amount` is a string ("50.00") so no float rounding happens on the way. */
export interface TransferRequest {
  fromAccountNumber: string;
  toAccountNumber: string;
  amount: string;
  /** Currency of `amount`; absent = MYR. */
  currency?: string;
  description: string;
  idempotencyKey: string;
  quoteId?: string;
}

/** TransferResponseBody.java */
export interface TransferResult {
  transactionReference: string;
  status: string;
  createdAt: string;
  /** "Transfer successful", or the duplicate-request message when the key was used before. */
  message: string;
  amount: number;
  originalCurrency: string | null;
  originalAmount: number | null;
  exchangeRate: number | null;
}

/** POST /api/transfers → 201. */
export const createTransfer = (request: TransferRequest): Promise<TransferResult> =>
  apiFetch<TransferResult>('/api/transfers', { method: 'POST', body: request });

/** Sends a transfer, then re-fetches balances and histories (they all changed). Never retried automatically. */
export const useCreateTransfer = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: createTransfer,
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: queryKeys.accounts.all });
      void queryClient.invalidateQueries({ queryKey: queryKeys.transactions.all });
    },
  });
};
