import { ApiError } from 'api/client';
import { EMAIL_PATTERN, FULL_NAME_MAX } from 'components/sections/authentication/signup';
import { fieldErrorsOf } from 'lib/errors';

export interface ProfileValues {
  fullName: string;
  email: string;
}

export type ProfileErrors = Partial<Record<keyof ProfileValues, string>>;

/** The backend's rules (UserProfileUpdateRequestBody.java): full name required ≤ 100, valid email. */
export const validateProfile = (values: ProfileValues): ProfileErrors => {
  const errors: ProfileErrors = {};
  const fullName = values.fullName.trim();
  if (!fullName) errors.fullName = 'Enter your full name.';
  else if (fullName.length > FULL_NAME_MAX) {
    errors.fullName = `Full name can be at most ${FULL_NAME_MAX} characters.`;
  }
  if (!EMAIL_PATTERN.test(values.email.trim())) errors.email = 'Enter a valid email address.';
  return errors;
};

/** Backend errors on their field: 400 field errors, 409 "Email already exists"; null otherwise. */
export const profileServerErrors = (error: unknown): ProfileErrors | null => {
  const fields = fieldErrorsOf(error);
  const mapped: ProfileErrors = {};
  if (fields.fullName) mapped.fullName = fields.fullName;
  if (fields.email) mapped.email = fields.email;
  if (Object.keys(mapped).length > 0) return mapped;
  if (error instanceof ApiError && error.status === 409 && /email/i.test(error.message)) {
    return { email: 'This email is already used by another account.' };
  }
  return null;
};
