/**
 * Money helpers. Rule: never do arithmetic on amounts in the browser. The backend uses BigDecimal;
 * here amounts are only formatted for display, and user input is validated and sent on as a string.
 */

export const DEFAULT_CURRENCY = 'MYR';

const formatters = new Map<string, Intl.NumberFormat>();

const formatterFor = (currency: string): Intl.NumberFormat => {
  let formatter = formatters.get(currency);
  if (!formatter) {
    formatter = new Intl.NumberFormat('en-MY', { style: 'currency', currency });
    formatters.set(currency, formatter);
  }
  return formatter;
};

/** Decimals a currency uses: MYR, USD, SGD, EUR 2; JPY 0. */
export const currencyDecimals = (currency: string = DEFAULT_CURRENCY): number =>
  formatterFor(currency).resolvedOptions().maximumFractionDigits ?? 2;

/**
 * Display an amount from the API (BigDecimal arrives as a JSON number or string): "RM 1,234.50".
 * Display only: never use the result, or the parsed number, for calculations.
 */
export const formatMoney = (
  amount: number | string | null | undefined,
  currency: string = DEFAULT_CURRENCY,
): string => {
  if (amount === null || amount === undefined || amount === '') return '—';
  const value = typeof amount === 'number' ? amount : Number(amount);
  if (!Number.isFinite(value)) return '—';
  return formatterFor(currency).format(value);
};

/** "+RM 50.00" for money in, "−RM 20.00" for money out (transaction lists). */
export const formatSignedMoney = (
  amount: number | string | null | undefined,
  direction: 'in' | 'out',
  currency: string = DEFAULT_CURRENCY,
): string => `${direction === 'in' ? '+' : '−'}${formatMoney(amount, currency)}`;

const rateFormatter = new Intl.NumberFormat('en-MY', {
  minimumFractionDigits: 4,
  maximumFractionDigits: 8,
});

/** Exchange rate in MYR per 1 unit of foreign currency: 4.6 → "4.6000". */
export const formatRate = (rate: number | string | null | undefined): string => {
  if (rate === null || rate === undefined || rate === '') return '—';
  const value = typeof rate === 'number' ? rate : Number(rate);
  return Number.isFinite(value) ? rateFormatter.format(value) : '—';
};

/** Backend limit: @Digits(integer = 17) on every amount. */
const MAX_WHOLE_DIGITS = 17;

/**
 * Validate what the user typed in an amount field. Returns the string the API expects
 * ("1234.50"; "1000" for JPY), or null when it isn't a positive amount with the currency's decimals.
 * Works on the digits as text, so no float rounding can creep in.
 */
export const normalizeAmountInput = (
  input: string,
  currency: string = DEFAULT_CURRENCY,
): string | null => {
  const cleaned = input.trim().replace(/,/g, '');
  const decimals = currencyDecimals(currency);
  const pattern = decimals === 0 ? /^\d+$/ : new RegExp(`^\\d+(\\.\\d{1,${decimals}})?$`);
  if (!pattern.test(cleaned)) return null;

  const [wholeRaw, fraction = ''] = cleaned.split('.');
  const whole = wholeRaw.replace(/^0+(?=\d)/, '');
  if (/^0*$/.test(whole + fraction)) return null; // zero
  if (whole.length > MAX_WHOLE_DIGITS) return null;

  return decimals === 0 ? whole : `${whole}.${fraction.padEnd(decimals, '0')}`;
};
