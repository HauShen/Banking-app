import {
  Alert,
  Button,
  Card,
  Chip,
  Divider,
  Grid,
  Link,
  Skeleton,
  Stack,
  Typography,
} from '@mui/material';
import { GridPaginationModel } from '@mui/x-data-grid';
import { ApiError } from 'api/client';
import { useAccountByNumber } from 'api/adminAccounts';
import { useUser } from 'api/adminUsers';
import { useAccountTransactions } from 'api/transactions';
import type { Account, Transaction } from 'api/types';
import IconifyIcon from 'components/base/IconifyIcon';
import { ACCOUNT_TYPE_LABEL } from 'components/sections/admin/accounts/AdminAccountsTable';
import BalanceHistory from 'components/sections/dashboard/balance/BalanceHistory';
import TransactionDetailsDrawer from 'components/sections/transactions/TransactionDetailsDrawer';
import TransactionsTable, {
  PAGE_SIZE_OPTIONS,
} from 'components/sections/transactions/TransactionsTable';
import { formatDate } from 'lib/dates';
import { errorMessage } from 'lib/errors';
import { formatMoney } from 'lib/money';
import { formatAccountNumber } from 'lib/transactions';
import { ReactNode, useState } from 'react';
import { Link as RouterLink, useParams } from 'react-router-dom';
import paths from 'routes/path';

const FIRST_PAGE: GridPaginationModel = { page: 0, pageSize: PAGE_SIZE_OPTIONS[0] };

const Fact = ({ label, children }: { label: string; children: ReactNode }) => (
  <Stack gap={0.25}>
    <Typography variant="caption" sx={{ color: 'text.secondary' }}>
      {label}
    </Typography>
    <Typography component="div" sx={{ fontWeight: 500 }}>
      {children}
    </Typography>
  </Stack>
);

/** Who owns the account, from the admin user API (the account itself only has the owner's id). */
const OwnerName = ({ userId }: { userId: string }) => {
  const { data: owner, isLoading } = useUser(userId);
  if (isLoading) return <Skeleton width={140} />;
  return <>{owner ? `${owner.fullName} (${owner.username})` : userId}</>;
};

const AccountHeader = ({ account }: { account: Account }) => {
  const locked = account.accountStatus === 'LOCKED';
  return (
    <Card sx={{ p: { xs: 3, md: 4 } }}>
      <Stack direction="row" alignItems="center" gap={2} flexWrap="wrap" sx={{ mb: 2 }}>
        <Typography variant="h5">{formatAccountNumber(account.accountNumber)}</Typography>
        <Chip
          size="small"
          label={locked ? 'Locked' : 'Active'}
          color={locked ? 'error' : 'success'}
        />
      </Stack>
      <Divider sx={{ mb: 2 }} />
      <Grid container spacing={2}>
        <Grid item xs={12} sm={6} md={3}>
          <Fact label="Owner">
            <OwnerName userId={account.userId} />
          </Fact>
        </Grid>
        <Grid item xs={6} md={3}>
          <Fact label="Balance">{formatMoney(account.balance, account.accountCurrency)}</Fact>
        </Grid>
        <Grid item xs={6} md={3}>
          <Fact label="Type">{ACCOUNT_TYPE_LABEL[account.accountType]}</Fact>
        </Grid>
        <Grid item xs={6} md={3}>
          <Fact label="Opened">{formatDate(account.createdAt)}</Fact>
        </Grid>
      </Grid>
    </Card>
  );
};

/**
 * Admin only: any account's full transaction history and daily balances, read-only. The backend allows
 * admins to read every account's history; moving money stays with the owner.
 */
const AdminAccountHistoryPage = () => {
  const { accountNumber = '' } = useParams();
  const accountQuery = useAccountByNumber(accountNumber);
  const [pagination, setPagination] = useState<GridPaginationModel>(FIRST_PAGE);
  const [openTransaction, setOpenTransaction] = useState<Transaction | null>(null);
  const account = accountQuery.data;
  const history = useAccountTransactions(
    account?.accountNumber,
    pagination.page,
    pagination.pageSize,
  );

  const backLink = (
    <Link
      component={RouterLink}
      to={paths.adminAccounts}
      sx={{ display: 'inline-flex', alignItems: 'center', gap: 0.5 }}
    >
      <IconifyIcon icon="mdi:arrow-left" width={18} />
      All accounts
    </Link>
  );

  if (accountQuery.error) {
    const notFound = accountQuery.error instanceof ApiError && accountQuery.error.status === 404;
    return (
      <Stack gap={3} sx={{ mb: 3, '& > *': { flexShrink: 0 } }}>
        {backLink}
        <Alert
          severity={notFound ? 'warning' : 'error'}
          action={
            notFound ? undefined : (
              <Button color="inherit" size="small" onClick={() => void accountQuery.refetch()}>
                Try again
              </Button>
            )
          }
        >
          {notFound
            ? `There is no account ${formatAccountNumber(accountNumber)}.`
            : errorMessage(accountQuery.error)}
        </Alert>
      </Stack>
    );
  }

  // A grid like the dashboard's: each section keeps its natural height (in a flex column the cards would shrink).
  return (
    <Grid container spacing={3} sx={{ mb: 3 }}>
      <Grid item xs={12}>
        {backLink}
      </Grid>
      <Grid item xs={12}>
        {account ? (
          <AccountHeader account={account} />
        ) : (
          <Skeleton variant="rounded" height={150} aria-label="Loading account" />
        )}
      </Grid>
      {account && (
        <Grid item xs={12}>
          <BalanceHistory accountNumber={account.accountNumber} />
        </Grid>
      )}
      <Grid item xs={12}>
        <Typography
          sx={{ fontWeight: 600, fontSize: { xs: 'body2.fontSize', md: 'h6.fontSize' }, mb: 2 }}
        >
          Transactions
        </Typography>
        {history.error ? (
          <Alert severity="error">{errorMessage(history.error)}</Alert>
        ) : (
          <Card sx={{ p: { xs: 1, md: 2 } }}>
            <TransactionsTable
              accountNumber={account?.accountNumber ?? ''}
              data={history.data}
              loading={accountQuery.isLoading || history.isFetching}
              paginationModel={pagination}
              onPaginationModelChange={setPagination}
              onRowClick={setOpenTransaction}
            />
          </Card>
        )}
      </Grid>
      <TransactionDetailsDrawer
        transaction={openTransaction}
        accountNumber={account?.accountNumber ?? ''}
        onClose={() => setOpenTransaction(null)}
      />
    </Grid>
  );
};

export default AdminAccountHistoryPage;
