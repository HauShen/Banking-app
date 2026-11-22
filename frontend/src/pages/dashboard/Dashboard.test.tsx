import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import type { Account, Transaction } from 'api/types';
import { clearAccessToken } from 'api/tokenStore';
import { http, HttpResponse } from 'msw';
import { historyOf, mockBalanceHistory } from 'test/balanceHistory';
import { mockRates } from 'test/fx';
import { mockSession, renderApp } from 'test/renderApp';
import { API, server } from 'test/server';
import { beforeEach, describe, expect, it } from 'vitest';
import TransferPage from 'pages/transfer';
import paths from 'routes/path';
import Dashboard from '.';

const customer = {
  id: 'u1',
  username: 'hau',
  fullName: 'Hau Shen',
  email: 'hau@banking.local',
  role: 'CUSTOMER',
};

const account = (overrides: Partial<Account>): Account => ({
  accountId: 1,
  userId: 'u1',
  accountNumber: '1111111111',
  accountType: 'SAVINGS',
  accountStatus: 'ACTIVE',
  accountCurrency: 'MYR',
  balance: 0,
  createdAt: '2026-11-01T00:00:00Z',
  ...overrides,
});

const savings = account({ accountId: 1, accountNumber: '1111111111', balance: 1500.5 });
const current = account({
  accountId: 2,
  accountNumber: '2222222222',
  accountType: 'CURRENT',
  accountStatus: 'LOCKED',
  balance: 250,
});

const tx = (overrides: Partial<Transaction>): Transaction => ({
  reference: 'TXN-1',
  type: 'TRANSFER',
  fromAccountNumber: '1111111111',
  toAccountNumber: '3333333333',
  amount: 20,
  currency: 'MYR',
  originalCurrency: null,
  originalAmount: null,
  exchangeRate: null,
  status: 'SUCCESS',
  description: 'Lunch',
  createdAt: '2026-11-17T12:00:00Z',
  postedAt: '2026-11-17T12:00:00Z',
  ...overrides,
});

const page = (content: Transaction[]) => ({
  content,
  totalElements: content.length,
  totalPages: 1,
  number: 0,
  size: 5,
});

/** Summary with the given accounts, and a transaction list per account number. */
const mockBank = (accounts: Account[], history: Record<string, Transaction[]> = {}) => {
  const requested: string[] = [];
  server.use(
    http.get(`${API}/api/accounts/summary`, () =>
      HttpResponse.json({
        balance: accounts.reduce((sum, a) => sum + a.balance, 0),
        accountType: accounts[0]?.accountType ?? 'N/A',
        accounts,
      }),
    ),
    http.get(`${API}/api/transfers/account/:accountNumber`, ({ params, request }) => {
      const accountNumber = params.accountNumber as string;
      requested.push(`${accountNumber}?${new URL(request.url).searchParams}`);
      return HttpResponse.json(page(history[accountNumber] ?? []));
    }),
  );
  mockBalanceHistory();
  return requested;
};

/** Intl uses no-break spaces in "RM 1,500.50"; compare with plain spaces. */
const text = (element: HTMLElement) => element.textContent?.replace(/\s/g, ' ') ?? '';

beforeEach(() => clearAccessToken());

describe('dashboard', () => {
  it("shows the user's accounts with balances, type, status and the total", async () => {
    mockSession(customer);
    mockBank([savings, current]);
    renderApp('/', {}, <Dashboard />);

    const savingsCard = await screen.findByRole('button', { name: /savings account 1111111111/i });
    const currentCard = screen.getByRole('button', { name: /current account 2222222222/i });

    expect(text(savingsCard)).toContain('RM 1,500.50');
    expect(text(savingsCard)).toContain('1111 111 111');
    expect(text(savingsCard)).toContain('Hau Shen');
    expect(within(currentCard).getByText('Locked')).toBeInTheDocument();
    expect(text(screen.getByText(/total balance/i))).toContain('RM 1,750.50');
  });

  it('lists the latest 5 transactions of the first account, then of the card the user picks', async () => {
    mockSession(customer);
    const requested = mockBank([savings, current], {
      '1111111111': [
        tx({ reference: 'A', description: 'Lunch' }),
        tx({
          reference: 'B',
          type: 'CASH_DEPOSIT',
          fromAccountNumber: 'Cash Deposit Machine',
          toAccountNumber: '1111111111',
          amount: 100,
          description: null,
        }),
      ],
      '2222222222': [
        tx({
          reference: 'C',
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
    });
    renderApp('/', {}, <Dashboard />);

    const lunch = await screen.findByText('Lunch');
    expect(text(lunch.closest('li') as HTMLElement)).toContain('−RM 20.00');
    expect(
      text(screen.getByText('Cash deposit', { selector: 'li *' }).closest('li') as HTMLElement),
    ).toContain('+RM 100.00');
    expect(requested[0]).toBe('1111111111?page=0&size=5');

    await userEvent.click(screen.getByRole('button', { name: /current account 2222222222/i }));

    const remittance = await screen.findByText('Foreign remittance from John');
    const row = text(remittance.closest('li') as HTMLElement);
    expect(row).toContain('+RM 460.00');
    expect(row).toContain('US$100.00 @ 4.6000');
    expect(screen.queryByText('Lunch')).not.toBeInTheDocument();
  });

  it('says so when an account has no transactions yet', async () => {
    mockSession(customer);
    mockBank([savings]);
    renderApp('/', {}, <Dashboard />);
    expect(await screen.findByText('No transactions yet')).toBeInTheDocument();
  });

  it('tells a new customer that an administrator opens accounts', async () => {
    mockSession(customer);
    const requested = mockBank([]);
    renderApp('/', {}, <Dashboard />);

    expect(await screen.findByText('No account yet')).toBeInTheDocument();
    expect(screen.getByText(/an administrator opens accounts/i)).toBeInTheDocument();
    expect(screen.queryByText('Recent Transactions')).not.toBeInTheDocument();
    expect(requested).toHaveLength(0);
  });

  it('shows an error with a retry button when the accounts cannot be loaded', async () => {
    mockSession(customer);
    server.use(
      http.get(`${API}/api/accounts/summary`, () =>
        HttpResponse.json({ status: 403, message: 'Forbidden' }, { status: 403 }),
      ),
    );
    renderApp('/', {}, <Dashboard />);

    expect(await screen.findByText("You don't have permission to do this.")).toBeInTheDocument();

    mockBank([savings]);
    await userEvent.click(screen.getByRole('button', { name: /try again/i }));
    expect(
      await screen.findByRole('button', { name: /savings account 1111111111/i }),
    ).toBeInTheDocument();
  });

  it('starts a transfer from the quick-transfer card with the details filled in', async () => {
    mockSession(customer);
    mockBank([savings]);
    mockRates();
    renderApp('/', {}, <Dashboard />, { [paths.transfer]: <TransferPage /> });

    const user = userEvent.setup();
    await user.type(await screen.findByLabelText(/to account number/i), '3333333333');
    await user.type(screen.getByLabelText(/^amount/i), '42');
    await user.click(screen.getByRole('button', { name: /continue/i }));

    expect(await screen.findByText('Send money')).toBeInTheDocument();
    expect(screen.getByDisplayValue('3333333333')).toBeInTheDocument();
    expect(screen.getByDisplayValue('42')).toBeInTheDocument();
  });
});

describe('balance history', () => {
  it("shows the selected account's last 30 days: today, highest and lowest, and a labelled chart", async () => {
    mockSession(customer);
    mockBank([savings, current]);
    const requests = mockBalanceHistory((accountNumber) =>
      HttpResponse.json(historyOf(accountNumber, [1200, 1650.25, 980, 1500.5])),
    );
    renderApp('/', {}, <Dashboard />);

    expect(text(await screen.findByText(/^Today/))).toBe('Today RM 1,500.50');
    expect(text(screen.getByText(/^Highest/))).toBe('Highest RM 1,650.25 on 18 Nov 2026');
    expect(text(screen.getByText(/^Lowest/))).toBe('Lowest RM 980.00 on 19 Nov 2026');
    expect(
      screen.getByRole('img', {
        name: 'Closing balance of 1111 111 111 per day, 17 Nov 2026 to 20 Nov 2026',
      }),
    ).toBeInTheDocument();
    expect(screen.getByRole('button', { name: '30 days' })).toHaveAttribute('aria-pressed', 'true');
    expect(requests).toEqual(['1111111111?days=30']);
  });

  it('asks again for another period, and for the card the user picks', async () => {
    mockSession(customer);
    mockBank([savings, current]);
    const requests = mockBalanceHistory();
    renderApp('/', {}, <Dashboard />);
    const user = userEvent.setup();
    await screen.findByText(/^Highest/);

    await user.click(screen.getByRole('button', { name: '7 days' }));
    await waitFor(() => expect(requests).toContain('1111111111?days=7'));
    await user.click(screen.getByRole('button', { name: /current account 2222222222/i }));

    await screen.findByRole('img', { name: /closing balance of 2222 222 222/i });
    expect(requests).toEqual(['1111111111?days=30', '1111111111?days=7', '2222222222?days=7']);
  });

  it('says when the account was opened today', async () => {
    mockSession(customer);
    mockBank([savings]);
    mockBalanceHistory((accountNumber) => HttpResponse.json(historyOf(accountNumber, [20])));
    renderApp('/', {}, <Dashboard />);

    expect(
      await screen.findByText('The account was opened today, so the line starts tomorrow.'),
    ).toBeInTheDocument();
  });

  it('shows an error with a retry', async () => {
    mockSession(customer);
    mockBank([savings]);
    let fail = true;
    mockBalanceHistory((accountNumber) =>
      fail
        ? HttpResponse.json({ status: 500, message: 'boom' }, { status: 500 })
        : HttpResponse.json(historyOf(accountNumber, [20, 30])),
    );
    renderApp('/', {}, <Dashboard />);

    const retry = await screen.findByRole('button', { name: /retry/i });
    expect(
      screen.getByText('Something went wrong on the server. Please try again later.'),
    ).toBeInTheDocument();
    fail = false;
    await userEvent.setup().click(retry);

    expect(text(await screen.findByText(/^Today/))).toBe('Today RM 30.00');
  });
});
