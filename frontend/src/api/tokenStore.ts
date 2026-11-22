/**
 * The access token lives only in this module's memory: never localStorage or sessionStorage,
 * so an XSS script can't read it from storage. A page reload loses it on purpose;
 * AuthProvider then gets a new one with the httpOnly refresh cookie.
 */
let accessToken: string | null = null;

export const getAccessToken = (): string | null => accessToken;

export const setAccessToken = (token: string): void => {
  accessToken = token;
};

export const clearAccessToken = (): void => {
  accessToken = null;
};
