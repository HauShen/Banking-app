import { ApiError } from 'api/client';
import { describe, expect, it } from 'vitest';
import { remittanceProblem, validateRemittance } from './remittanceValidation';

describe('validateRemittance', () => {
  it('returns the API amount and the trimmed sender name', () => {
    expect(
      validateRemittance({ currency: 'USD', amount: '100', senderName: '  John Tan ' }),
    ).toEqual({
      errors: {},
      amount: '100.00',
      senderName: 'John Tan',
    });
    expect(validateRemittance({ currency: 'JPY', amount: '5000', senderName: 'Aiko' }).amount).toBe(
      '5000',
    );
  });

  it('checks the amount with the currency decimals and requires a sender of at most 100 characters', () => {
    expect(validateRemittance({ currency: 'JPY', amount: '10.5', senderName: '' }).errors).toEqual({
      amount: 'Enter a whole number of JPY, e.g. 1000.',
      senderName: 'Enter who sent the money.',
    });
    expect(
      validateRemittance({ currency: 'USD', amount: '1', senderName: 'x'.repeat(101) }).errors
        .senderName,
    ).toBe('At most 100 characters.');
  });
});

describe('remittanceProblem', () => {
  it('explains a locked or missing account', () => {
    expect(
      remittanceProblem(new ApiError(400, 'Selected account has been locked')).message,
    ).toMatch(/this account is locked/i);
    expect(remittanceProblem(new ApiError(404, 'To account not found')).message).toBe(
      'This account no longer exists.',
    );
  });

  it('puts currency and amount problems on their field', () => {
    expect(remittanceProblem(new ApiError(400, 'MYR needs no conversion'))).toMatchObject({
      field: 'currency',
    });
    expect(remittanceProblem(new ApiError(400, 'JPY amounts cannot have decimal places'))).toEqual({
      field: 'amount',
      message: 'JPY amounts cannot have decimal places.',
    });
  });

  it('asks for a new key when the key belongs to a different transaction', () => {
    expect(
      remittanceProblem(
        new ApiError(409, 'idempotencyKey has already been used for a different transaction'),
      ),
    ).toMatchObject({ newKeyNeeded: true });
  });
});
