import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { clearAccessToken } from 'api/tokenStore';
import { http, HttpResponse } from 'msw';
import paths from 'routes/path';
import { mockNoSession, renderApp as renderWithAuth } from 'test/renderApp';
import { API, server } from 'test/server';
import { beforeEach, describe, expect, it } from 'vitest';
import LoginForm from './LoginForm';

const devadmin = {
  id: '1',
  username: 'devadmin',
  fullName: 'Dev Admin',
  email: 'devadmin@banking.local',
  role: 'ADMIN',
};

const renderApp = (startAt: string) => renderWithAuth(startAt, { [paths.login]: <LoginForm /> });

const noSession = mockNoSession;

const submitLogin = async (password: string) => {
  const user = userEvent.setup();
  await user.type(await screen.findByLabelText(/username/i), 'devadmin');
  await user.type(screen.getByLabelText(/^password/i), password);
  await user.click(screen.getByRole('button', { name: /login/i }));
};

beforeEach(() => clearAccessToken());

describe('login flow', () => {
  it('sends a signed-out user to the login page', async () => {
    noSession();
    renderApp(paths.default);
    expect(await screen.findByLabelText(/username/i)).toBeInTheDocument();
    expect(screen.queryByText('Dashboard')).not.toBeInTheDocument();
  });

  it('restores the session from the refresh cookie after a reload', async () => {
    server.use(
      http.post(`${API}/api/auth/refresh`, () =>
        HttpResponse.json({ accessToken: 'a1', tokenType: 'Bearer', expiresIn: 900 }),
      ),
      http.get(`${API}/api/users/me`, () => HttpResponse.json(devadmin)),
    );
    renderApp(paths.default);
    expect(await screen.findByText('Dashboard')).toBeInTheDocument();
  });

  it('shows an error for a wrong password', async () => {
    noSession();
    server.use(
      http.post(`${API}/api/auth/login`, () =>
        HttpResponse.json({ status: 401, message: 'Bad credentials' }, { status: 401 }),
      ),
    );
    renderApp(paths.login);

    await submitLogin('wrong');

    expect(await screen.findByText('Invalid username or password.')).toBeInTheDocument();
  });

  it('shows how long to wait when the account is locked', async () => {
    noSession();
    server.use(
      http.post(`${API}/api/auth/login`, () =>
        HttpResponse.json(
          { status: 429, message: 'locked' },
          { status: 429, headers: { 'Retry-After': '900' } },
        ),
      ),
    );
    renderApp(paths.login);

    await submitLogin('wrong');

    expect(
      await screen.findByText('Too many failed attempts. Try again in 15 minutes.'),
    ).toBeInTheDocument();
  });

  it('signs in and continues to the page the user wanted', async () => {
    noSession();
    server.use(
      http.post(`${API}/api/auth/login`, () =>
        HttpResponse.json({ accessToken: 'a1', tokenType: 'Bearer', expiresIn: 900 }),
      ),
      http.get(`${API}/api/users/me`, ({ request }) =>
        request.headers.get('Authorization') === 'Bearer a1'
          ? HttpResponse.json(devadmin)
          : new HttpResponse(null, { status: 401 }),
      ),
    );
    renderApp(paths.default); // → redirected to login, remembering "/"

    await submitLogin('DevAdmin123!');

    expect(await screen.findByText('Dashboard')).toBeInTheDocument();
  });
});
