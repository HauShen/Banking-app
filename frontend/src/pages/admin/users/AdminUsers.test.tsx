import { screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { clearAccessToken } from 'api/tokenStore';
import type { UserProfile } from 'api/types';
import { http, HttpResponse } from 'msw';
import paths from 'routes/path';
import RequireRole from 'routes/RequireRole';
import { mockSession, renderApp } from 'test/renderApp';
import { API, server } from 'test/server';
import { beforeEach, describe, expect, it } from 'vitest';
import AdminAccountsPage from 'pages/admin/accounts';
import { useAuth } from 'providers/AuthProvider';
import AdminUsersPage from '.';

const person = (id: string, username: string, role: UserProfile['role']): UserProfile => ({
  id,
  username,
  fullName: `${username[0].toUpperCase()}${username.slice(1)} User`,
  email: `${username}@banking.local`,
  role,
  createdAt: '2026-11-01T12:00:00Z',
  updatedAt: '2026-11-01T12:00:00Z',
});

const devadmin = person('a1', 'devadmin', 'ADMIN');

/**
 * In-memory user store behind the admin API: lists (with page/size/sort recorded), role changes,
 * deletes, new admins and new customers all work on it. `overrides` can replace one endpoint's answer.
 */
const mockAdminApi = (
  users: UserProfile[],
  overrides: { patch?: () => Response; del?: () => Response; post?: () => Response } = {},
) => {
  let store = [...users];
  const requests: string[] = [];
  const bodies: unknown[] = [];
  server.use(
    http.get(`${API}/api/admin/users`, ({ request }) => {
      const url = new URL(request.url);
      requests.push(`GET ${url.search}`);
      const page = Number(url.searchParams.get('page'));
      const size = Number(url.searchParams.get('size'));
      return HttpResponse.json({
        content: store.slice(page * size, page * size + size),
        page,
        size,
        totalElements: store.length,
      });
    }),
    http.patch(`${API}/api/admin/users/:id/role`, async ({ params, request }) => {
      const body = (await request.json()) as { role: UserProfile['role'] };
      bodies.push(body);
      requests.push(`PATCH ${params.id}`);
      if (overrides.patch) return overrides.patch();
      store = store.map((u) => (u.id === params.id ? { ...u, role: body.role } : u));
      return HttpResponse.json(store.find((u) => u.id === params.id));
    }),
    http.delete(`${API}/api/admin/users/:id`, ({ params }) => {
      requests.push(`DELETE ${params.id}`);
      if (overrides.del) return overrides.del();
      store = store.filter((u) => u.id !== params.id);
      return new HttpResponse(null, { status: 204 });
    }),
    http.post(`${API}/api/admin/users`, async ({ request }) => {
      const body = (await request.json()) as Omit<UserProfile, 'id' | 'role'>;
      bodies.push(body);
      requests.push('POST');
      if (overrides.post) return overrides.post();
      const created = { ...person(`n${store.length}`, body.username, 'ADMIN'), ...body };
      store = [...store, created];
      return HttpResponse.json(created, { status: 201 });
    }),
    http.post(`${API}/api/admin/users/customers`, async ({ request }) => {
      const body = (await request.json()) as Omit<UserProfile, 'id' | 'role'>;
      bodies.push(body);
      requests.push('POST customers');
      const created = { ...person(`n${store.length}`, body.username, 'CUSTOMER'), ...body };
      store = [...store, created];
      return HttpResponse.json(created, { status: 201 });
    }),
  );
  return { requests, bodies };
};

const renderUsers = () =>
  renderApp(paths.adminUsers, {}, <p>Dashboard</p>, {
    [paths.adminUsers]: (
      <RequireRole role="ADMIN">
        <AdminUsersPage />
      </RequireRole>
    ),
  });

const openUser = async (username: string) => {
  await userEvent.click(await screen.findByText(username));
  return screen.findByRole('dialog', { name: /user details/i });
};

beforeEach(() => clearAccessToken());

describe('admin users page', () => {
  it('lists users sorted by username, marks roles and the signed-in admin', async () => {
    mockSession(devadmin);
    const api = mockAdminApi([devadmin, person('c1', 'hau', 'CUSTOMER')]);
    renderUsers();

    expect(await screen.findByText('hau')).toBeInTheDocument();
    expect(screen.getByText('(you)')).toBeInTheDocument();
    expect(screen.getByText('2 users')).toBeInTheDocument();
    expect(screen.getAllByText('Administrator').length).toBeGreaterThan(0);
    expect(screen.getByText('Customer')).toBeInTheDocument();
    expect(api.requests[0]).toBe('GET ?page=0&size=20&sortBy=USERNAME&direction=ASC');
  });

  it('sorts on the server when a column header is clicked', async () => {
    mockSession(devadmin);
    const api = mockAdminApi([devadmin]);
    renderUsers();
    await screen.findByText('(you)');

    await userEvent.click(screen.getByRole('columnheader', { name: /email/i }));

    await screen.findByText('(you)');
    expect(api.requests).toContain('GET ?page=0&size=20&sortBy=EMAIL&direction=ASC');
  });

  it('makes a customer an administrator after confirming', async () => {
    mockSession(devadmin);
    const api = mockAdminApi([devadmin, person('c1', 'hau', 'CUSTOMER')]);
    renderUsers();

    const details = await openUser('hau');
    await userEvent.click(within(details).getByRole('button', { name: /make administrator/i }));
    const confirm = await screen.findByRole('dialog', { name: /make administrator\?/i });
    await userEvent.click(within(confirm).getByRole('button', { name: /make administrator/i }));

    expect(await screen.findByText('hau is now an administrator.')).toBeInTheDocument();
    expect(api.bodies).toEqual([{ role: 'ADMIN' }]);
    // findBy: while the confirm dialog's closing animation runs, MUI still marks the panel aria-hidden.
    expect(
      await within(details).findByRole('button', { name: /remove admin rights/i }),
    ).toBeInTheDocument();
  });

  it("shows the backend's reason when a role change is refused", async () => {
    mockSession(devadmin);
    mockAdminApi([devadmin, person('a2', 'other', 'ADMIN')], {
      patch: () =>
        HttpResponse.json(
          { status: 409, message: 'Cannot demote the last remaining admin' },
          { status: 409 },
        ),
    });
    renderUsers();

    const details = await openUser('other');
    await userEvent.click(within(details).getByRole('button', { name: /remove admin rights/i }));
    const confirm = await screen.findByRole('dialog', { name: /remove admin rights\?/i });
    await userEvent.click(within(confirm).getByRole('button', { name: /remove admin rights/i }));

    expect(
      await within(confirm).findByText('Cannot demote the last remaining admin'),
    ).toBeInTheDocument();
  });

  it('deletes a user after confirming and refreshes the list', async () => {
    mockSession(devadmin);
    const api = mockAdminApi([devadmin, person('c1', 'hau', 'CUSTOMER')]);
    renderUsers();

    const details = await openUser('hau');
    await userEvent.click(within(details).getByRole('button', { name: /delete user/i }));
    const confirm = await screen.findByRole('dialog', { name: /delete user\?/i });
    await userEvent.click(within(confirm).getByRole('button', { name: /^delete$/i }));

    expect(await screen.findByText('1 users')).toBeInTheDocument();
    expect(screen.queryByText('hau')).not.toBeInTheDocument();
    expect(api.requests).toContain('DELETE c1');
  });

  it("shows the demo guard's reason when a shared demo user can't be deleted", async () => {
    const reason =
      "This is a shared demo user, so it can't be changed. Log in as demoadmin and create your own customer to try this.";
    mockSession(devadmin);
    const api = mockAdminApi([devadmin, person('c1', 'weiming', 'CUSTOMER')], {
      del: () => HttpResponse.json({ status: 403, message: reason }, { status: 403 }),
    });
    renderUsers();

    const details = await openUser('weiming');
    await userEvent.click(within(details).getByRole('button', { name: /delete user/i }));
    const confirm = await screen.findByRole('dialog', { name: /delete user\?/i });
    await userEvent.click(within(confirm).getByRole('button', { name: /^delete$/i }));

    expect(await within(confirm).findByText(reason)).toBeInTheDocument();
    expect(api.requests).toContain('DELETE c1');
    expect(screen.getByText('2 users')).toBeInTheDocument();
  });

  it('offers no role change or delete on your own account', async () => {
    mockSession(devadmin);
    mockAdminApi([devadmin]);
    renderUsers();

    const details = await openUser('devadmin');
    expect(within(details).getByText(/this is your own account/i)).toBeInTheDocument();
    expect(within(details).queryByRole('button', { name: /delete user/i })).not.toBeInTheDocument();
  });

  it('creates a customer through the admin endpoint and lists them', async () => {
    mockSession(devadmin);
    const api = mockAdminApi([devadmin]);
    renderUsers();
    const user = userEvent.setup();

    await user.click(await screen.findByRole('button', { name: /new customer/i }));
    const dialog = await screen.findByRole('dialog', { name: /new customer/i });
    expect(within(dialog).getByText(/give it to the customer/i)).toBeInTheDocument();

    await user.type(within(dialog).getByLabelText(/full name/i), 'Siti Aminah');
    await user.type(within(dialog).getByLabelText(/username/i), 'siti');
    await user.type(within(dialog).getByLabelText(/email address/i), 'siti@banking.local');
    await user.type(within(dialog).getByLabelText(/^password/i), 'Secret123!');
    await user.type(within(dialog).getByLabelText(/confirm password/i), 'Secret123!');
    await user.click(within(dialog).getByRole('button', { name: /create customer/i }));

    expect(await screen.findByText(/customer siti created/i)).toBeInTheDocument();
    expect(await screen.findByText('siti')).toBeInTheDocument();
    expect(screen.getByText('2 users')).toBeInTheDocument();
    expect(screen.getByText('Customer')).toBeInTheDocument();
    expect(api.requests).toContain('POST customers');
    expect(api.requests).not.toContain('POST');
    expect(api.bodies).toEqual([
      {
        username: 'siti',
        fullName: 'Siti Aminah',
        email: 'siti@banking.local',
        password: 'Secret123!',
      },
    ]);
  });

  it('creates an administrator, with the same field rules and errors on their field', async () => {
    mockSession(devadmin);
    const api = mockAdminApi([devadmin], {
      post: () =>
        HttpResponse.json({ status: 409, message: 'Username already exists' }, { status: 409 }),
    });
    renderUsers();
    const user = userEvent.setup();

    await user.click(await screen.findByRole('button', { name: /new administrator/i }));
    const dialog = await screen.findByRole('dialog', { name: /new administrator/i });
    await user.click(within(dialog).getByRole('button', { name: /create administrator/i }));
    expect(await within(dialog).findByText('Enter the full name.')).toBeInTheDocument();
    expect(api.requests).not.toContain('POST');

    await user.type(within(dialog).getByLabelText(/full name/i), 'Second Admin');
    await user.type(within(dialog).getByLabelText(/username/i), 'devadmin');
    await user.type(within(dialog).getByLabelText(/email address/i), 'second@banking.local');
    await user.type(within(dialog).getByLabelText(/^password/i), 'Secret123!');
    await user.type(within(dialog).getByLabelText(/confirm password/i), 'Secret123!');
    await user.click(within(dialog).getByRole('button', { name: /create administrator/i }));

    expect(await within(dialog).findByText('Username already exists.')).toBeInTheDocument();
    expect(api.bodies).toEqual([
      {
        username: 'devadmin',
        fullName: 'Second Admin',
        email: 'second@banking.local',
        password: 'Secret123!',
      },
    ]);
  });

  it('sends customers back to the dashboard', async () => {
    mockSession(person('c1', 'hau', 'CUSTOMER'));
    const api = mockAdminApi([]);
    renderUsers();

    expect(await screen.findByText('Dashboard')).toBeInTheDocument();
    expect(api.requests).toHaveLength(0);
  });

  it("opens the user's accounts from the user panel", async () => {
    mockSession(devadmin);
    mockAdminApi([devadmin, person('c1', 'hau', 'CUSTOMER')]);
    server.use(
      http.get(`${API}/api/admin/users/:id`, () =>
        HttpResponse.json(person('c1', 'hau', 'CUSTOMER')),
      ),
      http.get(`${API}/api/accounts/get_all/:userId`, () => HttpResponse.json([])),
    );
    renderApp(paths.adminUsers, {}, <p>Dashboard</p>, {
      [paths.adminUsers]: <AdminUsersPage />,
      [paths.adminAccounts]: <AdminAccountsPage />,
    });

    const details = await openUser('hau');
    await userEvent.click(within(details).getByRole('link', { name: /accounts of this user/i }));

    expect(await screen.findByText('Accounts of Hau User (hau)')).toBeInTheDocument();
  });

  describe('editing name and email', () => {
    /** PUT /api/admin/users/:id and PUT /api/users/me, recorded; `adminReply` can replace the admin answer. */
    const mockEdits = (store: UserProfile[], adminReply?: () => Response) => {
      const edits: string[] = [];
      const bodies: unknown[] = [];
      const save = (id: string, body: { fullName: string; email: string }) => {
        const index = store.findIndex((u) => u.id === id);
        store[index] = {
          ...store[index],
          ...body,
          email: body.email.toLowerCase(),
          updatedAt: '2026-11-19T15:00:00Z',
        };
        return store[index];
      };
      server.use(
        http.put(`${API}/api/admin/users/:id`, async ({ params, request }) => {
          const body = (await request.json()) as { fullName: string; email: string };
          edits.push(`admin ${params.id}`);
          bodies.push(body);
          if (adminReply) return adminReply();
          return HttpResponse.json(save(params.id as string, body));
        }),
        http.put(`${API}/api/users/me`, async ({ request }) => {
          const body = (await request.json()) as { fullName: string; email: string };
          edits.push('me');
          bodies.push(body);
          return HttpResponse.json(save(devadmin.id, body));
        }),
      );
      return { edits, bodies };
    };

    const editDetails = async (details: HTMLElement, fullName: string, email?: string) => {
      const user = userEvent.setup();
      await user.click(within(details).getByRole('button', { name: /edit details/i }));
      const dialog = await screen.findByRole('dialog', { name: /^edit /i });
      const name = within(dialog).getByLabelText(/full name/i);
      await user.clear(name);
      await user.type(name, fullName);
      if (email !== undefined) {
        const field = within(dialog).getByLabelText(/email address/i);
        await user.clear(field);
        await user.type(field, email);
      }
      await user.click(within(dialog).getByRole('button', { name: /save changes/i }));
      return dialog;
    };

    it("saves another user's changes through the admin endpoint", async () => {
      mockSession(devadmin);
      const store = [devadmin, person('c1', 'hau', 'CUSTOMER')];
      mockAdminApi(store);
      const api = mockEdits(store);
      renderUsers();

      const details = await openUser('hau');
      await editDetails(details, '  Hau Shen Tan ', 'Hau.Tan@Banking.local');

      expect(await screen.findByText('Saved the changes to hau.')).toBeInTheDocument();
      expect(api.edits).toEqual(['admin c1']);
      expect(api.bodies).toEqual([{ fullName: 'Hau Shen Tan', email: 'Hau.Tan@Banking.local' }]);
      expect(await within(details).findByText('hau.tan@banking.local')).toBeInTheDocument();
    });

    it('shows "already used" on the email field', async () => {
      mockSession(devadmin);
      const store = [devadmin, person('c1', 'hau', 'CUSTOMER')];
      mockAdminApi(store);
      mockEdits(store, () =>
        HttpResponse.json({ status: 409, message: 'Email already exists' }, { status: 409 }),
      );
      renderUsers();

      const details = await openUser('hau');
      const dialog = await editDetails(details, 'Hau', 'taken@banking.local');

      expect(
        await within(dialog).findByText('This email is already used by another account.'),
      ).toBeInTheDocument();
    });

    it('saves your own entry through /api/users/me, so the signed-in name updates too', async () => {
      mockSession(devadmin);
      const store = [devadmin];
      mockAdminApi(store);
      const api = mockEdits(store);
      const SignedInAs = () => <p>Signed in as {useAuth().user?.fullName}</p>;
      renderApp(paths.adminUsers, {}, <p>Dashboard</p>, {
        [paths.adminUsers]: (
          <>
            <SignedInAs />
            <AdminUsersPage />
          </>
        ),
      });

      const details = await openUser('devadmin');
      await editDetails(details, 'Dev Admin Renamed');

      expect(await screen.findByText('Signed in as Dev Admin Renamed')).toBeInTheDocument();
      expect(api.edits).toEqual(['me']);
    });
  });
});
