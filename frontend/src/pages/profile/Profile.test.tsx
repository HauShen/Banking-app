import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { clearAccessToken, getAccessToken } from 'api/tokenStore';
import { http, HttpResponse } from 'msw';
import { useAuth } from 'providers/AuthProvider';
import paths from 'routes/path';
import { mockSession, renderApp } from 'test/renderApp';
import { API, server } from 'test/server';
import { beforeEach, describe, expect, it } from 'vitest';
import ProfilePage from '.';

const customer = {
  id: 'u1',
  username: 'hau',
  fullName: 'Hau Shen',
  email: 'hau@banking.local',
  role: 'CUSTOMER',
  createdAt: '2026-11-01T12:00:00Z',
  updatedAt: '2026-11-01T12:00:00Z',
};

/** Stands in for the profile menu / account cards: shows the user's name from the auth context. */
const SignedInAs = () => <p>Signed in as {useAuth().user?.fullName}</p>;

const renderProfile = () =>
  renderApp(paths.profile, {}, <p>Dashboard</p>, {
    [paths.profile]: (
      <>
        <SignedInAs />
        <ProfilePage />
      </>
    ),
  });

/** PUT /api/users/me echoes the body (email lower-cased like the backend), or answers with `reply`. */
const mockUpdate = (reply?: () => Response) => {
  const bodies: unknown[] = [];
  server.use(
    http.put(`${API}/api/users/me`, async ({ request }) => {
      const body = (await request.json()) as { fullName: string; email: string };
      bodies.push(body);
      if (reply) return reply();
      return HttpResponse.json({
        ...customer,
        fullName: body.fullName,
        email: body.email.toLowerCase(),
      });
    }),
  );
  return bodies;
};

beforeEach(() => {
  clearAccessToken();
  mockSession(customer);
});

describe('profile page', () => {
  it('shows the account details and the current name and email', async () => {
    mockUpdate();
    renderProfile();

    expect(await screen.findByDisplayValue('Hau Shen')).toBeInTheDocument();
    expect(screen.getByDisplayValue('hau@banking.local')).toBeInTheDocument();
    expect(screen.getByText('hau')).toBeInTheDocument();
    expect(screen.getByText('Customer')).toBeInTheDocument();
    expect(screen.getByText('1 Nov 2026')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /save changes/i })).toBeDisabled();
  });

  it('saves the changes and updates the name everywhere at once', async () => {
    const bodies = mockUpdate();
    renderProfile();
    const user = userEvent.setup();

    const name = await screen.findByLabelText(/full name/i);
    await user.clear(name);
    await user.type(name, '  Hau Shen Tan ');
    const email = screen.getByLabelText(/email address/i);
    await user.clear(email);
    await user.type(email, 'Hau.Tan@Banking.local');
    await user.click(screen.getByRole('button', { name: /save changes/i }));

    expect(await screen.findByText('Your profile has been saved.')).toBeInTheDocument();
    expect(bodies).toEqual([{ fullName: 'Hau Shen Tan', email: 'Hau.Tan@Banking.local' }]);
    expect(screen.getByText('Signed in as Hau Shen Tan')).toBeInTheDocument();
    expect(screen.getByDisplayValue('hau.tan@banking.local')).toBeInTheDocument();
  });

  it('checks the form before sending anything', async () => {
    const bodies = mockUpdate();
    renderProfile();
    const user = userEvent.setup();

    await user.clear(await screen.findByLabelText(/full name/i));
    const email = screen.getByLabelText(/email address/i);
    await user.clear(email);
    await user.type(email, 'not-an-email');
    await user.click(screen.getByRole('button', { name: /save changes/i }));

    expect(await screen.findByText('Enter your full name.')).toBeInTheDocument();
    expect(screen.getByText('Enter a valid email address.')).toBeInTheDocument();
    expect(bodies).toHaveLength(0);
  });

  it('shows "already used" on the email field', async () => {
    mockUpdate(() =>
      HttpResponse.json({ status: 409, message: 'Email already exists' }, { status: 409 }),
    );
    renderProfile();
    const user = userEvent.setup();

    const email = await screen.findByLabelText(/email address/i);
    await user.clear(email);
    await user.type(email, 'taken@banking.local');
    await user.click(screen.getByRole('button', { name: /save changes/i }));

    expect(
      await screen.findByText('This email is already used by another account.'),
    ).toBeInTheDocument();
    expect(screen.getByText('Signed in as Hau Shen')).toBeInTheDocument();
  });
});

/** POST /api/auth/change-password: a new session token, or `reply`. Records the bodies and auth headers. */
const mockChangePassword = (reply?: () => Response) => {
  const calls: { body: unknown; auth: string | null }[] = [];
  server.use(
    http.post(`${API}/api/auth/change-password`, async ({ request }) => {
      calls.push({ body: await request.json(), auth: request.headers.get('Authorization') });
      if (reply) return reply();
      return HttpResponse.json({
        accessToken: 'new-session-token',
        tokenType: 'Bearer',
        expiresIn: 900,
      });
    }),
  );
  return calls;
};

const fillPasswords = async (current: string, next: string, confirm = next) => {
  const user = userEvent.setup();
  await user.type(await screen.findByLabelText(/current password/i), current);
  await user.type(screen.getByLabelText(/^new password/i), next);
  await user.type(screen.getByLabelText(/confirm new password/i), confirm);
  await user.click(screen.getByRole('button', { name: /change password/i }));
};

describe('changing the password', () => {
  it('sends the current and new password, keeps the new session token and clears the form', async () => {
    const calls = mockChangePassword();
    renderProfile();

    await fillPasswords('Passw0rd123', 'N3wPassword!');

    expect(
      await screen.findByText(
        'Your password has been changed. You have been signed out on your other devices.',
      ),
    ).toBeInTheDocument();
    expect(calls).toEqual([
      {
        body: { currentPassword: 'Passw0rd123', newPassword: 'N3wPassword!' },
        auth: 'Bearer session-token',
      },
    ]);
    expect(getAccessToken()).toBe('new-session-token');
    expect(screen.getByLabelText(/current password/i)).toHaveValue('');
    expect(screen.getByLabelText(/confirm new password/i)).toHaveValue('');
  });

  it('checks the form before sending anything', async () => {
    const calls = mockChangePassword();
    renderProfile();
    const user = userEvent.setup();

    await user.click(await screen.findByRole('button', { name: /change password/i }));
    expect(await screen.findByText('Enter your current password.')).toBeInTheDocument();
    expect(screen.getByText('Password must be 8–100 characters.')).toBeInTheDocument();

    await user.type(screen.getByLabelText(/current password/i), 'Passw0rd123');
    await user.type(screen.getByLabelText(/^new password/i), 'Passw0rd123');
    await user.type(screen.getByLabelText(/confirm new password/i), 'Passw0rd12');
    await user.click(screen.getByRole('button', { name: /change password/i }));

    expect(
      await screen.findByText('New password must differ from the current one.'),
    ).toBeInTheDocument();
    expect(screen.getByText('Passwords do not match.')).toBeInTheDocument();
    expect(calls).toHaveLength(0);
  });

  it('shows a wrong current password on its field, and keeps the session token', async () => {
    mockChangePassword(() =>
      HttpResponse.json(
        {
          status: 400,
          message: 'Current password is incorrect',
          fieldErrors: { currentPassword: 'Current password is incorrect' },
        },
        { status: 400 },
      ),
    );
    renderProfile();

    await fillPasswords('Wrong-Passw0rd', 'N3wPassword!');

    expect(await screen.findByText('Current password is incorrect')).toBeInTheDocument();
    expect(getAccessToken()).toBe('session-token');
    expect(screen.getByLabelText(/^new password/i)).toHaveValue('N3wPassword!');
  });

  it('explains how long to wait after too many wrong guesses', async () => {
    mockChangePassword(() =>
      HttpResponse.json(
        { status: 429, message: 'Too many failed login attempts. Try again later.' },
        { status: 429, headers: { 'Retry-After': '120' } },
      ),
    );
    renderProfile();

    await fillPasswords('Wrong-Passw0rd', 'N3wPassword!');

    expect(
      await screen.findByText('Too many requests. Try again in 2 minutes.'),
    ).toBeInTheDocument();
  });
});
