import { Alert, Box, Chip, Link, List, ListItem, Skeleton, Stack, Typography } from '@mui/material';
import { useAccountTransactions } from 'api/transactions';
import type { Transaction } from 'api/types';
import IconifyIcon from 'components/base/IconifyIcon';
import CardContainer from 'components/common/CardContainter';
import { formatDateTime } from 'lib/dates';
import { errorMessage } from 'lib/errors';
import { formatMoney, formatRate, formatSignedMoney } from 'lib/money';
import { describeTransaction } from 'lib/transactions';
import { Link as RouterLink } from 'react-router-dom';
import paths from 'routes/path';

const RECENT_COUNT = 5;

const ICONS: Record<Transaction['type'], { in: string; out: string; bg: string }> = {
  TRANSFER: { in: 'mdi:arrow-bottom-left', out: 'mdi:arrow-top-right', bg: 'neutral.light' },
  CASH_DEPOSIT: { in: 'mdi:cash-plus', out: 'mdi:cash-plus', bg: 'warning.light' },
  FOREIGN_REMITTANCE: { in: 'mdi:earth', out: 'mdi:earth', bg: 'success.lighter' },
};

const TransactionRow = ({
  transaction,
  accountNumber,
}: {
  transaction: Transaction;
  accountNumber: string;
}) => {
  const { direction, title, counterparty } = describeTransaction(transaction, accountNumber);
  const icon = ICONS[transaction.type];
  const foreign = transaction.originalCurrency && transaction.originalAmount !== null;

  return (
    <ListItem disablePadding sx={{ display: 'flex', alignItems: 'center', gap: 2.5 }}>
      <Stack
        sx={{
          width: 48,
          height: 48,
          flexShrink: 0,
          alignItems: 'center',
          justifyContent: 'center',
          bgcolor: icon.bg,
          borderRadius: '50%',
          color: 'primary.darker',
        }}
      >
        <IconifyIcon icon={icon[direction]} width={22} />
      </Stack>
      <Stack sx={{ flexGrow: 1, minWidth: 0 }} gap={0.5}>
        <Typography
          noWrap
          sx={{
            color: 'primary.darker',
            fontSize: { xs: 'subtitle1.fontSize', md: 'body2.fontSize' },
          }}
        >
          {title}
        </Typography>
        <Typography noWrap sx={{ color: 'primary.light', fontSize: 'caption.fontSize' }}>
          {title === counterparty ? '' : `${counterparty} · `}
          {formatDateTime(transaction.createdAt)}
        </Typography>
      </Stack>
      <Stack alignItems="flex-end" gap={0.5} sx={{ flexShrink: 0 }}>
        <Typography
          sx={{
            color: direction === 'in' ? 'success.main' : 'error.main',
            fontWeight: 600,
            fontSize: { xs: 'caption.fontSize', md: 'body1.fontSize' },
          }}
        >
          {formatSignedMoney(transaction.amount, direction, transaction.currency)}
        </Typography>
        {foreign && (
          <Typography sx={{ color: 'text.secondary', fontSize: 'caption.fontSize' }}>
            {formatMoney(transaction.originalAmount, transaction.originalCurrency ?? undefined)} @{' '}
            {formatRate(transaction.exchangeRate)}
          </Typography>
        )}
        {transaction.status !== 'SUCCESS' && (
          <Chip
            size="small"
            label={transaction.status === 'PENDING' ? 'Pending' : 'Failed'}
            color={transaction.status === 'PENDING' ? 'warning' : 'error'}
          />
        )}
      </Stack>
    </ListItem>
  );
};

interface RecentTransactionsProps {
  accountNumber: string;
}

/** The latest transactions of one account (the selected card). */
const RecentTransactions = ({ accountNumber }: RecentTransactionsProps) => {
  const { data, isLoading, error } = useAccountTransactions(accountNumber, 0, RECENT_COUNT);

  let body;
  if (isLoading) {
    body = (
      <Stack gap={2.5} aria-label="Loading transactions">
        {Array.from({ length: 3 }, (_, i) => (
          <Skeleton key={i} variant="rounded" height={48} />
        ))}
      </Stack>
    );
  } else if (error) {
    body = <Alert severity="error">{errorMessage(error)}</Alert>;
  } else if (!data || data.content.length === 0) {
    body = (
      <Box sx={{ py: 4, textAlign: 'center', color: 'text.secondary' }}>
        <Typography>No transactions yet</Typography>
      </Box>
    );
  } else {
    body = (
      <List disablePadding sx={{ '& > *:not(:last-child)': { mb: 2.5 } }}>
        {data.content.map((transaction) => (
          <TransactionRow
            key={transaction.reference}
            transaction={transaction}
            accountNumber={accountNumber}
          />
        ))}
      </List>
    );
  }

  return (
    <CardContainer
      title="Recent Transactions"
      action={
        <Link
          component={RouterLink}
          to={`${paths.transactions}?account=${accountNumber}`}
          sx={{ fontWeight: 600, fontSize: { xs: 'caption.fontSize', md: 'body1.fontSize' } }}
        >
          See all
        </Link>
      }
    >
      <Box sx={{ p: { xs: 0.5, xl: 1 }, pb: { xs: 2, xl: 3 } }}>{body}</Box>
    </CardContainer>
  );
};

export default RecentTransactions;
