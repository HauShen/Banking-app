import { screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import type { Account, Transaction } from 'api/types';
import { clearAccessToken } from 'api/tokenStore';
import { http, HttpResponse } from 'msw';
import paths from 'routes/path';
import { mockSession, renderApp } from 'test/renderApp';
import { API, server } from 'test/server';
import { beforeEach, describe, expect, it } from 'vitest';
import TransactionsPage from '.';

const customer = {
  id: 'u1',
  username: 'hau',
  fullName: 'Hau Shen',
  email: 'h@b.local',
  role: 'CUSTOMER',
};

const account = (accountNumber: string, overrides: Partial<Account> = {}): Account => ({
  accountId: Number(accountNumber.slice(0, 1)),
  userId: 'u1',
  accountNumber,
  accountType: 'SAVINGS',
  accountStatus: 'ACTIVE',
  accountCurrency: 'MYR',
  balance: 100,
  createdAt: '2026-11-01T00:00:00Z',
  ...overrides,
});

const tx = (reference: string, overrides: Partial<Transaction> = {}): Transaction => ({
  reference,
  type: 'TRANSFER',
  fromAccountNumber: '1111111111',
  toAccountNumber: '3333333333',
  amount: 10,
  currency: 'MYR',
  originalCurrency: null,
  originalAmount: null,
  exchangeRate: null,
  status: 'SUCCESS',
  description: `Payment ${reference}`,
  createdAt: '2026-11-17T12:00:00Z',
  postedAt: '2026-11-17T12:00:01Z',
  ...overrides,
});

/**
 * Accounts 1111111111 (25 transactions) and 2222222222 (one foreign remittance).
 * Records every history request as "account?page=&size=".
 */
const mockBank = (
  accounts: Account[] = [account('1111111111'), account('2222222222', { accountType: 'CURRENT' })],
) => {
  const all: Record<string, Transaction[]> = {
    '1111111111': Array.from({ length: 25 }, (_, i) => tx(`T${i + 1}`)),
    '2222222222': [
      tx('RMT-1', {
        type: 'FOREIGN_REMITTANCE',
        fromAccountNumber: 'Foreign Remittance',
        toAccountNumber: '2222222222',
        amount: 460,
        originalCurrency: 'USD',
        originalAmount: 100,
        exchangeRate: 4.6,
        description: 'Foreign remittance from John',
      }),
    ],
  };
  const requests: string[] = [];
  server.use(
    http.get(`${API}/api/accounts/summary`, () =>
      HttpResponse.json({ balance: 200, accountType: 'SAVINGS', accounts }),
    ),
    http.get(`${API}/api/transfers/account/:accountNumber`, ({ params, request }) => {
      const url = new URL(request.url);
      const page = Number(url.searchParams.get('page'));
      const size = Number(url.searchParams.get('size'));
      const list = all[params.accountNumber as string] ?? [];
      requests.push(`${params.accountNumber}?page=${page}&size=${size}`);
      return HttpResponse.json({
        content: list.slice(page * size, page * size + size),
        totalElements: list.length,
        totalPages: Math.ceil(list.length / size),
        number: page,
        size,
      });
    }),
  );
  return requests;
};

const renderPage = (url = paths.transactions) =>
  renderApp(url, {}, <p>Dashboard</p>, { [paths.transactions]: <TransactionsPage /> });

const plain = (text: string | null | undefined) => (text ?? '').replace(/\s/g, ' ');

beforeEach(() => {
  clearAccessToken();
  mockSession(customer);
});

describe('transactions page', () => {
  it('shows the first page of the first account, and asks the server for the next page', async () => {
    const requests = mockBank();
    renderPage();

    expect(await screen.findByText('Payment T1')).toBeInTheDocument();
    expect(screen.getByText('Payment T10')).toBeInTheDocument();
    expect(screen.queryByText('Payment T11')).not.toBeInTheDocument();
    expect(screen.getByText(/1–10 of 25/)).toBeInTheDocument();
    // The right-aligned amounts come last, so they don't run into the next column.
    expect(screen.getAllByRole('columnheader').slice(-1)[0]).toHaveTextContent('Amount');

    await userEvent.click(screen.getByRole('button', { name: /go to next page/i }));

    expect(await screen.findByText('Payment T11')).toBeInTheDocument();
    expect(requests).toEqual(['1111111111?page=0&size=10', '1111111111?page=1&size=10']);
  });

  it('opens the account from the URL (the dashboard "See all" link)', async () => {
    const requests = mockBank();
    renderPage(`${paths.transactions}?account=2222222222`);

    expect(await screen.findByText('Foreign remittance from John')).toBeInTheDocument();
    expect(requests).toEqual(['2222222222?page=0&size=10']);
  });

  it('switches account with the selector and starts again at the first page', async () => {
    const requests = mockBank();
    renderPage();
    await screen.findByText('Payment T1');
    await userEvent.click(screen.getByRole('button', { name: /go to next page/i }));
    await screen.findByText('Payment T11');

    await userEvent.click(screen.getByRole('combobox', { name: /account/i }));
    await userEvent.click(await screen.findByRole('option', { name: /2222 222 222/ }));

    expect(await screen.findByText('Foreign remittance from John')).toBeInTheDocument();
    expect(screen.queryByText('Payment T11')).not.toBeInTheDocument();
    expect(requests[requests.length - 1]).toBe('2222222222?page=0&size=10');
  });

  it('shows all details of a transaction when its row is clicked', async () => {
    mockBank();
    renderPage(`${paths.transactions}?account=2222222222`);

    await userEvent.click(await screen.findByText('Foreign remittance from John'));

    const details = await screen.findByRole('dialog', { name: /transaction details/i });
    expect(plain(details.textContent)).toContain('+RM 460.00');
    expect(
      within(details).getByText('Foreign remittance', { selector: 'div' }),
    ).toBeInTheDocument();
    expect(plain(details.textContent)).toContain('US$100.00');
    expect(plain(details.textContent)).toContain('1 USD = RM 4.6000');
    expect(within(details).getByText('RMT-1')).toBeInTheDocument();
    expect(within(details).getByText('Completed')).toBeInTheDocument();

    await userEvent.click(within(details).getByRole('button', { name: /close/i }));
    expect(screen.queryByRole('dialog', { name: /transaction details/i })).not.toBeInTheDocument();
  });

  it('says so when the account has no transactions', async () => {
    mockBank([account('9999999999')]);
    renderPage();
    expect(await screen.findByText('No transactions yet')).toBeInTheDocument();
  });

  it('explains that there is nothing to show without an account', async () => {
    const requests = mockBank([]);
    renderPage();
    expect(await screen.findByText('No account yet')).toBeInTheDocument();
    expect(requests).toHaveLength(0);
  });
});
