import { ApiError, NetworkError } from 'api/client';

/** 90 → "2 minutes", 1 → "1 second". */
export const formatWait = (seconds: number): string => {
  if (seconds < 60) return `${seconds} second${seconds === 1 ? '' : 's'}`;
  const minutes = Math.ceil(seconds / 60);
  return `${minutes} minute${minutes === 1 ? '' : 's'}`;
};

type MessageOverride = string | ((error: ApiError) => string);

/** Context-specific wording per HTTP status, e.g. { 401: 'Invalid username or password.' } on the login form. */
export type ErrorMessages = Partial<Record<number, MessageOverride>>;

/**
 * One sentence to show the user for anything the API client throws.
 * 400 / 404 / 409 use the backend's own message (e.g. "Selected account has been locked"), and so does 403 when the
 * backend wrote one (e.g. "You can only view your own accounts", or the live demo's "This is a shared demo
 * user…"); 5xx never shows server internals.
 */
export const errorMessage = (error: unknown, overrides: ErrorMessages = {}): string => {
  if (error instanceof NetworkError) {
    return "Can't reach the server. Check your connection and try again.";
  }
  if (!(error instanceof ApiError)) return 'Something went wrong. Please try again.';

  const override = overrides[error.status];
  if (override) return typeof override === 'function' ? override(error) : override;

  if (error.status === 401) return 'Your session has expired. Please sign in again.';
  if (error.status === 403) {
    // "Forbidden" is only the status text: the backend didn't say why.
    return error.message && error.message !== 'Forbidden'
      ? error.message
      : "You don't have permission to do this.";
  }
  if (error.status === 429) {
    return error.retryAfterSeconds
      ? `Too many requests. Try again in ${formatWait(error.retryAfterSeconds)}.`
      : 'Too many requests. Try again later.';
  }
  if (error.status >= 500) return 'Something went wrong on the server. Please try again later.';
  return error.message || 'Something went wrong. Please try again.';
};

/** Per-field messages of a 400 validation error, e.g. { amount: 'amount must be greater than 0' }; otherwise {}. */
export const fieldErrorsOf = (error: unknown): Record<string, string> =>
  error instanceof ApiError && error.status === 400 && error.fieldErrors ? error.fieldErrors : {};
