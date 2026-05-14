export function formatCurrency(
  value,
  locale = "en-MY",
  currency = "MYR"
) {
  const amount = Number(value ?? 0);

  return new Intl.NumberFormat(locale, {
    style: "currency",
    currency,
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  }).format(amount);
}