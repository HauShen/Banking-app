import { describe, expect, it } from 'vitest';
import {
  currencyDecimals,
  formatMoney,
  formatRate,
  formatSignedMoney,
  normalizeAmountInput,
} from './money';

/** Intl puts a no-break space between "RM" and the number; compare with plain spaces. */
const plain = (text: string) => text.replace(/\s/g, ' ');

describe('formatMoney', () => {
  it('formats MYR by default, and other currencies with their own decimals', () => {
    expect(plain(formatMoney(1234.5))).toBe('RM 1,234.50');
    expect(plain(formatMoney('20'))).toBe('RM 20.00');
    expect(plain(formatMoney(100, 'USD'))).toBe('US$100.00');
    expect(plain(formatMoney(1000, 'JPY'))).toBe('JP¥1,000');
  });

  it('shows a dash for missing or invalid amounts', () => {
    expect(formatMoney(null)).toBe('—');
    expect(formatMoney(undefined)).toBe('—');
    expect(formatMoney('')).toBe('—');
    expect(formatMoney('abc')).toBe('—');
  });

  it('adds a sign for money in and out', () => {
    expect(plain(formatSignedMoney(50, 'in'))).toBe('+RM 50.00');
    expect(plain(formatSignedMoney(20, 'out'))).toBe('−RM 20.00');
  });
});

describe('formatRate', () => {
  it('shows at least 4 decimals', () => {
    expect(formatRate(4.6)).toBe('4.6000');
    expect(formatRate('0.0312')).toBe('0.0312');
    expect(formatRate(null)).toBe('—');
  });
});

describe('currencyDecimals', () => {
  it('knows that JPY has no decimals', () => {
    expect(currencyDecimals('MYR')).toBe(2);
    expect(currencyDecimals('USD')).toBe(2);
    expect(currencyDecimals('JPY')).toBe(0);
  });
});

describe('normalizeAmountInput', () => {
  it.each([
    ['100', '100.00'],
    ['1,234.5', '1234.50'],
    ['0.01', '0.01'],
    ['007.10', '7.10'],
    [' 25 ', '25.00'],
  ])('accepts %j as %j', (input, expected) => {
    expect(normalizeAmountInput(input)).toBe(expected);
  });

  it.each(['0', '0.00', '-5', '1.234', 'abc', '', '1e5', '12.', '123456789012345678'])(
    'rejects %j',
    (input) => {
      expect(normalizeAmountInput(input)).toBeNull();
    },
  );

  it('uses the decimals of the currency', () => {
    expect(normalizeAmountInput('1000', 'JPY')).toBe('1000');
    expect(normalizeAmountInput('10.5', 'JPY')).toBeNull();
    expect(normalizeAmountInput('10.5', 'USD')).toBe('10.50');
  });
});
