import { screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { clearAccessToken } from 'api/tokenStore';
import type { Account, UserProfile } from 'api/types';
import { http, HttpResponse } from 'msw';
import paths from 'routes/path';
import RequireRole from 'routes/RequireRole';
import { mockSession, renderApp } from 'test/renderApp';
import { API, server } from 'test/server';
import { beforeEach, describe, expect, it } from 'vitest';
import AdminAccountsPage from '.';

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

const account = (accountId: number, userId: string, overrides: Partial<Account> = {}): Account => ({
  accountId,
  userId,
  accountNumber: String(1000000000 + accountId),
  accountType: 'SAVINGS',
  accountStatus: 'ACTIVE',
  accountCurrency: 'MYR',
  balance: 100,
  createdAt: `2026-11-0${accountId}T00:00:00Z`,
  ...overrides,
});

/** In-memory accounts behind the admin account API; records requests. `del` can replace DELETE's answer. */
const mockBank = (accounts: Account[], options: { del?: () => Response } = {}) => {
  let store = [...accounts];
  const requests: string[] = [];
  const bodies: unknown[] = [];
  server.use(
    http.get(`${API}/api/admin/users`, () =>
      HttpResponse.json({ content: [devadmin, hau], page: 0, size: 100, totalElements: 2 }),
    ),
    http.get(`${API}/api/admin/users/:id`, ({ params }) =>
      HttpResponse.json([devadmin, hau].find((u) => u.id === params.id)),
    ),
    http.get(`${API}/api/accounts/get_all`, ({ request }) => {
      const status = new URL(request.url).searchParams.get('status');
      requests.push(`GET all${status ? ` ${status}` : ''}`);
      return HttpResponse.json(status ? store.filter((a) => a.accountStatus === status) : store);
    }),
    http.get(`${API}/api/accounts/get_all/:userId`, ({ params }) => {
      requests.push(`GET user ${params.userId}`);
      return HttpResponse.json(store.filter((a) => a.userId === params.userId));
    }),
    http.post(`${API}/api/accounts/create`, async ({ request }) => {
      const body = (await request.json()) as {
        userId: string;
        accountType: Account['accountType'];
      };
      bodies.push(body);
      const created = account(store.length + 1, body.userId, {
        accountType: body.accountType,
        balance: 20,
      });
      store = [...store, created];
      return HttpResponse.json(created, { status: 201 });
    }),
    http.patch(`${API}/api/accounts/:id/status`, ({ params, request }) => {
      const status = new URL(request.url).searchParams.get('status') as Account['accountStatus'];
      requests.push(`PATCH ${params.id} ${status}`);
      store = store.map((a) =>
        a.accountId === Number(params.id) ? { ...a, accountStatus: status } : a,
      );
      return HttpResponse.json(store.find((a) => a.accountId === Number(params.id)));
    }),
    http.delete(`${API}/api/accounts/:id`, ({ params }) => {
      requests.push(`DELETE ${params.id}`);
      if (options.del) return options.del();
      store = store.filter((a) => a.accountId !== Number(params.id));
      return new HttpResponse(null, { status: 204 });
    }),
  );
  return { requests, bodies };
};

const renderAccounts = (url: string = paths.adminAccounts) =>
  renderApp(url, {}, <p>Dashboard</p>, {
    [paths.adminAccounts]: (
      <RequireRole role="ADMIN">
        <AdminAccountsPage />
      </RequireRole>
    ),
  });

const plain = (text: string | null | undefined) => (text ?? '').replace(/\s/g, ' ');

const openAccountRow = async (accountNumber: string) => {
  await userEvent.click(await screen.findByText(accountNumber));
  return screen.findByRole('dialog', { name: /account details/i });
};

beforeEach(() => {
  clearAccessToken();
  mockSession(devadmin);
});

describe('admin accounts page', () => {
  it('lists every account with its owner, and filters by status on the server', async () => {
    const bank = mockBank([
      account(1, 'c1'),
      account(2, 'c1', { accountStatus: 'LOCKED', balance: 5 }),
    ]);
    renderAccounts();

    const row = (await screen.findByText('1000 000 001')).closest('[role="row"]') as HTMLElement;
    expect(plain(row.textContent)).toContain('hau');
    expect(plain(row.textContent)).toContain('RM 100.00');
    const lockedRow = screen.getByText('1000 000 002').closest('[role="row"]') as HTMLElement;
    expect(within(lockedRow).getByText('Locked')).toBeInTheDocument();
    // The right-aligned balances come last, so they don't run into the next column.
    expect(screen.getAllByRole('columnheader').slice(-1)[0]).toHaveTextContent('Balance');

    await userEvent.click(screen.getByRole('button', { name: /^locked$/i }));

    await screen.findByText('1000 000 002');
    expect(screen.queryByText('1000 000 001')).not.toBeInTheDocument();
    expect(bank.requests).toEqual(['GET all', 'GET all LOCKED']);
  });

  it("shows one user's accounts and opens a new one for them", async () => {
    const bank = mockBank([account(1, 'c1'), account(2, 'a1')]);
    renderAccounts(`${paths.adminAccounts}?user=c1`);

    expect(await screen.findByText('Accounts of hau full (hau)')).toBeInTheDocument();
    expect(await screen.findByText('1000 000 001')).toBeInTheDocument();
    expect(screen.queryByText('1000 000 002')).not.toBeInTheDocument();

    const user = userEvent.setup();
    await user.click(screen.getByRole('button', { name: /^open account$/i }));
    const dialog = await screen.findByRole('dialog', { name: /open account/i });
    await user.click(within(dialog).getByRole('combobox', { name: /account type/i }));
    await user.click(await screen.findByRole('option', { name: 'Current' }));
    await user.click(within(dialog).getByRole('button', { name: /open account/i }));

    expect(
      await screen.findByText('Current account 1000 000 003 opened with RM 20.00.', {
        normalizer: plain,
      }),
    ).toBeInTheDocument();
    expect(bank.bodies).toEqual([{ userId: 'c1', accountType: 'CURRENT' }]);
    expect(await screen.findByText('1000 000 003')).toBeInTheDocument();
    expect(bank.requests).toEqual(['GET user c1', 'GET user c1']);
  });

  it('locks an account after confirming', async () => {
    const bank = mockBank([account(7, 'c1')]);
    renderAccounts();

    const details = await openAccountRow('1000 000 007');
    await userEvent.click(within(details).getByRole('button', { name: /lock account/i }));
    const confirm = await screen.findByRole('dialog', { name: /lock account\?/i });
    await userEvent.click(within(confirm).getByRole('button', { name: /^lock$/i }));

    expect(await screen.findByText('Account 1000 000 007 is now locked.')).toBeInTheDocument();
    expect(bank.requests).toContain('PATCH 7 LOCKED');
    expect(
      await within(details).findByRole('button', { name: /unlock account/i }),
    ).toBeInTheDocument();
  });

  it("explains why an account with transactions can't be deleted", async () => {
    mockBank([account(7, 'c1')], {
      del: () =>
        HttpResponse.json(
          { status: 409, message: 'Account 7 has transactions and cannot be deleted' },
          { status: 409 },
        ),
    });
    renderAccounts();

    const details = await openAccountRow('1000 000 007');
    await userEvent.click(within(details).getByRole('button', { name: /delete account/i }));
    const confirm = await screen.findByRole('dialog', { name: /delete account\?/i });
    await userEvent.click(within(confirm).getByRole('button', { name: /^delete$/i }));

    expect(
      await within(confirm).findByText('Account 7 has transactions and cannot be deleted'),
    ).toBeInTheDocument();
  });

  it('deletes an account without transactions', async () => {
    const bank = mockBank([account(7, 'c1'), account(8, 'c1')]);
    renderAccounts();

    const details = await openAccountRow('1000 000 007');
    await userEvent.click(within(details).getByRole('button', { name: /delete account/i }));
    const confirm = await screen.findByRole('dialog', { name: /delete account\?/i });
    await userEvent.click(within(confirm).getByRole('button', { name: /^delete$/i }));

    expect(await screen.findByText('Account 1000 000 007 deleted.')).toBeInTheDocument();
    expect(bank.requests).toContain('DELETE 7');
    expect(screen.queryByText('1000 000 007')).not.toBeInTheDocument();
  });
});
