import { ApiError } from 'api/client';
import type { Account } from 'api/types';
import { errorMessage, fieldErrorsOf } from 'lib/errors';
import { normalizeAmountInput } from 'lib/money';

export interface TransferValues {
  fromAccountNumber: string;
  toAccountNumber: string;
  amount: string;
  description: string;
}

export type TransferField = keyof TransferValues;
export type TransferErrors = Partial<Record<TransferField, string>>;

/** What is sent after validation: digits-only account number, amount as the API string. */
export interface ValidTransfer {
  fromAccountNumber: string;
  toAccountNumber: string;
  amount: string;
  description: string;
}

/** Account.MINIMUM_BALANCE in the backend. */
export const MINIMUM_BALANCE_TEXT = 'RM 20.00';
/** transactions.description is VARCHAR(255). */
export const DESCRIPTION_MAX = 255;

const ACCOUNT_NUMBER = /^\d{10}$/;

export const validateTransfer = (
  values: TransferValues,
  ownAccounts: Account[],
): { errors: TransferErrors; transfer?: ValidTransfer } => {
  const errors: TransferErrors = {};
  const from = ownAccounts.find((a) => a.accountNumber === values.fromAccountNumber);
  const to = values.toAccountNumber.replace(/\s/g, '');
  const amount = normalizeAmountInput(values.amount);
  const description = values.description.trim();

  if (!from) errors.fromAccountNumber = 'Choose the account to send from.';
  else if (from.accountStatus !== 'ACTIVE') errors.fromAccountNumber = 'This account is locked.';

  if (!ACCOUNT_NUMBER.test(to)) errors.toAccountNumber = 'Enter the 10-digit account number.';
  else if (to === values.fromAccountNumber) {
    errors.toAccountNumber = 'Choose an account other than the one you send from.';
  }

  if (!amount) errors.amount = 'Enter an amount, e.g. 50 or 12.50.';

  if (!description) errors.description = 'Add a short description, e.g. "Rent".';
  else if (description.length > DESCRIPTION_MAX) {
    errors.description = `At most ${DESCRIPTION_MAX} characters.`;
  }

  if (Object.keys(errors).length > 0 || !amount) return { errors };
  return {
    errors,
    transfer: {
      fromAccountNumber: values.fromAccountNumber,
      toAccountNumber: to,
      amount,
      description,
    },
  };
};

export interface TransferProblem {
  /** Set when the problem belongs to one field: the page goes back to the form and marks it. */
  field?: TransferField;
  message: string;
  /** The key was used for a different transfer: the next attempt needs a new key. */
  newKeyNeeded?: boolean;
}

/** Turns a transfer error from the backend into words for the customer, on the field it concerns. */
export const transferProblem = (error: unknown): TransferProblem => {
  const fields = fieldErrorsOf(error);
  for (const field of ['toAccountNumber', 'amount', 'description', 'fromAccountNumber'] as const) {
    if (fields[field]) return { field, message: fields[field] };
  }
  if (error instanceof ApiError) {
    const message = error.message;
    if (error.status === 404 && /^to account not found/i.test(message)) {
      return { field: 'toAccountNumber', message: 'No account has this number.' };
    }
    if (/same account/i.test(message)) {
      return {
        field: 'toAccountNumber',
        message: 'Choose an account other than the one you send from.',
      };
    }
    if (/^insufficient balance/i.test(message)) {
      return {
        field: 'amount',
        message: `Not enough money: ${MINIMUM_BALANCE_TEXT} must stay in the account.`,
      };
    }
    if (message === 'Selected account has been locked') {
      return { message: "The recipient's account is locked, so it can't receive money right now." };
    }
    if (/locked account/i.test(message)) {
      return { message: "Your account is locked, so you can't send money from it." };
    }
    if (/idempotencyKey/i.test(message)) {
      return {
        message:
          'This transfer was already sent with different details. Check your transactions before trying again.',
        newKeyNeeded: true,
      };
    }
  }
  return { message: errorMessage(error) };
};
