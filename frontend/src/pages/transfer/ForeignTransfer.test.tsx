import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import type { Account } from 'api/types';
import { clearAccessToken } from 'api/tokenStore';
import { http, HttpResponse } from 'msw';
import paths from 'routes/path';
import { mockQuotes, mockRates } from 'test/fx';
import { mockSession, renderApp } from 'test/renderApp';
import { API, server } from 'test/server';
import { beforeEach, describe, expect, it } from 'vitest';
import TransferPage from '.';

const customer = {
  id: 'u1',
  username: 'hau',
  fullName: 'Hau Shen',
  email: 'h@b.local',
  role: 'CUSTOMER',
};
const savings: Account = {
  accountId: 1,
  userId: 'u1',
  accountNumber: '1111111111',
  accountType: 'SAVINGS',
  accountStatus: 'ACTIVE',
  accountCurrency: 'MYR',
  balance: 1000,
  createdAt: '2026-11-01T00:00:00Z',
};

/** Locked quote for 100 USD at the sell rate, expiring `validForMs` from now. */
const lockedQuote = (id: string, validForMs = 60_000) => ({
  quoteId: id,
  currency: 'USD',
  amount: 100,
  exchangeRate: 4.235,
  myrAmount: 423.5,
  expiresAt: new Date(Date.now() + validForMs).toISOString(),
});

/** A function: a Response body can be read only once, so every request needs a fresh one. */
const fxReceipt = () =>
  HttpResponse.json(
    {
      transactionReference: 'TXN-FX1',
      status: 'SUCCESS',
      createdAt: '2026-11-18T01:00:00Z',
      message: 'Transfer successful',
      amount: 423.5,
      originalCurrency: 'USD',
      originalAmount: 100,
      exchangeRate: 4.235,
    },
    { status: 201 },
  );

/** Locks answered by `locks` in order; transfers by `transfers` in order. Records both bodies. */
const mockFxBank = (locks: Array<() => Response>, transfers: Array<() => Response>) => {
  const lockBodies: unknown[] = [];
  const transferBodies: Array<Record<string, unknown>> = [];
  server.use(
    http.get(`${API}/api/accounts/summary`, () =>
      HttpResponse.json({ balance: 1000, accountType: 'SAVINGS', accounts: [savings] }),
    ),
    http.post(`${API}/api/fx/quotes`, async ({ request }) => {
      lockBodies.push(await request.json());
      return (locks[lockBodies.length - 1] ?? locks[locks.length - 1])();
    }),
    http.post(`${API}/api/transfers`, async ({ request }) => {
      transferBodies.push((await request.json()) as Record<string, unknown>);
      return (transfers[transferBodies.length - 1] ?? transfers[transfers.length - 1])();
    }),
  );
  return { lockBodies, transferBodies };
};

const fillUsdTransfer = async () => {
  const user = userEvent.setup();
  await user.type(await screen.findByLabelText(/to account number/i), '3333333333');
  await user.click(screen.getByRole('combobox', { name: /currency/i }));
  await user.click(await screen.findByRole('option', { name: 'USD' }));
  await user.type(screen.getByLabelText(/^amount/i), '100');
  await user.type(screen.getByLabelText(/description/i), 'Course fee');
  return user;
};

const renderTransfer = () =>
  renderApp(paths.transfer, {}, <p>Dashboard</p>, { [paths.transfer]: <TransferPage /> });

const plain = (text: string | null | undefined) => (text ?? '').replace(/\s/g, ' ');

beforeEach(() => {
  clearAccessToken();
  mockSession(customer);
  mockRates();
  mockQuotes();
});

describe('foreign-currency transfer', () => {
  it('shows the live rate, locks it on review and sends the transfer with the quote', async () => {
    const bank = mockFxBank(
      [() => HttpResponse.json(lockedQuote('q-1'), { status: 201 })],
      [fxReceipt],
    );
    renderTransfer();
    const user = await fillUsdTransfer();

    // Informational quote while typing (sell rate 4.2350).
    expect(await screen.findByText(/≈ RM 423\.50 from your account/)).toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: /review transfer/i }));

    expect(await screen.findByText('Rate locked: 1 USD = RM 4.2350')).toBeInTheDocument();
    expect(screen.getByLabelText(/time left to confirm/i)).toHaveTextContent(/0:5\d left/);
    expect(bank.lockBodies).toEqual([{ currency: 'USD', amount: '100.00' }]);

    await user.click(screen.getByRole('button', { name: /confirm transfer/i }));

    expect(await screen.findByText('TXN-FX1')).toBeInTheDocument();
    expect(plain(screen.getByRole('heading', { name: /sent/ }).textContent)).toBe('US$100.00 sent');
    expect(plain(document.body.textContent)).toContain('Paid from your account');
    expect(plain(document.body.textContent)).toContain('RM 423.50');
    expect(bank.transferBodies[0]).toMatchObject({
      currency: 'USD',
      amount: '100.00',
      quoteId: 'q-1',
      toAccountNumber: '3333333333',
    });
  });

  it('stops Confirm when the locked rate runs out, until a new rate is locked', async () => {
    const bank = mockFxBank(
      [
        // Expires 2.5 s from now; the page stops offering it 2 s early, so after ~0.5 s.
        () => HttpResponse.json(lockedQuote('q-short', 2_500), { status: 201 }),
        () => HttpResponse.json(lockedQuote('q-new'), { status: 201 }),
      ],
      [fxReceipt],
    );
    renderTransfer();
    const user = await fillUsdTransfer();
    await user.click(screen.getByRole('button', { name: /review transfer/i }));

    expect(await screen.findByText('The locked rate has expired.')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /confirm transfer/i })).toBeDisabled();

    await user.click(screen.getByRole('button', { name: /get a new rate/i }));
    expect(await screen.findByText('Rate locked: 1 USD = RM 4.2350')).toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: /confirm transfer/i }));
    expect(await screen.findByText('TXN-FX1')).toBeInTheDocument();
    expect(bank.transferBodies[0].quoteId).toBe('q-new');
  });

  it('offers a new rate when the backend says the quote has expired', async () => {
    mockFxBank(
      [() => HttpResponse.json(lockedQuote('q-1'), { status: 201 })],
      [() => HttpResponse.json({ status: 400, message: 'The quote has expired' }, { status: 400 })],
    );
    renderTransfer();
    const user = await fillUsdTransfer();
    await user.click(screen.getByRole('button', { name: /review transfer/i }));
    await user.click(await screen.findByRole('button', { name: /confirm transfer/i }));

    expect(await screen.findByText(/get a new rate to continue/i)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /get a new rate/i })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /confirm transfer/i })).toBeDisabled();
  });

  it('checks JPY amounts for decimals before asking for a rate', async () => {
    const bank = mockFxBank(
      [() => HttpResponse.json(lockedQuote('q-1'), { status: 201 })],
      [fxReceipt],
    );
    renderTransfer();
    const user = userEvent.setup();
    await user.type(await screen.findByLabelText(/to account number/i), '3333333333');
    await user.click(screen.getByRole('combobox', { name: /currency/i }));
    await user.click(await screen.findByRole('option', { name: 'JPY' }));
    await user.type(screen.getByLabelText(/^amount/i), '10.5');
    await user.type(screen.getByLabelText(/description/i), 'Snacks');
    await user.click(screen.getByRole('button', { name: /review transfer/i }));

    expect(await screen.findByText('Enter a whole number of JPY, e.g. 1000.')).toBeInTheDocument();
    expect(bank.lockBodies).toHaveLength(0);
  });
});
