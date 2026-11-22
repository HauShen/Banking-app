import { delay, http, HttpResponse } from 'msw';
import { API, server } from 'test/server';
import { beforeEach, describe, expect, it } from 'vitest';
import { login, logout, restoreSession } from './auth';
import { setAuthFailureListener } from './client';
import { clearAccessToken, getAccessToken, setAccessToken } from './tokenStore';

const user = { id: '1', username: 'hau', fullName: 'Hau', email: 'h@x', role: 'CUSTOMER' };

beforeEach(() => {
  clearAccessToken();
  setAuthFailureListener(null);
});

describe('login', () => {
  it('keeps the access token in memory and sends no Authorization header', async () => {
    let authHeader: string | null = 'not checked';
    server.use(
      http.post(`${API}/api/auth/login`, ({ request }) => {
        authHeader = request.headers.get('Authorization');
        return HttpResponse.json({ accessToken: 'a1', tokenType: 'Bearer', expiresIn: 900 });
      }),
    );

    await login('hau', 'secret');

    expect(getAccessToken()).toBe('a1');
    expect(authHeader).toBeNull();
    expect(window.localStorage.length).toBe(0);
  });
});

describe('restoreSession', () => {
  it('resolves null when there is no valid refresh cookie', async () => {
    server.use(
      http.post(`${API}/api/auth/refresh`, () =>
        HttpResponse.json({ status: 401, message: 'Invalid refresh token' }, { status: 401 }),
      ),
    );

    await expect(restoreSession()).resolves.toBeNull();
  });

  it('loads the user, with one refresh even when called twice at once (React StrictMode)', async () => {
    let refreshCalls = 0;
    server.use(
      http.post(`${API}/api/auth/refresh`, async () => {
        refreshCalls++;
        await delay(20);
        return HttpResponse.json({ accessToken: 'a2', tokenType: 'Bearer', expiresIn: 900 });
      }),
      http.get(`${API}/api/users/me`, () => HttpResponse.json(user)),
    );

    const [first, second] = await Promise.all([restoreSession(), restoreSession()]);

    expect(first).toEqual(user);
    expect(second).toEqual(user);
    expect(refreshCalls).toBe(1);
  });
});

describe('logout', () => {
  it('forgets the access token even when the server call fails', async () => {
    setAccessToken('a1');
    server.use(
      http.post(`${API}/api/auth/logout`, () =>
        HttpResponse.json({ status: 500, message: 'boom' }, { status: 500 }),
      ),
    );

    await expect(logout()).rejects.toMatchObject({ status: 500 });
    expect(getAccessToken()).toBeNull();
  });
});
