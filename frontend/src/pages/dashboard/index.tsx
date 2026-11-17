import { Grid } from '@mui/material';
import MyCards from 'components/sections/dashboard/creditCards/MyCards';
import RecentTransactions from 'components/sections/dashboard/transactions/RecentTransaction';
import QuickTransfer from 'components/sections/dashboard/transfer/QuickTransfer';

const Dashboard = () => {
  return (
    <Grid container spacing={{ xs: 2.5, sm: 3 }} mb={3}>
      {/* ------------- Card section ---------------- */}
      <Grid item xs={12} xl={8} zIndex={1}>
        <MyCards />
      </Grid>
      <Grid item xs={12} xl={4} zIndex={1}>
        <RecentTransactions />
      </Grid>

      {/* ------------- Transfer section ---------------- */}
      <Grid item xs={12} lg={6} xxl={4} zIndex={1}>
        <QuickTransfer />
      </Grid>
    </Grid>
  );
};

export default Dashboard;
