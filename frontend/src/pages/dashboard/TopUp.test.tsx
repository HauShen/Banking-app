import { screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import type { Account } from 'api/types';
import { clearAccessToken } from 'api/tokenStore';
import { http, HttpResponse } from 'msw';
import { mockSession, renderApp } from 'test/renderApp';
import { API, server } from 'test/server';
import { beforeEach, describe, expect, it } from 'vitest';
import Dashboard from '.';

const customer = {
  id: 'u1',
  username: 'hau',
  fullName: 'Hau Shen',
  email: 'h@b.local',
  role: 'CUSTOMER',
};
const savings: Account = {
  accountId: 7,
  userId: 'u1',
  accountNumber: '1111111111',
  accountType: 'SAVINGS',
  accountStatus: 'ACTIVE',
  accountCurrency: 'MYR',
  balance: 1500.5,
  createdAt: '2026-11-01T00:00:00Z',
};

/** The summary reflects deposits made through PATCH /topup; `reply` can replace the deposit's answer. */
const mockBank = (account: Account, reply?: () => Response) => {
  let balance = account.balance;
  const topUps: string[] = [];
  server.use(
    http.get(`${API}/api/accounts/summary`, () =>
      HttpResponse.json({
        balance,
        accountType: account.accountType,
        accounts: [{ ...account, balance }],
      }),
    ),
    http.get(`${API}/api/transfers/account/:accountNumber`, () =>
      HttpResponse.json({ content: [], totalElements: 0, totalPages: 0, number: 0, size: 5 }),
    ),
    http.patch(`${API}/api/accounts/:id/topup`, ({ params, request }) => {
      const amount = new URL(request.url).searchParams.get('amount') as string;
      topUps.push(`${params.id}?amount=${amount}`);
      if (reply) return reply();
      balance = Math.round((balance + Number(amount)) * 100) / 100;
      return HttpResponse.json({ ...account, balance });
    }),
  );
  return topUps;
};

const plain = (text: string | null | undefined) => (text ?? '').replace(/\s/g, ' ');

const openDepositAndType = async (amount: string) => {
  const user = userEvent.setup();
  await user.click(await screen.findByRole('button', { name: /cash deposit/i }));
  const dialog = await screen.findByRole('dialog');
  await user.type(within(dialog).getByLabelText(/amount/i), amount);
  await user.click(within(dialog).getByRole('button', { name: /^deposit$/i }));
  return dialog;
};

beforeEach(() => {
  clearAccessToken();
  mockSession(customer);
});

describe('cash deposit', () => {
  it('deposits into the selected account and shows the new balance', async () => {
    const topUps = mockBank(savings);
    renderApp('/', {}, <Dashboard />);

    await openDepositAndType('100');

    const message = await screen.findByText(/deposited into/);
    expect(plain(message.textContent)).toBe(
      'RM 100.00 deposited into 1111 111 111. New balance: RM 1,600.50.',
    );
    expect(topUps).toEqual(['7?amount=100.00']);
    // The card shows the re-fetched balance.
    const card = await screen.findByRole('button', { name: /savings account 1111111111/i });
    await screen.findByText(
      (_, el) => el === card && plain(card.textContent).includes('RM 1,600.50'),
    );
  });

  it('checks the amount before sending anything', async () => {
    const topUps = mockBank(savings);
    renderApp('/', {}, <Dashboard />);

    await openDepositAndType('0');

    expect(await screen.findByText('Enter an amount, e.g. 100 or 50.50.')).toBeInTheDocument();
    expect(topUps).toHaveLength(0);
  });

  it('asks the user to check the balance instead of retrying after a network error', async () => {
    const topUps = mockBank(savings, () => HttpResponse.error());
    renderApp('/', {}, <Dashboard />);

    const dialog = await openDepositAndType('50');

    expect(
      await within(dialog).findByText(/check your balance before trying again/i),
    ).toBeInTheDocument();
    expect(topUps).toHaveLength(1);
  });

  it('shows the backend message for a refused deposit', async () => {
    mockBank(savings, () =>
      HttpResponse.json(
        { status: 400, message: 'Cannot top up a non-active account' },
        { status: 400 },
      ),
    );
    renderApp('/', {}, <Dashboard />);

    const dialog = await openDepositAndType('50');
    expect(
      await within(dialog).findByText('Cannot top up a non-active account'),
    ).toBeInTheDocument();
  });

  it('is not offered for a locked account', async () => {
    mockBank({ ...savings, accountStatus: 'LOCKED' });
    renderApp('/', {}, <Dashboard />);

    expect(await screen.findByRole('button', { name: /cash deposit/i })).toBeDisabled();
    expect(screen.getByText("Locked accounts can't receive deposits.")).toBeInTheDocument();
  });
});
