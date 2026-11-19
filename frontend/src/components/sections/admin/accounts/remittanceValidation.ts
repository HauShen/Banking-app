import { ApiError } from 'api/client';
import { errorMessage, fieldErrorsOf } from 'lib/errors';
import { currencyDecimals, normalizeAmountInput } from 'lib/money';

export interface RemittanceValues {
  currency: string;
  amount: string;
  senderName: string;
}

export type RemittanceField = keyof RemittanceValues;
export type RemittanceErrors = Partial<Record<RemittanceField, string>>;

/** RemittanceRequestBody.java: senderName @Size(max = 100). */
export const SENDER_NAME_MAX = 100;

export const validateRemittance = (
  values: RemittanceValues,
): { errors: RemittanceErrors; amount?: string; senderName?: string } => {
  const errors: RemittanceErrors = {};
  const amount = normalizeAmountInput(values.amount, values.currency);
  const senderName = values.senderName.trim();

  if (!values.currency) errors.currency = 'Choose the currency the money was sent in.';
  if (!amount) {
    errors.amount =
      currencyDecimals(values.currency) === 0
        ? `Enter a whole number of ${values.currency}, e.g. 1000.`
        : 'Enter an amount, e.g. 100 or 250.50.';
  }
  if (!senderName) errors.senderName = 'Enter who sent the money.';
  else if (senderName.length > SENDER_NAME_MAX) {
    errors.senderName = `At most ${SENDER_NAME_MAX} characters.`;
  }

  if (Object.keys(errors).length > 0) return { errors };
  return { errors, amount: amount as string, senderName };
};

export interface RemittanceProblem {
  field?: RemittanceField;
  message: string;
  /** The key was used for a different transaction: the next attempt needs a new key. */
  newKeyNeeded?: boolean;
}

/** Backend errors (RecordForeignRemittanceService) in words for the admin, on the field they concern. */
export const remittanceProblem = (error: unknown): RemittanceProblem => {
  const fields = fieldErrorsOf(error);
  for (const field of ['amount', 'currency', 'senderName'] as const) {
    if (fields[field]) return { field, message: fields[field] };
  }
  if (error instanceof ApiError) {
    const message = error.message;
    if (message === 'Selected account has been locked') {
      return { message: 'This account is locked, so it cannot receive money. Unlock it first.' };
    }
    if (/^to account not found/i.test(message)) {
      return { message: 'This account no longer exists.' };
    }
    if (/MYR needs no conversion/i.test(message)) {
      return { field: 'currency', message: 'MYR needs no conversion: use a cash deposit instead.' };
    }
    if (/is not supported$/i.test(message)) {
      return { field: 'currency', message: 'This currency is not offered.' };
    }
    if (/cannot have decimal places|at most \d+ decimal places/i.test(message)) {
      return { field: 'amount', message: `${message}.` };
    }
    if (/less than RM 0\.01/i.test(message)) {
      return {
        field: 'amount',
        message: 'This amount is worth less than RM 0.01. Enter a larger amount.',
      };
    }
    if (/idempotencyKey/i.test(message)) {
      return {
        message:
          'This request was already used for a different transaction. Check the account history first.',
        newKeyNeeded: true,
      };
    }
  }
  return { message: errorMessage(error) };
};
