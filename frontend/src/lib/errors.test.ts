import { ApiError, NetworkError } from 'api/client';
import { describe, expect, it } from 'vitest';
import { errorMessage, fieldErrorsOf, formatWait } from './errors';

describe('errorMessage', () => {
  it('explains a network error', () => {
    expect(errorMessage(new NetworkError())).toMatch(/can't reach the server/i);
  });

  it("uses the backend's message for business errors", () => {
    expect(errorMessage(new ApiError(400, 'Selected account has been locked'))).toBe(
      'Selected account has been locked',
    );
    expect(errorMessage(new ApiError(404, 'To account not found'))).toBe('To account not found');
    expect(errorMessage(new ApiError(409, 'idempotencyKey has already been used'))).toBe(
      'idempotencyKey has already been used',
    );
  });

  it('uses fixed wording for 401, 403, 429 and never shows server internals', () => {
    expect(errorMessage(new ApiError(401, 'x'))).toMatch(/session has expired/);
    expect(errorMessage(new ApiError(403, 'Forbidden'))).toBe(
      "You don't have permission to do this.",
    );
    expect(errorMessage(new ApiError(429, 'x', undefined, 90))).toBe(
      'Too many requests. Try again in 2 minutes.',
    );
    expect(errorMessage(new ApiError(500, 'NullPointerException at Foo.java:42'))).toBe(
      'Something went wrong on the server. Please try again later.',
    );
  });

  it('lets a page override the wording per status', () => {
    const error = new ApiError(429, 'x', undefined, 30);
    expect(errorMessage(new ApiError(401, 'x'), { 401: 'Invalid username or password.' })).toBe(
      'Invalid username or password.',
    );
    expect(errorMessage(error, { 429: (e) => `Wait ${e.retryAfterSeconds}s` })).toBe('Wait 30s');
  });

  it('falls back to a generic message for anything else', () => {
    expect(errorMessage(new Error('boom'))).toBe('Something went wrong. Please try again.');
  });
});

describe('fieldErrorsOf', () => {
  it('returns the field errors of a 400 only', () => {
    expect(fieldErrorsOf(new ApiError(400, 'Validation failed', { amount: 'too small' }))).toEqual({
      amount: 'too small',
    });
    expect(fieldErrorsOf(new ApiError(409, 'x', { a: 'b' }))).toEqual({});
    expect(fieldErrorsOf(new Error('x'))).toEqual({});
  });
});

describe('formatWait', () => {
  it.each([
    [1, '1 second'],
    [45, '45 seconds'],
    [60, '1 minute'],
    [90, '2 minutes'],
    [900, '15 minutes'],
  ])('%i s → %j', (seconds, expected) => {
    expect(formatWait(seconds)).toBe(expected);
  });
});
