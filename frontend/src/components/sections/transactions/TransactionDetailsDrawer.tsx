import { Box, Chip, Divider, Drawer, IconButton, Stack, Typography } from '@mui/material';
import type { Transaction } from 'api/types';
import IconifyIcon from 'components/base/IconifyIcon';
import { formatDateTime } from 'lib/dates';
import { formatMoney, formatRate, formatSignedMoney } from 'lib/money';
import {
  describeTransaction,
  formatAccountNumber,
  TRANSACTION_STATUS_LABEL,
  TRANSACTION_TYPE_LABEL,
} from 'lib/transactions';
import { ReactNode } from 'react';

interface TransactionDetailsDrawerProps {
  transaction: Transaction | null;
  /** The account whose history is shown: decides money in vs out. */
  accountNumber: string;
  onClose: () => void;
}

const Row = ({ label, children }: { label: string; children: ReactNode }) => (
  <Stack direction="row" justifyContent="space-between" gap={2} sx={{ py: 1 }}>
    <Typography sx={{ color: 'text.secondary' }}>{label}</Typography>
    <Typography component="div" sx={{ textAlign: 'right', wordBreak: 'break-all' }}>
      {children}
    </Typography>
  </Stack>
);

/** Everything the API returns for one transaction. Uses the row's data: GET /{reference} returns the same fields. */
const TransactionDetailsDrawer = ({
  transaction,
  accountNumber,
  onClose,
}: TransactionDetailsDrawerProps) => {
  const view = transaction ? describeTransaction(transaction, accountNumber) : null;

  return (
    <Drawer anchor="right" open={Boolean(transaction)} onClose={onClose}>
      {transaction && view && (
        <Box
          sx={{ width: { xs: '100vw', sm: 420 }, p: 3 }}
          role="dialog"
          aria-label="Transaction details"
        >
          <Stack direction="row" justifyContent="space-between" alignItems="center">
            <Typography variant="h5">Transaction details</Typography>
            <IconButton aria-label="Close" onClick={onClose}>
              <IconifyIcon icon="mdi:close" />
            </IconButton>
          </Stack>

          <Box sx={{ my: 3, textAlign: 'center' }}>
            <Typography
              variant="h3"
              sx={{ color: view.direction === 'in' ? 'success.main' : 'error.main' }}
            >
              {formatSignedMoney(transaction.amount, view.direction, transaction.currency)}
            </Typography>
            <Typography sx={{ color: 'text.secondary', mt: 1 }}>{view.title}</Typography>
          </Box>

          <Divider />
          <Row label="Status">
            <Chip
              size="small"
              label={TRANSACTION_STATUS_LABEL[transaction.status]}
              color={
                transaction.status === 'SUCCESS'
                  ? 'success'
                  : transaction.status === 'PENDING'
                    ? 'warning'
                    : 'error'
              }
            />
          </Row>
          <Row label="Type">{TRANSACTION_TYPE_LABEL[transaction.type]}</Row>
          <Row label="From">{formatAccountNumber(transaction.fromAccountNumber)}</Row>
          <Row label="To">{formatAccountNumber(transaction.toAccountNumber)}</Row>
          {transaction.description && <Row label="Description">{transaction.description}</Row>}
          <Row label="Created">{formatDateTime(transaction.createdAt)}</Row>
          {transaction.postedAt && <Row label="Posted">{formatDateTime(transaction.postedAt)}</Row>}
          {transaction.originalCurrency && transaction.originalAmount !== null && (
            <>
              <Divider sx={{ my: 1 }} />
              <Row label="Original amount">
                {formatMoney(transaction.originalAmount, transaction.originalCurrency)}
              </Row>
              <Row label="Exchange rate">
                1 {transaction.originalCurrency} = RM {formatRate(transaction.exchangeRate)}
              </Row>
            </>
          )}
          <Divider sx={{ my: 1 }} />
          <Row label="Reference">{transaction.reference}</Row>
        </Box>
      )}
    </Drawer>
  );
};

export default TransactionDetailsDrawer;
