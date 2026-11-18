import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { clearAccessToken } from 'api/tokenStore';
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
