import { ApiError } from 'api/client';
import { describe, expect, it } from 'vitest';
import { signupServerErrors, SignupValues, validateSignup } from './signup';

const valid: SignupValues = {
  fullName: 'Test User',
  username: 'testuser',
  email: 'test@banking.local',
  password: 'Test1234!',
  confirmPassword: 'Test1234!',
};

describe('validateSignup', () => {
  it('accepts a valid form', () => {
    expect(validateSignup(valid)).toEqual({});
  });

  it('checks every field like the backend does', () => {
    expect(
      validateSignup({
        fullName: '   ',
        username: 'ab',
        email: 'not-an-email',
        password: 'short',
        confirmPassword: 'different',
      }),
    ).toEqual({
      fullName: 'Enter the full name.',
      username: 'Username must be 3–30 characters.',
      email: 'Enter a valid email address.',
      password: 'Password must be at least 8 characters.',
      confirmPassword: 'Passwords do not match.',
    });
  });

  it('limits username to 30 and full name to 100 characters', () => {
    const errors = validateSignup({
      ...valid,
      username: 'u'.repeat(31),
      fullName: 'n'.repeat(101),
    });
    expect(errors.username).toBeDefined();
    expect(errors.fullName).toBe('Full name can be at most 100 characters.');
  });

  it('ignores spaces around the username and email', () => {
    expect(validateSignup({ ...valid, username: '  abc  ', email: ' a@b.co ' })).toEqual({});
  });
});

describe('signupServerErrors', () => {
  it('maps bean-validation field errors', () => {
    const error = new ApiError(400, 'Request validation failed', {
      username: 'size must be between 3 and 30',
    });
    expect(signupServerErrors(error)).toEqual({ username: 'Size must be between 3 and 30.' });
  });

  it('puts "already exists" on the username or email field', () => {
    expect(signupServerErrors(new ApiError(409, 'Username already exists'))).toEqual({
      username: 'Username already exists.',
    });
    expect(signupServerErrors(new ApiError(409, 'Email already exists'))).toEqual({
      email: 'Email already exists.',
    });
  });

  it('maps domain messages that name a field', () => {
    expect(signupServerErrors(new ApiError(400, 'email format is invalid'))).toEqual({
      email: 'Email format is invalid.',
    });
  });

  it('returns null for errors that are not about one field', () => {
    expect(signupServerErrors(new ApiError(409, 'Username or email already exists'))).toBeNull();
    expect(signupServerErrors(new ApiError(500, 'boom'))).toBeNull();
    expect(signupServerErrors(new Error('x'))).toBeNull();
  });
});
