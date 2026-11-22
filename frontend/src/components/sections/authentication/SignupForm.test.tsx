import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { clearAccessToken } from 'api/tokenStore';
import { http, HttpResponse } from 'msw';
import paths from 'routes/path';
import { mockNoSession, renderApp } from 'test/renderApp';
import { API, server } from 'test/server';
import { beforeEach, describe, expect, it } from 'vitest';
import SignupForm from './SignupForm';

const renderSignup = () => renderApp(paths.signup, { [paths.signup]: <SignupForm /> });

/** Counts register calls and answers with `reply`; also serves /me for the signed-in user. */
const mockRegister = (reply: () => Response) => {
  const calls: unknown[] = [];
  server.use(
    http.post(`${API}/api/auth/register`, async ({ request }) => {
      calls.push(await request.json());
      return reply();
    }),
    http.get(`${API}/api/users/me`, ({ request }) =>
      request.headers.get('Authorization') === 'Bearer new-token'
        ? HttpResponse.json({
            id: '2',
            username: 'testuser',
            fullName: 'Test User',
            email: 'test@banking.local',
            role: 'CUSTOMER',
          })
        : new HttpResponse(null, { status: 401 }),
    ),
  );
  return calls;
};

const fillIn = async (values: Partial<Record<string, string>>) => {
  const user = userEvent.setup();
  const labels: Record<string, RegExp> = {
    fullName: /full name/i,
    username: /username/i,
    email: /email address/i,
    password: /^password/i,
    confirmPassword: /confirm password/i,
  };
  for (const [field, value] of Object.entries(values)) {
    if (value) await user.type(await screen.findByLabelText(labels[field]), value);
  }
  await user.click(screen.getByRole('button', { name: /sign up/i }));
};

const complete = {
  fullName: '  Test User ',
  username: ' testuser ',
  email: 'test@banking.local',
  password: 'Test1234!',
  confirmPassword: 'Test1234!',
};

beforeEach(() => {
  clearAccessToken();
  mockNoSession();
});

describe('sign-up form', () => {
  it('checks the form before sending anything', async () => {
    const calls = mockRegister(() => HttpResponse.json({}));
    renderSignup();

    await fillIn({ username: 'ab', email: 'nope', password: 'short', confirmPassword: 'other' });

    expect(await screen.findByText('Enter your full name.')).toBeInTheDocument();
    expect(screen.getByText('Username must be 3–30 characters.')).toBeInTheDocument();
    expect(screen.getByText('Enter a valid email address.')).toBeInTheDocument();
    expect(screen.getByText('Password must be at least 8 characters.')).toBeInTheDocument();
    expect(screen.getByText('Passwords do not match.')).toBeInTheDocument();
    expect(calls).toHaveLength(0);
  });

  it('creates the account, signs the user in and opens the dashboard', async () => {
    const calls = mockRegister(() =>
      HttpResponse.json(
        { accessToken: 'new-token', tokenType: 'Bearer', expiresIn: 900 },
        { status: 201 },
      ),
    );
    renderSignup();

    await fillIn(complete);

    expect(await screen.findByText('Dashboard')).toBeInTheDocument();
    // Trimmed values, no confirmPassword, and no role (the backend decides: CUSTOMER).
    expect(calls).toEqual([
      {
        fullName: 'Test User',
        username: 'testuser',
        email: 'test@banking.local',
        password: 'Test1234!',
      },
    ]);
  });

  it('shows "already exists" on the field it is about', async () => {
    mockRegister(() =>
      HttpResponse.json({ status: 409, message: 'Email already exists' }, { status: 409 }),
    );
    renderSignup();

    await fillIn(complete);

    expect(await screen.findByText('Email already exists.')).toBeInTheDocument();
    expect(screen.queryByText('Dashboard')).not.toBeInTheDocument();
  });

  it('shows other errors above the form', async () => {
    mockRegister(() => HttpResponse.json({ status: 500, message: 'boom' }, { status: 500 }));
    renderSignup();

    await fillIn(complete);

    expect(
      await screen.findByText('Something went wrong on the server. Please try again later.'),
    ).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /sign up/i })).toBeEnabled();
  });
});
