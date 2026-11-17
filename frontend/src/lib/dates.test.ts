import { describe, expect, it } from 'vitest';
import { formatDate, formatDateTime } from './dates';

// Midday UTC: the same calendar day in every time zone from UTC−11 to UTC+11, so these pass on any machine.
const MIDDAY = '2026-11-17T12:00:00Z';

describe('formatDate', () => {
  it('shows day, short month and year', () => {
    expect(formatDate(MIDDAY)).toBe('17 Nov 2026');
  });
});

describe('formatDateTime', () => {
  it('adds the time of day', () => {
    const text = formatDateTime(MIDDAY);
    expect(text).toContain('17 Nov 2026');
    expect(text).toMatch(/\d{1,2}:\d{2}\s?[ap]m/i);
  });

  it('shows a dash for missing or invalid values', () => {
    expect(formatDateTime(null)).toBe('—');
    expect(formatDateTime('not a date')).toBe('—');
    expect(formatDate(undefined)).toBe('—');
  });
});
