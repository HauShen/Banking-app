import { http, HttpResponse } from 'msw';
import { API, server } from './server';

/** The backend's configured test rates (application.properties fx.rates.*). */
export const RATES = [
  { currency: 'EUR', buyRate: 4.83, sellRate: 4.92, asOf: '2026-11-18T00:00:00Z' },
  { currency: 'JPY', buyRate: 0.0273, sellRate: 0.02795, asOf: '2026-11-18T00:00:00Z' },
  { currency: 'SGD', buyRate: 3.21, sellRate: 3.265, asOf: '2026-11-18T00:00:00Z' },
  { currency: 'USD', buyRate: 4.165, sellRate: 4.235, asOf: '2026-11-18T00:00:00Z' },
];

/** GET /api/fx/rates. */
export const mockRates = () =>
  server.use(http.get(`${API}/api/fx/rates`, () => HttpResponse.json(RATES)));

/** GET /api/fx/quote: amount × sell rate (PAY) or buy rate (RECEIVE), rounded to sen. */
export const mockQuotes = () =>
  server.use(
    http.get(`${API}/api/fx/quote`, ({ request }) => {
      const url = new URL(request.url);
      const currency = url.searchParams.get('currency') as string;
      const amount = Number(url.searchParams.get('amount'));
      const direction = url.searchParams.get('direction') ?? 'PAY';
      const rate = RATES.find((r) => r.currency === currency);
      if (!rate) {
        return HttpResponse.json(
          { status: 400, message: `Currency ${currency} is not supported` },
          { status: 400 },
        );
      }
      const exchangeRate = direction === 'PAY' ? rate.sellRate : rate.buyRate;
      return HttpResponse.json({
        currency,
        amount,
        direction,
        exchangeRate,
        myrAmount: Math.round(amount * exchangeRate * 100) / 100,
        asOf: rate.asOf,
      });
    }),
  );
