import type { Transaction } from 'api/types';

export interface TransactionView {
  direction: 'in' | 'out';
  /** What the user wrote, or a label when there is no description. */
  title: string;
  /** The other side: "To 1234 567 890", "From …", "Cash deposit", "Foreign remittance". */
  counterparty: string;
}

/** 10-digit account numbers as "1234 567 890"; anything else unchanged. */
export const formatAccountNumber = (accountNumber: string): string =>
  /^\d{10}$/.test(accountNumber)
    ? `${accountNumber.slice(0, 4)} ${accountNumber.slice(4, 7)} ${accountNumber.slice(7)}`
    : accountNumber;

/**
 * How a transaction looks from one account's point of view. A transfer between two of the
 * user's own accounts is "out" on the sender's list and "in" on the receiver's.
 */
export const describeTransaction = (
  transaction: Transaction,
  viewingAccountNumber: string,
): TransactionView => {
  let direction: 'in' | 'out';
  let counterparty: string;

  if (transaction.type === 'CASH_DEPOSIT') {
    direction = 'in';
    counterparty = 'Cash deposit';
  } else if (transaction.type === 'FOREIGN_REMITTANCE') {
    direction = 'in';
    counterparty = 'Foreign remittance';
  } else if (transaction.toAccountNumber === viewingAccountNumber) {
    direction = 'in';
    counterparty = `From ${formatAccountNumber(transaction.fromAccountNumber)}`;
  } else {
    direction = 'out';
    counterparty = `To ${formatAccountNumber(transaction.toAccountNumber)}`;
  }

  const title = transaction.description?.trim() || counterparty;
  return { direction, title, counterparty };
};
