import { ApiError, apiFetch, refreshAccessToken } from './client';
import { clearAccessToken, setAccessToken } from './tokenStore';
import type { AuthenticationResponse, UserProfile } from './types';

/** POST /api/auth/login. The refresh cookie arrives in Set-Cookie; the access token stays in memory. */
export const login = async (username: string, password: string): Promise<void> => {
  const res = await apiFetch<AuthenticationResponse>('/api/auth/login', {
    method: 'POST',
    body: { username, password },
    auth: false, // a 401 here means wrong credentials, not an expired token
  });
  setAccessToken(res.accessToken);
};

export interface RegisterInput {
  username: string;
  fullName: string;
  email: string;
  password: string;
}

/**
 * POST /api/auth/register. A new user is always a CUSTOMER (the backend has no role field here).
 * Like login, the response signs the user in: access token in memory, refresh token in the cookie.
 */
export const register = async (input: RegisterInput): Promise<void> => {
  const res = await apiFetch<AuthenticationResponse>('/api/auth/register', {
    method: 'POST',
    body: input,
    auth: false,
  });
  setAccessToken(res.accessToken);
};

/** POST /api/auth/logout: revokes the user's tokens on every device and clears the cookie. */
export const logout = async (): Promise<void> => {
  try {
    await apiFetch<void>('/api/auth/logout', { method: 'POST' });
  } finally {
    clearAccessToken();
  }
};

export const getCurrentUser = (): Promise<UserProfile> => apiFetch<UserProfile>('/api/users/me');

export interface ProfileUpdate {
  fullName: string;
  email: string;
}

/** PUT /api/users/me: the signed-in user's own name and email (the username can't be changed). */
export const updateMyProfile = (update: ProfileUpdate): Promise<UserProfile> =>
  apiFetch<UserProfile>('/api/users/me', { method: 'PUT', body: update });

/**
 * POST /api/auth/change-password. The backend ends every other session and starts a new one for this
 * browser: a new access token (kept in memory, the old one no longer works) and a new refresh cookie.
 * A wrong current password is a 400 on `currentPassword`, not a 401, so it never triggers a refresh.
 */
export const changePassword = async (
  currentPassword: string,
  newPassword: string,
): Promise<void> => {
  const res = await apiFetch<AuthenticationResponse>('/api/auth/change-password', {
    method: 'POST',
    body: { currentPassword, newPassword },
  });
  setAccessToken(res.accessToken);
};

let restoring: Promise<UserProfile | null> | null = null;

/**
 * On page load: use the refresh cookie (if any) to get an access token, then load the user.
 * Resolves null when there's no valid session. Shared between concurrent callers
 * (React StrictMode runs effects twice in dev).
 */
export const restoreSession = (): Promise<UserProfile | null> => {
  if (!restoring) {
    restoring = (async () => {
      try {
        await refreshAccessToken();
      } catch (err) {
        if (err instanceof ApiError) return null; // no cookie, or it expired / was revoked
        throw err;
      }
      return getCurrentUser();
    })().finally(() => {
      restoring = null;
    });
  }
  return restoring;
};
