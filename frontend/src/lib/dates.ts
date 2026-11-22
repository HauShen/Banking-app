/** Date helpers. The API sends instants as ISO-8601 UTC strings; these show them in the browser's time zone. */

const dateTimeFormatter = new Intl.DateTimeFormat('en-MY', {
  dateStyle: 'medium',
  timeStyle: 'short',
});
const dateFormatter = new Intl.DateTimeFormat('en-MY', { dateStyle: 'medium' });

const toDate = (value: string | Date | null | undefined): Date | null => {
  if (!value) return null;
  const date = value instanceof Date ? value : new Date(value);
  return Number.isNaN(date.getTime()) ? null : date;
};

/** "17 Nov 2026, 9:07 pm" */
export const formatDateTime = (value: string | Date | null | undefined): string => {
  const date = toDate(value);
  return date ? dateTimeFormatter.format(date) : '—';
};

/** "17 Nov 2026" */
export const formatDate = (value: string | Date | null | undefined): string => {
  const date = toDate(value);
  return date ? dateFormatter.format(date) : '—';
};

/**
 * Calendar days ("2026-11-20") are dates in the bank's time zone, not instants: format them as they are, in UTC,
 * so a browser in another time zone doesn't move them to the day before.
 */
const dayFormatter = new Intl.DateTimeFormat('en-MY', {
  day: 'numeric',
  month: 'short',
  timeZone: 'UTC',
});
const longDayFormatter = new Intl.DateTimeFormat('en-MY', { dateStyle: 'medium', timeZone: 'UTC' });

const parseDay = (day: string): Date | null => {
  const match = /^(\d{4})-(\d{2})-(\d{2})$/.exec(day);
  return match
    ? new Date(Date.UTC(Number(match[1]), Number(match[2]) - 1, Number(match[3])))
    : null;
};

/** "2026-11-20" → "20 Nov" (chart axis). */
export const formatDay = (day: string): string => {
  const date = parseDay(day);
  return date ? dayFormatter.format(date) : '—';
};

/** "2026-11-20" → "20 Nov 2026". */
export const formatDayLong = (day: string): string => {
  const date = parseDay(day);
  return date ? longDayFormatter.format(date) : '—';
};
