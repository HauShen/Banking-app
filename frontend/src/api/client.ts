import { clearAccessToken, getAccessToken, setAccessToken } from './tokenStore';
import type { ApiErrorBody, AuthenticationResponse } from './types';

/** Empty by default: requests go to /api on the same origin, and the Vite dev proxy forwards them. */
const API_BASE_URL = (import.meta.env.VITE_API_BASE_URL ?? '').replace(/\/$/, '');

/** The server answered with an error status. */
export class ApiError extends Error {
  readonly status: number;
  readonly fieldErrors?: Record<string, string>;
  /** From the Retry-After header, e.g. on 429 when login is locked. */
  readonly retryAfterSeconds?: number;

  constructor(
    status: number,
    message: string,
    fieldErrors?: Record<string, string>,
    retryAfterSeconds?: number,
  ) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.fieldErrors = fieldErrors;
    this.retryAfterSeconds = retryAfterSeconds;
  }
}

/** The server couldn't be reached at all (backend down, no network, request blocked). */
export class NetworkError extends Error {
  constructor() {
    super("Can't reach the server.");
    this.name = 'NetworkError';
  }
}

/** Called when the session can't be recovered (refresh failed), so the app can show the login page. */
let authFailureListener: (() => void) | null = null;

export const setAuthFailureListener = (listener: (() => void) | null): void => {
  authFailureListener = listener;
};

const endSession = (): void => {
  clearAccessToken();
  authFailureListener?.();
};

const send = async (path: string, init: RequestInit): Promise<Response> => {
  try {
    // credentials: 'include' so the browser sends and stores the refresh cookie,
    // also when VITE_API_BASE_URL points at another origin.
    return await fetch(`${API_BASE_URL}${path}`, { ...init, credentials: 'include' });
  } catch {
    throw new NetworkError();
  }
};

const toApiError = async (res: Response): Promise<ApiError> => {
  let body: Partial<ApiErrorBody> = {};
  try {
    body = (await res.json()) as Partial<ApiErrorBody>;
  } catch {
    // Empty or non-JSON body: fall back to the status text below.
  }
  const retryAfter = Number(res.headers.get('Retry-After'));
  return new ApiError(
    res.status,
    body.message || res.statusText || `Request failed (${res.status})`,
    body.fieldErrors ?? undefined,
    Number.isFinite(retryAfter) && retryAfter > 0 ? retryAfter : undefined,
  );
};

let refreshInFlight: Promise<string> | null = null;

/**
 * Trades the httpOnly refresh cookie for a new access token (and a new cookie).
 * Callers that arrive while a refresh is running share it: the backend rotates the
 * refresh token on every use, so a second parallel request would send a used token.
 */
export const refreshAccessToken = (): Promise<string> => {
  if (!refreshInFlight) {
    refreshInFlight = (async () => {
      const res = await send('/api/auth/refresh', { method: 'POST' });
      if (!res.ok) throw await toApiError(res);
      const body = (await res.json()) as AuthenticationResponse;
      setAccessToken(body.accessToken);
      return body.accessToken;
    })().finally(() => {
      refreshInFlight = null;
    });
  }
  return refreshInFlight;
};

export interface RequestOptions extends Omit<RequestInit, 'body'> {
  /** Sent as JSON. */
  body?: unknown;
  /** Attach the access token and refresh it on 401. Default true; false for login/register. */
  auth?: boolean;
}

/**
 * fetch wrapper for the Banking API: JSON in and out, Bearer token, and one
 * refresh-and-retry when the access token has expired (401).
 */
export const apiFetch = async <T>(path: string, options: RequestOptions = {}): Promise<T> => {
  const { body, auth = true, headers, ...rest } = options;

  const buildInit = (): RequestInit => {
    const h = new Headers(headers);
    if (body !== undefined) h.set('Content-Type', 'application/json');
    const token = getAccessToken();
    if (auth && token) h.set('Authorization', `Bearer ${token}`);
    return { ...rest, headers: h, body: body === undefined ? undefined : JSON.stringify(body) };
  };

  let res = await send(path, buildInit());

  if (res.status === 401 && auth) {
    try {
      await refreshAccessToken();
    } catch (err) {
      // A server-side refusal means the session is over; a network error doesn't.
      if (err instanceof ApiError) {
        endSession();
        throw new ApiError(401, 'Your session has expired. Please sign in again.');
      }
      throw err;
    }
    res = await send(path, buildInit());
    if (res.status === 401) endSession();
  }

  if (!res.ok) throw await toApiError(res);
  if (res.status === 204) return undefined as T;
  const text = await res.text();
  return (text ? JSON.parse(text) : undefined) as T;
};
