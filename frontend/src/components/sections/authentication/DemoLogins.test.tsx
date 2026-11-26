import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { clearAccessToken } from 'api/tokenStore';
import { http, HttpResponse } from 'msw';
import paths from 'routes/path';
import { mockNoSession, renderApp } from 'test/renderApp';
import { API, server } from 'test/server';
import { beforeEach, describe, expect, it } from 'vitest';
import DemoLogins from './DemoLogins';

const demo = {
  customer: { username: 'weiming', password: 'Demo1234!' },
  admin: { username: 'demoadmin', password: 'DemoAdmin1!' },
  otherCustomers: ['mei.ling', 'ahmad.f', 'priya.n', 'jason.k'],
  resetTime: '04:00',
  timeZone: 'Asia/Kuala_Lumpur',
};

const weiming = {
  id: 'u1',
  username: 'weiming',
  fullName: 'Lim Wei Ming',
  email: 'weiming@banking.demo',
  role: 'CUSTOMER',
};

const renderLoginPage = () => renderApp(paths.login, { [paths.login]: <DemoLogins /> });

beforeEach(() => clearAccessToken());

describe('demo logins on the login page', () => {
  it('shows nothing outside the live demo', async () => {
    mockNoSession();
    let asked = false;
    server.use(
      http.get(`${API}/api/demo/logins`, () => {
        asked = true;
        return new HttpResponse(null, { status: 404 });
      }),
    );

    renderLoginPage();

    await waitFor(() => expect(asked).toBe(true));
    expect(screen.queryByText(/try the demo/i)).not.toBeInTheDocument();
  });

  it('shows the demo logins and when the demo resets', async () => {
    mockNoSession();
    server.use(http.get(`${API}/api/demo/logins`, () => HttpResponse.json(demo)));

    renderLoginPage();

    expect(await screen.findByText(/try the demo/i)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /log in as weiming \(customer\)/i })).toBeEnabled();
    expect(screen.getByRole('button', { name: /log in as demoadmin \(admin\)/i })).toBeEnabled();
    expect(screen.getByText('Demo1234!')).toBeInTheDocument();
    expect(screen.getByText('DemoAdmin1!')).toBeInTheDocument();
    expect(screen.getByText(/mei\.ling, ahmad\.f, priya\.n, jason\.k/)).toBeInTheDocument();
    expect(
      screen.getByText(/resets every night at 04:00 \(Asia\/Kuala Lumpur\)/),
    ).toBeInTheDocument();
  });

  it('signs in with one click', async () => {
    mockNoSession();
    let sent: unknown;
    server.use(
      http.get(`${API}/api/demo/logins`, () => HttpResponse.json(demo)),
      http.post(`${API}/api/auth/login`, async ({ request }) => {
        sent = await request.json();
        return HttpResponse.json({
          accessToken: 'demo-token',
          tokenType: 'Bearer',
          expiresIn: 900,
        });
      }),
      http.get(`${API}/api/users/me`, ({ request }) =>
        request.headers.get('Authorization') === 'Bearer demo-token'
          ? HttpResponse.json(weiming)
          : new HttpResponse(null, { status: 401 }),
      ),
    );
    renderLoginPage();

    await userEvent
      .setup()
      .click(await screen.findByRole('button', { name: /log in as weiming/i }));

    expect(await screen.findByText('Dashboard')).toBeInTheDocument();
    expect(sent).toEqual({ username: 'weiming', password: 'Demo1234!' });
  });

  it('explains a refused demo login', async () => {
    mockNoSession();
    server.use(
      http.get(`${API}/api/demo/logins`, () => HttpResponse.json(demo)),
      http.post(`${API}/api/auth/login`, () =>
        HttpResponse.json({ status: 401, message: 'Bad credentials' }, { status: 401 }),
      ),
    );
    renderLoginPage();

    await userEvent
      .setup()
      .click(await screen.findByRole('button', { name: /log in as demoadmin/i }));

    expect(await screen.findByText(/demo login was refused/i)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /log in as demoadmin/i })).toBeEnabled();
  });
});
