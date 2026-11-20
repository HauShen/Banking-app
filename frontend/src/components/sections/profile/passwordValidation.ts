import { PASSWORD_MIN } from 'components/sections/authentication/signup';
import { fieldErrorsOf } from 'lib/errors';

/** Same as the backend (ChangePasswordRequest.java): 8–100 characters. */
export const PASSWORD_MAX = 100;

export interface PasswordValues {
  currentPassword: string;
  newPassword: string;
  confirmPassword: string;
}

export type PasswordErrors = Partial<Record<keyof PasswordValues, string>>;

export const EMPTY_PASSWORDS: PasswordValues = {
  currentPassword: '',
  newPassword: '',
  confirmPassword: '',
};

/** Passwords are never trimmed: spaces are part of a password. */
export const validatePasswordChange = (values: PasswordValues): PasswordErrors => {
  const errors: PasswordErrors = {};
  if (!values.currentPassword) errors.currentPassword = 'Enter your current password.';
  if (values.newPassword.length < PASSWORD_MIN || values.newPassword.length > PASSWORD_MAX) {
    errors.newPassword = `Password must be ${PASSWORD_MIN}–${PASSWORD_MAX} characters.`;
  } else if (values.newPassword === values.currentPassword) {
    errors.newPassword = 'New password must differ from the current one.';
  }
  if (values.confirmPassword !== values.newPassword) {
    errors.confirmPassword = 'Passwords do not match.';
  }
  return errors;
};

/** The backend's 400 field errors (wrong current password, unchanged or invalid new one); null otherwise. */
export const passwordServerErrors = (error: unknown): PasswordErrors | null => {
  const fields = fieldErrorsOf(error);
  const mapped: PasswordErrors = {};
  if (fields.currentPassword) mapped.currentPassword = fields.currentPassword;
  if (fields.newPassword) mapped.newPassword = fields.newPassword;
  return Object.keys(mapped).length > 0 ? mapped : null;
};
