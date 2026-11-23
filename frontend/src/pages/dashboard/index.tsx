import { Alert, Grid, Snackbar } from '@mui/material';
import { useAccountSummary } from 'api/accounts';
import MyAccounts from 'components/sections/dashboard/accounts/MyAccounts';
import BalanceHistory from 'components/sections/dashboard/balance/BalanceHistory';
import TopUpDialog from 'components/sections/dashboard/accounts/TopUpDialog';
import RecentTransactions from 'components/sections/dashboard/transactions/RecentTransactions';
import QuickTransfer from 'components/sections/dashboard/transfer/QuickTransfer';
import { formatMoney } from 'lib/money';
import { formatAccountNumber } from 'lib/transactions';
import { useAuth } from 'providers/AuthProvider';
import { useState } from 'react';

const Dashboard = () => {
  const { data: summary, isLoading, error, refetch } = useAccountSummary();
  const [chosenAccountNumber, setChosenAccountNumber] = useState<string>();
  const [topUpOpen, setTopUpOpen] = useState(false);
  const [deposited, setDeposited] = useState<string>();
  // Only customers move money (transfers, cash deposits); the backend answers 403 to anyone else.
  const { user } = useAuth();
  const isCustomer = user?.role === 'CUSTOMER';

  // The chosen card, or the first account until the user picks one (or if the chosen one disappeared).
  const accounts = summary?.accounts ?? [];
  const selected =
    accounts.find((account) => account.accountNumber === chosenAccountNumber) ?? accounts[0];

  return (
    <Grid container spacing={{ xs: 2.5, sm: 3 }} mb={3}>
      <Grid item xs={12} xl={selected ? 8 : 12} zIndex={1}>
        <MyAccounts
          summary={summary}
          isLoading={isLoading}
          error={error}
          onRetry={() => void refetch()}
          selectedAccountNumber={selected?.accountNumber}
          onSelect={setChosenAccountNumber}
          onTopUp={isCustomer ? () => setTopUpOpen(true) : undefined}
        />
      </Grid>
      {selected && (
        <Grid item xs={12} xl={4} zIndex={1}>
          <RecentTransactions accountNumber={selected.accountNumber} showSeeAll={isCustomer} />
        </Grid>
      )}
      {selected && (
        <Grid item xs={12} zIndex={1}>
          <BalanceHistory accountNumber={selected.accountNumber} />
        </Grid>
      )}
      {selected && isCustomer && (
        <Grid item xs={12} xl={8} zIndex={1}>
          <QuickTransfer key={selected.accountNumber} account={selected} />
        </Grid>
      )}
      {selected && isCustomer && (
        <TopUpDialog
          key={selected.accountNumber}
          account={selected}
          open={topUpOpen}
          onClose={() => setTopUpOpen(false)}
          onDeposited={(amount, account) =>
            setDeposited(
              `${formatMoney(amount)} deposited into ${formatAccountNumber(account.accountNumber)}. ` +
                `New balance: ${formatMoney(account.balance)}.`,
            )
          }
        />
      )}
      <Snackbar
        open={Boolean(deposited)}
        autoHideDuration={6000}
        onClose={() => setDeposited(undefined)}
        anchorOrigin={{ vertical: 'bottom', horizontal: 'center' }}
      >
        <Alert severity="success" variant="filled" onClose={() => setDeposited(undefined)}>
          {deposited}
        </Alert>
      </Snackbar>
    </Grid>
  );
};

export default Dashboard;
