import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { clearAccessToken } from 'api/tokenStore';
import type { Account, UserProfile } from 'api/types';
import { http, HttpResponse } from 'msw';
import paths from 'routes/path';
import { mockQuotes, mockRates } from 'test/fx';
import { mockSession, renderApp } from 'test/renderApp';
import { API, server } from 'test/server';
import { beforeEach, describe, expect, it } from 'vitest';
import AdminAccountsPage from '.';

const devadmin: UserProfile = {
  id: 'a1',
  username: 'devadmin',
  fullName: 'Dev Admin',
  email: 'devadmin@banking.local',
  role: 'ADMIN',
  createdAt: '2026-11-01T12:00:00Z',
  updatedAt: '2026-11-01T12:00:00Z',
};

const savings = (overrides: Partial<Account> = {}): Account => ({
  accountId: 7,
  userId: 'c1',
  accountNumber: '1000000007',
  accountType: 'SAVINGS',
  accountStatus: 'ACTIVE',
  accountCurrency: 'MYR',
  balance: 100,
  createdAt: '2026-11-01T00:00:00Z',
  ...overrides,
});

/** One account; POST /api/admin/remittances answered by `replies` in order (default: credit at the buy rate). */
const mockBank = (account: Account, replies: Array<() => Response> = []) => {
  let balance = account.balance;
  const bodies: Array<Record<string, unknown>> = [];
  server.use(
    http.get(`${API}/api/admin/users`, () =>
      HttpResponse.json({ content: [devadmin], page: 0, size: 100, totalElements: 1 }),
    ),
    http.get(`${API}/api/accounts/get_all`, () => HttpResponse.json([{ ...account, balance }])),
    http.post(`${API}/api/admin/remittances`, async ({ request }) => {
      const body = (await request.json()) as Record<string, unknown>;
      bodies.push(body);
      const reply = replies[bodies.length - 1];
      if (reply) return reply();
      const credited = Math.round(Number(body.amount) * 4.165 * 100) / 100;
      balance = Math.round((balance + credited) * 100) / 100;
      return HttpResponse.json(
        {
          transactionReference: 'RMT-0123456789AB',
          status: 'SUCCESS',
          createdAt: '2026-11-19T14:00:00Z',
          message: 'Remittance received',
          toAccountNumber: account.accountNumber,
          amount: credited,
          originalCurrency: body.currency,
          originalAmount: Number(body.amount),
          exchangeRate: 4.165,
        },
        { status: 201 },
      );
    }),
  );
  return bodies;
};

const renderAccounts = () =>
  renderApp(paths.adminAccounts, {}, <p>Dashboard</p>, {
    [paths.adminAccounts]: <AdminAccountsPage />,
  });

const plain = (text: string | null | undefined) => (text ?? '').replace(/\s/g, ' ');

/** Opens the account panel, then the remittance dialog, and fills it in. */
const fillRemittance = async (amount: string, sender: string) => {
  const user = userEvent.setup();
  await user.click(await screen.findByText('1000 000 007'));
  const panel = await screen.findByRole('dialog', { name: /account details/i });
  await user.click(within(panel).getByRole('button', { name: /receive remittance/i }));
  const dialog = await screen.findByRole('dialog', { name: /receive remittance/i });
  await user.type(within(dialog).getByLabelText(/amount/i), amount);
  if (sender) await user.type(within(dialog).getByLabelText(/sender name/i), sender);
  return { user, dialog, panel };
};

beforeEach(() => {
  clearAccessToken();
  mockSession(devadmin);
  mockRates();
  mockQuotes();
});

describe('receive remittance', () => {
  it('shows the expected credit, records the remittance and shows the receipt and new balance', async () => {
    const bodies = mockBank(savings());
    renderAccounts();
    const { user, dialog, panel } = await fillRemittance('100', ' John Tan ');

    // Informational quote at the buy rate (4.1650), before anything is sent.
    expect(await within(dialog).findByText(/≈ RM 416\.50 will be credited/)).toBeInTheDocument();
    expect(bodies).toHaveLength(0);

    await user.click(within(dialog).getByRole('button', { name: /^receive$/i }));

    expect(await within(dialog).findByText('RMT-0123456789AB')).toBeInTheDocument();
    expect(plain(dialog.textContent)).toContain('+RM 416.50');
    expect(plain(dialog.textContent)).toContain('1 USD = RM 4.1650');
    expect(bodies).toEqual([
      {
        toAccountNumber: '1000000007',
        currency: 'USD',
        amount: '100.00',
        senderName: 'John Tan',
        idempotencyKey: expect.stringMatching(/^[0-9a-f-]{36}$/),
      },
    ]);
    // The account list is re-fetched: the panel shows the new balance.
    await waitFor(() => expect(plain(panel.textContent)).toContain('RM 516.50'));
  });

  it('checks the form before sending anything', async () => {
    const bodies = mockBank(savings());
    renderAccounts();
    const { user, dialog } = await fillRemittance('1.234', '');

    await user.click(within(dialog).getByRole('button', { name: /^receive$/i }));

    expect(
      await within(dialog).findByText('Enter an amount, e.g. 100 or 250.50.'),
    ).toBeInTheDocument();
    expect(within(dialog).getByText('Enter who sent the money.')).toBeInTheDocument();
    expect(bodies).toHaveLength(0);
  });

  it('reuses the same key when sending again after a network error', async () => {
    const bodies = mockBank(savings(), [() => HttpResponse.error()]);
    renderAccounts();
    const { user, dialog } = await fillRemittance('50', 'Aiko');

    await user.click(within(dialog).getByRole('button', { name: /^receive$/i }));
    expect(await within(dialog).findByText(/won't be credited twice/i)).toBeInTheDocument();

    await user.click(within(dialog).getByRole('button', { name: /^receive$/i }));
    expect(await within(dialog).findByText('RMT-0123456789AB')).toBeInTheDocument();

    expect(bodies).toHaveLength(2);
    expect(bodies[1].idempotencyKey).toBe(bodies[0].idempotencyKey);
  });

  it("shows the backend's refusal in words for the admin", async () => {
    mockBank(savings(), [
      () =>
        HttpResponse.json(
          { status: 400, message: 'Selected account has been locked' },
          { status: 400 },
        ),
    ]);
    renderAccounts();
    const { user, dialog } = await fillRemittance('50', 'Aiko');

    await user.click(within(dialog).getByRole('button', { name: /^receive$/i }));

    expect(
      await within(dialog).findByText(/this account is locked, so it cannot receive money/i),
    ).toBeInTheDocument();
  });

  it('is not offered for a locked account', async () => {
    mockBank(savings({ accountStatus: 'LOCKED' }));
    renderAccounts();

    await userEvent.click(await screen.findByText('1000 000 007'));
    const panel = await screen.findByRole('dialog', { name: /account details/i });
    expect(within(panel).getByRole('button', { name: /receive remittance/i })).toBeDisabled();
  });
});
