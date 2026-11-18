import { ApiError, NetworkError } from 'api/client';
import type { Account } from 'api/types';
import { describe, expect, it } from 'vitest';
import { transferProblem, TransferValues, validateTransfer } from './transferValidation';

const active: Account = {
  accountId: 1,
  userId: 'u1',
  accountNumber: '1111111111',
  accountType: 'SAVINGS',
  accountStatus: 'ACTIVE',
  accountCurrency: 'MYR',
  balance: 500,
  createdAt: '2026-11-01T00:00:00Z',
};
const locked: Account = {
  ...active,
  accountId: 2,
  accountNumber: '2222222222',
  accountStatus: 'LOCKED',
};

const values = (overrides: Partial<TransferValues> = {}): TransferValues => ({
  fromAccountNumber: '1111111111',
  toAccountNumber: '3333 333 333',
  amount: '50',
  currency: 'MYR',
  description: '  Rent  ',
  ...overrides,
});

describe('validateTransfer', () => {
  it('returns the request to send: digits-only account, amount string, trimmed description', () => {
    expect(validateTransfer(values(), [active])).toEqual({
      errors: {},
      transfer: {
        fromAccountNumber: '1111111111',
        toAccountNumber: '3333333333',
        amount: '50.00',
        currency: 'MYR',
        description: 'Rent',
      },
    });
  });

  it('checks every field', () => {
    const { errors, transfer } = validateTransfer(
      values({ fromAccountNumber: '', toAccountNumber: '123', amount: '0', description: ' ' }),
      [active],
    );
    expect(transfer).toBeUndefined();
    expect(Object.keys(errors).sort()).toEqual([
      'amount',
      'description',
      'fromAccountNumber',
      'toAccountNumber',
    ]);
  });

  it('refuses sending to the same account or from a locked one', () => {
    expect(validateTransfer(values({ toAccountNumber: '1111111111' }), [active]).errors).toEqual({
      toAccountNumber: 'Choose an account other than the one you send from.',
    });
    expect(
      validateTransfer(values({ fromAccountNumber: '2222222222' }), [active, locked]).errors,
    ).toEqual({ fromAccountNumber: 'This account is locked.' });
  });

  it('limits the description to 255 characters (database column)', () => {
    expect(
      validateTransfer(values({ description: 'x'.repeat(256) }), [active]).errors.description,
    ).toBe('At most 255 characters.');
  });
});

describe('transferProblem', () => {
  it('puts known backend errors on their field', () => {
    expect(transferProblem(new ApiError(404, 'To account not found'))).toEqual({
      field: 'toAccountNumber',
      message: 'No account has this number.',
    });
    expect(
      transferProblem(
        new ApiError(400, 'Insufficient balance. Minimum balance of $20.00 must be maintained.'),
      ),
    ).toEqual({ field: 'amount', message: 'Not enough money: RM 20.00 must stay in the account.' });
    expect(transferProblem(new ApiError(400, 'Cannot transfer to the same account')).field).toBe(
      'toAccountNumber',
    );
  });

  it('explains locked accounts on either side', () => {
    expect(transferProblem(new ApiError(400, 'Selected account has been locked')).message).toMatch(
      /recipient's account is locked/,
    );
    expect(
      transferProblem(new ApiError(400, 'Cannot process transfer with an locked account.')).message,
    ).toMatch(/your account is locked/i);
  });

  it('asks for a new key when the key was used for a different transfer', () => {
    expect(
      transferProblem(
        new ApiError(
          409,
          'idempotencyKey has already been used for a transfer with a different amount or recipient',
        ),
      ),
    ).toMatchObject({ newKeyNeeded: true });
  });

  it('falls back to the general error message', () => {
    expect(transferProblem(new NetworkError()).message).toMatch(/can't reach the server/i);
    expect(transferProblem(new ApiError(500, 'boom')).field).toBeUndefined();
  });
});

describe('foreign currency', () => {
  it('uses the decimals of the chosen currency', () => {
    expect(
      validateTransfer(values({ currency: 'USD', amount: '100.5' }), [active]).transfer,
    ).toMatchObject({
      currency: 'USD',
      amount: '100.50',
    });
    expect(
      validateTransfer(values({ currency: 'JPY', amount: '1000' }), [active]).transfer?.amount,
    ).toBe('1000');
    expect(
      validateTransfer(values({ currency: 'JPY', amount: '10.5' }), [active]).errors.amount,
    ).toBe('Enter a whole number of JPY, e.g. 1000.');
  });

  it('asks for a new rate when the locked quote has expired or was used', () => {
    expect(transferProblem(new ApiError(400, 'The quote has expired'))).toMatchObject({
      quoteExpired: true,
    });
    expect(transferProblem(new ApiError(400, 'The quote has already been used'))).toMatchObject({
      quoteExpired: true,
    });
    expect(transferProblem(new ApiError(400, 'Quote not found'))).toMatchObject({
      quoteExpired: true,
    });
  });

  it('puts currency-amount problems on the amount field', () => {
    expect(transferProblem(new ApiError(400, 'JPY amounts cannot have decimal places'))).toEqual({
      field: 'amount',
      message: 'JPY amounts cannot have decimal places.',
    });
    expect(
      transferProblem(new ApiError(400, 'The converted amount is less than RM 0.01')).field,
    ).toBe('amount');
    expect(transferProblem(new ApiError(400, 'Currency XYZ is not supported')).field).toBe(
      'currency',
    );
  });
});
