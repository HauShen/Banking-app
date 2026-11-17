import { ApiError } from 'api/client';
import { fieldErrorsOf } from 'lib/errors';

export interface SignupValues {
  fullName: string;
  username: string;
  email: string;
  password: string;
  confirmPassword: string;
}

export type SignupField = keyof SignupValues;
export type SignupErrors = Partial<Record<SignupField, string>>;

// Same rules as the backend (UserProfile.java, RegisterUserProfileCommand.java), so most mistakes
// are caught before any request is sent. The backend still checks everything.
export const USERNAME_MIN = 3;
export const USERNAME_MAX = 30;
export const FULL_NAME_MAX = 100;
export const PASSWORD_MIN = 8;
const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

export const validateSignup = (values: SignupValues): SignupErrors => {
  const errors: SignupErrors = {};
  const fullName = values.fullName.trim();
  const username = values.username.trim();

  if (!fullName) errors.fullName = 'Enter your full name.';
  else if (fullName.length > FULL_NAME_MAX) {
    errors.fullName = `Full name can be at most ${FULL_NAME_MAX} characters.`;
  }
  if (username.length < USERNAME_MIN || username.length > USERNAME_MAX) {
    errors.username = `Username must be ${USERNAME_MIN}–${USERNAME_MAX} characters.`;
  }
  if (!EMAIL_PATTERN.test(values.email.trim())) errors.email = 'Enter a valid email address.';
  if (values.password.length < PASSWORD_MIN) {
    errors.password = `Password must be at least ${PASSWORD_MIN} characters.`;
  }
  if (values.confirmPassword !== values.password) {
    errors.confirmPassword = 'Passwords do not match.';
  }
  return errors;
};

const SERVER_FIELDS: SignupField[] = ['fullName', 'username', 'email', 'password'];

/** "username must be 3-30 characters" → "Username must be 3-30 characters." */
const asSentence = (message: string): string => {
  const text = message.charAt(0).toUpperCase() + message.slice(1);
  return /[.!?]$/.test(text) ? text : `${text}.`;
};

/**
 * Puts a backend error on the field it is about, or returns null when it isn't about one field
 * (then the form shows it above the fields). Handles:
 * - 400 bean validation: fieldErrors { username: '...' }
 * - 400 domain checks: "username must be 3-30 characters", "email format is invalid"
 * - 409: "Username already exists", "Email already exists"
 */
export const signupServerErrors = (error: unknown): SignupErrors | null => {
  const mapped: SignupErrors = {};
  for (const [name, message] of Object.entries(fieldErrorsOf(error))) {
    if ((SERVER_FIELDS as string[]).includes(name))
      mapped[name as SignupField] = asSentence(message);
  }
  if (Object.keys(mapped).length > 0) return mapped;

  if (error instanceof ApiError && (error.status === 400 || error.status === 409)) {
    const message = error.message.toLowerCase();
    if (message.includes(' or ')) return null; // "Username or email already exists": not one field
    const field = SERVER_FIELDS.find((name) => message.startsWith(name.toLowerCase()));
    if (field) return { [field]: asSentence(error.message) };
  }
  return null;
};
