import { http, HttpResponse } from 'msw';
import { API, server } from './server';

/** Closing balances for consecutive days ending on `lastDay` ("2026-11-20"). */
export const historyOf = (accountNumber: string, closings: number[], lastDay = '2026-11-20') => {
  const end = new Date(`${lastDay}T00:00:00Z`);
  return {
    accountNumber,
    currency: 'MYR',
    timeZone: 'Asia/Kuala_Lumpur',
    days: closings.map((closingBalance, i) => {
      const day = new Date(end);
      day.setUTCDate(end.getUTCDate() - (closings.length - 1 - i));
      return { date: day.toISOString().slice(0, 10), closingBalance };
    }),
  };
};

/**
 * GET /balance-history for any account: three days by default, or what `reply` returns.
 * Records "accountNumber?days=N" for each request.
 */
export const mockBalanceHistory = (
  reply: (accountNumber: string, days: number) => Response = (accountNumber) =>
    HttpResponse.json(historyOf(accountNumber, [100, 150.5, 120])),
) => {
  const requests: string[] = [];
  server.use(
    http.get(
      `${API}/api/transfers/account/:accountNumber/balance-history`,
      ({ params, request }) => {
        const days = Number(new URL(request.url).searchParams.get('days'));
        requests.push(`${params.accountNumber as string}?days=${days}`);
        return reply(params.accountNumber as string, days);
      },
    ),
  );
  return requests;
};
