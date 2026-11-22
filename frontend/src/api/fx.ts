import { useMutation, useQuery } from '@tanstack/react-query';
import { apiFetch } from './client';
import { queryKeys } from './queryKeys';

/** ExchangeRateResponseBody.java: MYR per 1 unit of `currency`. */
export interface ExchangeRate {
  currency: string;
  /** The bank buys foreign currency at this rate (money coming in, e.g. a remittance). */
  buyRate: number;
  /** The bank sells foreign currency at this rate (the customer pays in it). */
  sellRate: number;
  asOf: string;
}

/** PAY: the customer pays `amount` in the foreign currency (sell rate). RECEIVE: money comes in (buy rate). */
export type QuoteDirection = 'PAY' | 'RECEIVE';

/** QuoteResponseBody.java: informational, the rate right now. */
export interface Quote {
  currency: string;
  amount: number;
  direction: QuoteDirection;
  exchangeRate: number;
  myrAmount: number;
  asOf: string;
}

/** LockedQuoteResponseBody.java: this rate is kept for this user for one transfer, until `expiresAt`. */
export interface LockedQuote {
  quoteId: string;
  currency: string;
  amount: number;
  exchangeRate: number;
  myrAmount: number;
  expiresAt: string;
}

export interface QuoteRequest {
  currency: string;
  /** API string, e.g. "100.00" (from normalizeAmountInput). */
  amount: string;
  direction?: QuoteDirection;
}

export const getRates = (): Promise<ExchangeRate[]> => apiFetch<ExchangeRate[]>('/api/fx/rates');

/** The offered currencies with their rates. The rates are configured, so they rarely change. */
export const useRates = () =>
  useQuery({ queryKey: queryKeys.fx.rates(), queryFn: getRates, staleTime: 5 * 60_000 });

export const getQuote = ({ currency, amount, direction = 'PAY' }: QuoteRequest): Promise<Quote> =>
  apiFetch<Quote>(
    `/api/fx/quote?currency=${encodeURIComponent(currency)}&amount=${encodeURIComponent(amount)}&direction=${direction}`,
  );

/** Live, informational quote; disabled while there is nothing valid to quote. */
export const useQuote = (request: QuoteRequest | undefined) =>
  useQuery({
    queryKey: queryKeys.fx.quote(
      request?.currency ?? '',
      request?.amount ?? '',
      request?.direction ?? 'PAY',
    ),
    queryFn: () => getQuote(request as QuoteRequest),
    enabled: Boolean(request),
    retry: false,
  });

/** POST /api/fx/quotes → 201: locks the sell rate for paying `amount` in `currency`. */
export const lockQuote = (
  request: Pick<QuoteRequest, 'currency' | 'amount'>,
): Promise<LockedQuote> =>
  apiFetch<LockedQuote>('/api/fx/quotes', { method: 'POST', body: request });

export const useLockQuote = () => useMutation({ mutationFn: lockQuote });
