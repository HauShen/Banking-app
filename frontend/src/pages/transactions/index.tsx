import { Alert, Button, Card, Stack, Typography } from '@mui/material';
import { GridPaginationModel } from '@mui/x-data-grid';
import { useAccountSummary } from 'api/accounts';
import { useAccountTransactions } from 'api/transactions';
import type { Transaction } from 'api/types';
import AccountSelect from 'components/sections/transactions/AccountSelect';
import TransactionDetailsDrawer from 'components/sections/transactions/TransactionDetailsDrawer';
import TransactionsTable, {
  PAGE_SIZE_OPTIONS,
} from 'components/sections/transactions/TransactionsTable';
import { errorMessage } from 'lib/errors';
import { useState } from 'react';
import { useSearchParams } from 'react-router-dom';

const FIRST_PAGE: GridPaginationModel = { page: 0, pageSize: PAGE_SIZE_OPTIONS[0] };

/**
 * Full history of one account. The account is in the URL (?account=…), so the dashboard's
 * "See all" link opens the right one and a reload keeps it.
 */
const TransactionsPage = () => {
  const summaryQuery = useAccountSummary();
  const [searchParams, setSearchParams] = useSearchParams();
  const [pagination, setPagination] = useState<GridPaginationModel>(FIRST_PAGE);
  const [openTransaction, setOpenTransaction] = useState<Transaction | null>(null);

  const accounts = summaryQuery.data?.accounts ?? [];
  const requested = searchParams.get('account');
  const account = accounts.find((a) => a.accountNumber === requested) ?? accounts[0];

  const history = useAccountTransactions(
    account?.accountNumber,
    pagination.page,
    pagination.pageSize,
  );

  const selectAccount = (accountNumber: string) => {
    setSearchParams({ account: accountNumber }, { replace: true });
    setPagination((current) => ({ ...current, page: 0 }));
  };

  if (summaryQuery.error) {
    return (
      <Alert
        severity="error"
        action={
          <Button color="inherit" size="small" onClick={() => void summaryQuery.refetch()}>
            Try again
          </Button>
        }
      >
        {errorMessage(summaryQuery.error)}
      </Alert>
    );
  }

  if (!summaryQuery.isLoading && !account) {
    return (
      <Card sx={{ p: { xs: 3, md: 5 }, textAlign: 'center' }}>
        <Typography variant="h5" sx={{ mb: 1 }}>
          No account yet
        </Typography>
        <Typography sx={{ color: 'text.secondary' }}>
          Your transactions will show up here once you have an account.
        </Typography>
      </Card>
    );
  }

  return (
    <Stack gap={3} sx={{ mb: 3 }}>
      {account && (
        <AccountSelect accounts={accounts} value={account.accountNumber} onChange={selectAccount} />
      )}
      {history.error ? (
        <Alert severity="error">{errorMessage(history.error)}</Alert>
      ) : (
        <Card sx={{ p: { xs: 1, md: 2 } }}>
          <TransactionsTable
            accountNumber={account?.accountNumber ?? ''}
            data={history.data}
            loading={summaryQuery.isLoading || history.isFetching}
            paginationModel={pagination}
            onPaginationModelChange={setPagination}
            onRowClick={setOpenTransaction}
          />
        </Card>
      )}
      <TransactionDetailsDrawer
        transaction={openTransaction}
        accountNumber={account?.accountNumber ?? ''}
        onClose={() => setOpenTransaction(null)}
      />
    </Stack>
  );
};

export default TransactionsPage;
