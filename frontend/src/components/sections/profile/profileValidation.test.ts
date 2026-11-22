import { ApiError } from 'api/client';
import { describe, expect, it } from 'vitest';
import { profileServerErrors, validateProfile } from './profileValidation';

describe('validateProfile', () => {
  it('accepts a valid name and email', () => {
    expect(validateProfile({ fullName: ' Hau Shen ', email: 'hau@banking.local' })).toEqual({});
  });

  it('requires a name of at most 100 characters and a valid email', () => {
    expect(validateProfile({ fullName: '  ', email: 'nope' })).toEqual({
      fullName: 'Enter your full name.',
      email: 'Enter a valid email address.',
    });
    expect(validateProfile({ fullName: 'n'.repeat(101), email: 'a@b.co' }).fullName).toBe(
      'Full name can be at most 100 characters.',
    );
  });
});

describe('profileServerErrors', () => {
  it('puts field errors and "Email already exists" on their field', () => {
    expect(
      profileServerErrors(
        new ApiError(400, 'Request validation failed', { email: 'Invalid email format' }),
      ),
    ).toEqual({ email: 'Invalid email format' });
    expect(profileServerErrors(new ApiError(409, 'Email already exists'))).toEqual({
      email: 'This email is already used by another account.',
    });
  });

  it('returns null for other errors', () => {
    expect(profileServerErrors(new ApiError(500, 'boom'))).toBeNull();
  });
});
