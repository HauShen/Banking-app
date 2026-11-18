import { Grid } from '@mui/material';
import { useAccountSummary } from 'api/accounts';
import MyAccounts from 'components/sections/dashboard/accounts/MyAccounts';
import RecentTransactions from 'components/sections/dashboard/transactions/RecentTransactions';
import QuickTransfer from 'components/sections/dashboard/transfer/QuickTransfer';
import { useState } from 'react';

const Dashboard = () => {
  const { data: summary, isLoading, error, refetch } = useAccountSummary();
  const [chosenAccountNumber, setChosenAccountNumber] = useState<string>();

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
        />
      </Grid>
      {selected && (
        <Grid item xs={12} xl={4} zIndex={1}>
          <RecentTransactions accountNumber={selected.accountNumber} />
        </Grid>
      )}
      {selected && (
        <Grid item xs={12} xl={8} zIndex={1}>
          <QuickTransfer key={selected.accountNumber} account={selected} />
        </Grid>
      )}
    </Grid>
  );
};

export default Dashboard;
