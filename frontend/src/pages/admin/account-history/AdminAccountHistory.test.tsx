import { screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { clearAccessToken } from 'api/tokenStore';
import type { Account, Transaction, UserProfile } from 'api/types';
import { http, HttpResponse } from 'msw';
import paths, { adminAccountHistoryPath } from 'routes/path';
import RequireRole from 'routes/RequireRole';
import { historyOf, mockBalanceHistory } from 'test/balanceHistory';
import { mockSession, renderApp } from 'test/renderApp';
import { API, server } from 'test/server';
import { beforeEach, describe, expect, it } from 'vitest';
import AdminAccountHistoryPage from '.';

const person = (id: string, username: string, role: UserProfile['role']): UserProfile => ({
  id,
  username,
  fullName: `${username} full`,
  email: `${username}@banking.local`,
  role,
  createdAt: '2026-11-01T12:00:00Z',
  updatedAt: '2026-11-01T12:00:00Z',
});
const devadmin = person('a1', 'devadmin', 'ADMIN');
const hau = person('c1', 'hau', 'CUSTOMER');

const haus: Account = {
  accountId: 7,
  userId: 'c1',
  accountNumber: '1000000007',
  accountType: 'CURRENT',
  accountStatus: 'LOCKED',
  accountCurrency: 'MYR',
  balance: 480.5,
  createdAt: '2026-11-03T02:00:00Z',
};

const tx = (overrides: Partial<Transaction>): Transaction => ({
  reference: 'TXN-1',
  type: 'TRANSFER',
  fromAccountNumber: '1000000007',
  toAccountNumber: '2000000002',
  amount: 30,
  currency: 'MYR',
  originalCurrency: null,
  originalAmount: null,
  exchangeRate: null,
  status: 'SUCCESS',
  description: 'Rent share',
  createdAt: '2026-11-18T04:00:00Z',
  postedAt: '2026-11-18T04:00:00Z',
  ...overrides,
});

/** The account, its owner, its history (records the pages asked for) and its balance history. */
const mockAccount = (transactions: Transaction[] = []) => {
  const pages: string[] = [];
  server.use(
    http.get(`${API}/api/accounts/number/:accountNumber`, ({ params }) =>
      params.accountNumber === haus.accountNumber
        ? HttpResponse.json(haus)
        : HttpResponse.json(
            { status: 404, message: `Account not found: ${params.accountNumber as string}` },
            { status: 404 },
          ),
    ),
    http.get(`${API}/api/admin/users/:id`, ({ params }) =>
      HttpResponse.json([devadmin, hau].find((u) => u.id === params.id)),
    ),
    http.get(`${API}/api/transfers/account/:accountNumber`, ({ params, request }) => {
      pages.push(`${params.accountNumber as string}?${new URL(request.url).searchParams}`);
      return HttpResponse.json({
        content: transactions,
        totalElements: transactions.length,
        totalPages: 1,
        number: 0,
        size: 10,
      });
    }),
  );
  mockBalanceHistory((accountNumber) =>
    HttpResponse.json(historyOf(accountNumber, [500.5, 480.5])),
  );
  return pages;
};

const renderHistory = (accountNumber = haus.accountNumber) =>
  renderApp(adminAccountHistoryPath(accountNumber), {}, <p>Dashboard</p>, {
    [paths.adminAccountHistory]: (
      <RequireRole role="ADMIN">
        <AdminAccountHistoryPage />
      </RequireRole>
    ),
    [paths.adminAccounts]: <p>All accounts page</p>,
  });

const plain = (text: string | null | undefined) => (text ?? '').replace(/\s/g, ' ');

beforeEach(() => clearAccessToken());

describe("admin: an account's history", () => {
  it('shows the account with its owner, its transactions and its balance chart', async () => {
    mockSession(devadmin);
    const pages = mockAccount([tx({ reference: 'A' })]);
    renderHistory();

    expect(await screen.findByText('1000 000 007')).toBeInTheDocument();
    expect(await screen.findByText('hau full (hau)')).toBeInTheDocument();
    expect(screen.getByText('Locked')).toBeInTheDocument();
    expect(plain(screen.getByText(/RM 480\.50/, { selector: 'div' }).textContent)).toBe(
      'RM 480.50',
    );
    expect(screen.getByText('Current')).toBeInTheDocument();

    const row = (await screen.findByText('Rent share')).closest('[role="row"]') as HTMLElement;
    expect(plain(row.textContent)).toContain('−RM 30.00'); // out of this account
    expect(pages).toEqual(['1000000007?page=0&size=10']);
    expect(
      await screen.findByRole('img', { name: /closing balance of 1000 000 007/i }),
    ).toBeInTheDocument();
  });

  it('opens a transaction in the details panel', async () => {
    mockSession(devadmin);
    mockAccount([tx({ reference: 'TXN-REF-123' })]);
    renderHistory();
    const user = userEvent.setup();

    await user.click(await screen.findByText('Rent share'));

    const details = await screen.findByRole('dialog', { name: /transaction details/i });
    expect(within(details).getByText('TXN-REF-123')).toBeInTheDocument();
  });

  it('says so for an unknown account, with a way back to all accounts', async () => {
    mockSession(devadmin);
    mockAccount();
    renderHistory('9999999999');

    expect(await screen.findByText('There is no account 9999 999 999.')).toBeInTheDocument();
    await userEvent.setup().click(screen.getByRole('link', { name: /all accounts/i }));
    expect(await screen.findByText('All accounts page')).toBeInTheDocument();
  });

  it('is not shown to a customer', async () => {
    mockSession(hau);
    const pages = mockAccount([tx({})]);
    renderHistory();

    expect(await screen.findByText('Dashboard')).toBeInTheDocument();
    expect(pages).toHaveLength(0);
  });
});
