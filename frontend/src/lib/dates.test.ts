import { describe, expect, it } from 'vitest';
import { formatDate, formatDateTime, formatDay, formatDayLong } from './dates';

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

describe('formatDay / formatDayLong', () => {
  it('formats a calendar day as it is, whatever the browser time zone', () => {
    expect(formatDay('2026-11-20')).toBe('20 Nov');
    expect(formatDayLong('2026-11-01')).toBe('1 Nov 2026');
  });

  it('shows a dash for anything that is not a YYYY-MM-DD day', () => {
    expect(formatDay('20/11/2026')).toBe('—');
    expect(formatDayLong('')).toBe('—');
  });
});
