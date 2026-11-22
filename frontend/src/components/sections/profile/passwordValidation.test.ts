import { ApiError } from 'api/client';
import { describe, expect, it } from 'vitest';
import { passwordServerErrors, validatePasswordChange } from './passwordValidation';

const values = (currentPassword: string, newPassword: string, confirmPassword = newPassword) => ({
  currentPassword,
  newPassword,
  confirmPassword,
});

describe('validatePasswordChange', () => {
  it('accepts a new password of 8–100 characters that differs from the current one', () => {
    expect(validatePasswordChange(values('old-password', 'abcdefgh'))).toEqual({});
    expect(validatePasswordChange(values('old-password', 'x'.repeat(100)))).toEqual({});
  });

  it('rejects a short or too long new password, an unchanged one and a mismatched confirmation', () => {
    expect(validatePasswordChange(values('old', 'short')).newPassword).toBe(
      'Password must be 8–100 characters.',
    );
    expect(validatePasswordChange(values('old', 'x'.repeat(101))).newPassword).toBeDefined();
    expect(validatePasswordChange(values('same-password', 'same-password')).newPassword).toBe(
      'New password must differ from the current one.',
    );
    expect(validatePasswordChange(values('old', 'abcdefgh', 'abcdefgH')).confirmPassword).toBe(
      'Passwords do not match.',
    );
    expect(validatePasswordChange(values('', 'abcdefgh')).currentPassword).toBe(
      'Enter your current password.',
    );
  });

  it('never trims: spaces count as part of the password', () => {
    expect(validatePasswordChange(values('old', '  abcd  ')).newPassword).toBeUndefined();
  });
});

describe('passwordServerErrors', () => {
  it('puts the backend field errors on their fields, and returns null for anything else', () => {
    const wrong = new ApiError(400, 'Current password is incorrect', {
      currentPassword: 'Current password is incorrect',
    });
    expect(passwordServerErrors(wrong)).toEqual({
      currentPassword: 'Current password is incorrect',
    });
    expect(passwordServerErrors(new ApiError(500, 'boom'))).toBeNull();
  });
});
