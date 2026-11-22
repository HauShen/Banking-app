import { delay, http, HttpResponse } from 'msw';
import { API, server } from 'test/server';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { ApiError, apiFetch, NetworkError, setAuthFailureListener } from './client';
import { clearAccessToken, getAccessToken, setAccessToken } from './tokenStore';

/** /api/users/me accepts only `acceptedToken()`; /api/auth/refresh issues `nextToken` (or refuses). */
const mockBackend = (options: {
  acceptedToken: () => string;
  refresh?: () => Promise<Response> | Response;
}) => {
  const refreshCalls = { count: 0 };
  server.use(
    http.get(`${API}/api/users/me`, ({ request }) =>
      request.headers.get('Authorization') === `Bearer ${options.acceptedToken()}`
        ? HttpResponse.json({ username: 'hau' })
        : HttpResponse.json({ status: 401, message: 'Unauthorized' }, { status: 401 }),
    ),
    http.post(`${API}/api/auth/refresh`, async () => {
      refreshCalls.count++;
      return options.refresh
        ? options.refresh()
        : HttpResponse.json({ status: 401, message: 'Invalid refresh token' }, { status: 401 });
    }),
  );
  return refreshCalls;
};

beforeEach(() => {
  clearAccessToken();
  setAuthFailureListener(null);
});

describe('apiFetch', () => {
  it('sends the access token and a JSON body', async () => {
    setAccessToken('t1');
    let seen: { auth: string | null; type: string | null; body: unknown } | undefined;
    server.use(
      http.post(`${API}/api/things`, async ({ request }) => {
        seen = {
          auth: request.headers.get('Authorization'),
          type: request.headers.get('Content-Type'),
          body: await request.json(),
        };
        return HttpResponse.json({ ok: true });
      }),
    );

    const result = await apiFetch<{ ok: boolean }>('/api/things', {
      method: 'POST',
      body: { amount: '10.00' },
    });

    expect(result).toEqual({ ok: true });
    expect(seen).toEqual({
      auth: 'Bearer t1',
      type: 'application/json',
      body: { amount: '10.00' },
    });
  });

  it('refreshes the token once and retries after a 401', async () => {
    setAccessToken('expired');
    const refreshCalls = mockBackend({
      acceptedToken: () => 'fresh',
      refresh: () =>
        HttpResponse.json({ accessToken: 'fresh', tokenType: 'Bearer', expiresIn: 900 }),
    });

    await expect(apiFetch('/api/users/me')).resolves.toEqual({ username: 'hau' });
    expect(refreshCalls.count).toBe(1);
    expect(getAccessToken()).toBe('fresh');
  });

  it('lets parallel requests share one refresh (the refresh token is rotated on every use)', async () => {
    setAccessToken('expired');
    const refreshCalls = mockBackend({
      acceptedToken: () => 'fresh',
      refresh: async () => {
        await delay(20);
        return HttpResponse.json({ accessToken: 'fresh', tokenType: 'Bearer', expiresIn: 900 });
      },
    });

    const results = await Promise.all([
      apiFetch('/api/users/me'),
      apiFetch('/api/users/me'),
      apiFetch('/api/users/me'),
    ]);

    expect(results).toHaveLength(3);
    expect(refreshCalls.count).toBe(1);
  });

  it('ends the session when the refresh is refused', async () => {
    setAccessToken('expired');
    const onAuthFailure = vi.fn();
    setAuthFailureListener(onAuthFailure);
    mockBackend({ acceptedToken: () => 'never' });

    const error = await apiFetch('/api/users/me').catch((e: unknown) => e);

    expect(error).toBeInstanceOf(ApiError);
    expect((error as ApiError).status).toBe(401);
    expect((error as ApiError).message).toMatch(/expired/);
    expect(onAuthFailure).toHaveBeenCalledOnce();
    expect(getAccessToken()).toBeNull();
  });

  it('keeps the session when the server is unreachable', async () => {
    setAccessToken('t1');
    const onAuthFailure = vi.fn();
    setAuthFailureListener(onAuthFailure);
    server.use(http.get(`${API}/api/users/me`, () => HttpResponse.error()));

    await expect(apiFetch('/api/users/me')).rejects.toBeInstanceOf(NetworkError);
    expect(onAuthFailure).not.toHaveBeenCalled();
    expect(getAccessToken()).toBe('t1');
  });

  it('does not try to refresh when auth is false (e.g. a wrong password on login)', async () => {
    const refreshCalls = mockBackend({ acceptedToken: () => 'x' });
    server.use(
      http.post(`${API}/api/auth/login`, () =>
        HttpResponse.json({ status: 401, message: 'Bad credentials' }, { status: 401 }),
      ),
    );

    await expect(
      apiFetch('/api/auth/login', { method: 'POST', body: {}, auth: false }),
    ).rejects.toMatchObject({ status: 401 });
    expect(refreshCalls.count).toBe(0);
  });

  it('reads the message, field errors and Retry-After from error responses', async () => {
    server.use(
      http.post(`${API}/api/auth/login`, () =>
        HttpResponse.json(
          { status: 429, message: 'Too many attempts' },
          { status: 429, headers: { 'Retry-After': '90' } },
        ),
      ),
      http.post(`${API}/api/transfers`, () =>
        HttpResponse.json(
          { status: 400, message: 'Validation failed', fieldErrors: { amount: 'too small' } },
          { status: 400 },
        ),
      ),
    );

    await expect(
      apiFetch('/api/auth/login', { method: 'POST', body: {}, auth: false }),
    ).rejects.toMatchObject({ status: 429, message: 'Too many attempts', retryAfterSeconds: 90 });
    await expect(apiFetch('/api/transfers', { method: 'POST', body: {} })).rejects.toMatchObject({
      status: 400,
      fieldErrors: { amount: 'too small' },
    });
  });

  it('returns undefined for 204 No Content', async () => {
    setAccessToken('t1');
    server.use(http.post(`${API}/api/auth/logout`, () => new HttpResponse(null, { status: 204 })));

    await expect(apiFetch('/api/auth/logout', { method: 'POST' })).resolves.toBeUndefined();
  });
});
