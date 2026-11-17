import type { Transaction } from 'api/types';
import { describe, expect, it } from 'vitest';
import { describeTransaction, formatAccountNumber } from './transactions';

const base: Transaction = {
  reference: 'TXN-1',
  type: 'TRANSFER',
  fromAccountNumber: '1111111111',
  toAccountNumber: '2222222222',
  amount: 50,
  currency: 'MYR',
  originalCurrency: null,
  originalAmount: null,
  exchangeRate: null,
  status: 'SUCCESS',
  description: 'Lunch',
  createdAt: '2026-11-17T12:00:00Z',
  postedAt: '2026-11-17T12:00:00Z',
};

describe('formatAccountNumber', () => {
  it('groups 10-digit account numbers', () => {
    expect(formatAccountNumber('1234567890')).toBe('1234 567 890');
    expect(formatAccountNumber('Cash Deposit Machine')).toBe('Cash Deposit Machine');
  });
});

describe('describeTransaction', () => {
  it('is "out" for the sender and "in" for the receiver of a transfer', () => {
    expect(describeTransaction(base, '1111111111')).toEqual({
      direction: 'out',
      title: 'Lunch',
      counterparty: 'To 2222 222 222',
    });
    expect(describeTransaction(base, '2222222222')).toEqual({
      direction: 'in',
      title: 'Lunch',
      counterparty: 'From 1111 111 111',
    });
  });

  it('treats cash deposits and foreign remittances as money in', () => {
    const deposit = {
      ...base,
      type: 'CASH_DEPOSIT' as const,
      fromAccountNumber: 'Cash Deposit Machine',
    };
    const remittance = {
      ...base,
      type: 'FOREIGN_REMITTANCE' as const,
      fromAccountNumber: 'Foreign Remittance',
      description: 'Foreign remittance from John',
    };
    expect(describeTransaction(deposit, '2222222222')).toMatchObject({
      direction: 'in',
      counterparty: 'Cash deposit',
    });
    expect(describeTransaction(remittance, '2222222222')).toEqual({
      direction: 'in',
      title: 'Foreign remittance from John',
      counterparty: 'Foreign remittance',
    });
  });

  it('uses the counterparty as the title when there is no description', () => {
    expect(describeTransaction({ ...base, description: '  ' }, '1111111111').title).toBe(
      'To 2222 222 222',
    );
    expect(describeTransaction({ ...base, description: null }, '2222222222').title).toBe(
      'From 1111 111 111',
    );
  });
});
