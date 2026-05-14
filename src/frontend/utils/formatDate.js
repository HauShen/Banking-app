export function formatDate(
  dateValue,
  locale = "en-MY",
  options = {}
) {
  if (!dateValue) return "-";

  const date = new Date(dateValue);
  if (Number.isNaN(date.getTime())) return "-";

  const defaultOptions = {
    year: "numeric",
    month: "short",
    day: "2-digit",
  };

  return new Intl.DateTimeFormat(locale, {
    ...defaultOptions,
    ...options,
  }).format(date);
}