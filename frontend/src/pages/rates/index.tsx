import { Alert, Card, Grid, Skeleton, Typography } from '@mui/material';
import { useRates } from 'api/fx';
import QuoteCalculator from 'components/sections/rates/QuoteCalculator';
import RatesTable from 'components/sections/rates/RatesTable';
import { errorMessage } from 'lib/errors';

const RatesPage = () => {
  const rates = useRates();

  if (rates.error) return <Alert severity="error">{errorMessage(rates.error)}</Alert>;

  return (
    <Grid container spacing={3} sx={{ mb: 3 }}>
      <Grid item xs={12} lg={7}>
        <Card sx={{ p: { xs: 2, md: 3 } }}>
          <Typography variant="h5" sx={{ mb: 2 }}>
            Exchange rates
          </Typography>
          {rates.isLoading ? (
            <Skeleton variant="rounded" height={220} />
          ) : (
            <RatesTable rates={rates.data ?? []} />
          )}
        </Card>
      </Grid>
      <Grid item xs={12} lg={5}>
        <Card sx={{ p: { xs: 2, md: 3 } }}>
          <Typography variant="h5" sx={{ mb: 2 }}>
            Currency calculator
          </Typography>
          {rates.data && rates.data.length > 0 ? (
            <QuoteCalculator currencies={rates.data.map((rate) => rate.currency)} />
          ) : (
            <Skeleton variant="rounded" height={160} />
          )}
        </Card>
      </Grid>
    </Grid>
  );
};

export default RatesPage;
