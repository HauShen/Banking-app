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
 * deletes and new admins all work on it. `overrides` can replace one endpoint's answer.
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

  it('offers no role change or delete on your own account', async () => {
    mockSession(devadmin);
    mockAdminApi([devadmin]);
    renderUsers();

    const details = await openUser('devadmin');
    expect(within(details).getByText(/this is your own account/i)).toBeInTheDocument();
    expect(within(details).queryByRole('button', { name: /delete user/i })).not.toBeInTheDocument();
  });

  it('creates an administrator, with the sign-up rules and errors on their field', async () => {
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
    expect(await within(dialog).findByText('Enter your full name.')).toBeInTheDocument();
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
});
