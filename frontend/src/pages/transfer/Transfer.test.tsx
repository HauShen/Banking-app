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

const account = (accountNumber: string, overrides: Partial<Account> = {}): Account => ({
  accountId: 1,
  userId: 'u1',
  accountNumber,
  accountType: 'SAVINGS',
  accountStatus: 'ACTIVE',
  accountCurrency: 'MYR',
  balance: 500,
  createdAt: '2026-11-01T00:00:00Z',
  ...overrides,
});

const success = (amount: number) =>
  HttpResponse.json(
    {
      transactionReference: 'TXN-ABC123',
      status: 'SUCCESS',
      createdAt: '2026-11-18T01:00:00Z',
      message: 'Transfer successful',
      amount,
      originalCurrency: null,
      originalAmount: null,
      exchangeRate: null,
    },
    { status: 201 },
  );

/** Summary with the given accounts; POST /api/transfers answered by `replies` in order. Records bodies. */
const mockBank = (accounts: Account[], replies: Array<() => Response> = []) => {
  const bodies: Array<Record<string, unknown>> = [];
  let summaryRequests = 0;
  server.use(
    http.get(`${API}/api/accounts/summary`, () => {
      summaryRequests++;
      return HttpResponse.json({ balance: 500, accountType: 'SAVINGS', accounts });
    }),
    http.post(`${API}/api/transfers`, async ({ request }) => {
      bodies.push((await request.json()) as Record<string, unknown>);
      const reply = replies[bodies.length - 1] ?? replies[replies.length - 1];
      return reply();
    }),
  );
  return { bodies, summaryRequests: () => summaryRequests };
};

const renderTransfer = (startAt: Parameters<typeof renderApp>[0] = paths.transfer) =>
  renderApp(startAt, {}, <p>Dashboard</p>, { [paths.transfer]: <TransferPage /> });

const fillAndReview = async (fields: { to?: string; amount?: string; description?: string }) => {
  const user = userEvent.setup();
  if (fields.to !== undefined) {
    const input = await screen.findByLabelText(/to account number/i);
    await user.clear(input);
    await user.type(input, fields.to);
  }
  if (fields.amount !== undefined) {
    const input = screen.getByLabelText(/^amount/i);
    await user.clear(input);
    await user.type(input, fields.amount);
  }
  if (fields.description)
    await user.type(screen.getByLabelText(/description/i), fields.description);
  await user.click(screen.getByRole('button', { name: /review transfer/i }));
};

const plain = (text: string | null | undefined) => (text ?? '').replace(/\s/g, ' ');

beforeEach(() => {
  clearAccessToken();
  mockSession(customer);
  mockRates();
  mockQuotes();
});

describe('transfer page', () => {
  it('sends a transfer after review, shows the receipt and refreshes the balances', async () => {
    const bank = mockBank([account('1111111111')], [() => success(50)]);
    renderTransfer();

    await fillAndReview({ to: '3333 333 333', amount: '50', description: ' Rent ' });

    // Review step: nothing sent yet.
    expect(await screen.findByText(/money moves as soon as you confirm/i)).toBeInTheDocument();
    expect(plain(document.body.textContent)).toContain('3333 333 333');
    expect(plain(document.body.textContent)).toContain('RM 50.00');
    expect(bank.bodies).toHaveLength(0);
    const summaryBefore = bank.summaryRequests();

    await userEvent.click(screen.getByRole('button', { name: /confirm transfer/i }));

    expect(await screen.findByText('TXN-ABC123')).toBeInTheDocument();
    expect(plain(screen.getByRole('heading', { name: /sent/ }).textContent)).toBe('RM 50.00 sent');
    expect(bank.bodies).toEqual([
      {
        fromAccountNumber: '1111111111',
        toAccountNumber: '3333333333',
        amount: '50.00',
        description: 'Rent',
        idempotencyKey: expect.stringMatching(/^[0-9a-f-]{36}$/),
      },
    ]);
    await screen.findByText('TXN-ABC123');
    expect(bank.summaryRequests()).toBeGreaterThan(summaryBefore);
  });

  it('checks the form before anything is sent', async () => {
    const bank = mockBank([account('1111111111')], [() => success(1)]);
    renderTransfer();

    await fillAndReview({ to: '123', amount: '1.234', description: '' });

    expect(await screen.findByText('Enter the 10-digit account number.')).toBeInTheDocument();
    expect(screen.getByText('Enter an amount, e.g. 50 or 12.50.')).toBeInTheDocument();
    expect(screen.getByText(/add a short description/i)).toBeInTheDocument();
    expect(bank.bodies).toHaveLength(0);
  });

  it('goes back to the form and marks the amount when the balance is too low', async () => {
    mockBank(
      [account('1111111111')],
      [
        () =>
          HttpResponse.json(
            {
              status: 400,
              message: 'Insufficient balance. Minimum balance of $20.00 must be maintained.',
            },
            { status: 400 },
          ),
      ],
    );
    renderTransfer();
    await fillAndReview({ to: '3333333333', amount: '490', description: 'Too much' });
    await userEvent.click(await screen.findByRole('button', { name: /confirm transfer/i }));

    expect(
      await screen.findByText('Not enough money: RM 20.00 must stay in the account.'),
    ).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /review transfer/i })).toBeInTheDocument();
  });

  it('reuses the same key when confirming again after a network error, so it is never booked twice', async () => {
    const bank = mockBank([account('1111111111')], [() => HttpResponse.error(), () => success(25)]);
    renderTransfer();
    await fillAndReview({ to: '3333333333', amount: '25', description: 'Retry me' });

    await userEvent.click(await screen.findByRole('button', { name: /confirm transfer/i }));
    expect(await screen.findByText(/you can confirm again safely/i)).toBeInTheDocument();

    await userEvent.click(screen.getByRole('button', { name: /confirm transfer/i }));
    expect(await screen.findByText('TXN-ABC123')).toBeInTheDocument();

    expect(bank.bodies).toHaveLength(2);
    expect(bank.bodies[1].idempotencyKey).toBe(bank.bodies[0].idempotencyKey);
  });

  it('uses a new key once the details have changed', async () => {
    const bank = mockBank(
      [account('1111111111')],
      [
        () =>
          HttpResponse.json(
            { status: 400, message: 'Selected account has been locked' },
            { status: 400 },
          ),
        () => success(30),
      ],
    );
    renderTransfer();
    await fillAndReview({ to: '3333333333', amount: '25', description: 'First try' });
    await userEvent.click(await screen.findByRole('button', { name: /confirm transfer/i }));
    expect(await screen.findByText(/recipient's account is locked/i)).toBeInTheDocument();

    await userEvent.click(screen.getByRole('button', { name: /back/i }));
    await fillAndReview({ to: '4444444444', amount: '30' });
    await userEvent.click(await screen.findByRole('button', { name: /confirm transfer/i }));
    expect(await screen.findByText('TXN-ABC123')).toBeInTheDocument();

    expect(bank.bodies[1].toAccountNumber).toBe('4444444444');
    expect(bank.bodies[1].idempotencyKey).not.toBe(bank.bodies[0].idempotencyKey);
  });

  it('starts with the details from the quick-transfer card', async () => {
    mockBank([account('1111111111'), account('2222222222', { accountId: 2 })]);
    renderTransfer({
      pathname: paths.transfer,
      state: { fromAccountNumber: '2222222222', toAccountNumber: '3333333333', amount: '12.50' },
    });

    expect(await screen.findByDisplayValue('3333333333')).toBeInTheDocument();
    expect(screen.getByDisplayValue('12.50')).toBeInTheDocument();
    expect(plain(screen.getByRole('combobox', { name: /from/i }).textContent)).toContain(
      '2222 222 222',
    );
  });

  it('explains that an active account is needed', async () => {
    mockBank([account('1111111111', { accountStatus: 'LOCKED' })]);
    renderTransfer();
    expect(
      await screen.findByText(/you need an active account to send money/i),
    ).toBeInTheDocument();
  });
});
