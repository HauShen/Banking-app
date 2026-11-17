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
