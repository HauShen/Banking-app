import { Alert, Box, Button, Stack, Typography } from '@mui/material';
import type { LockedQuote } from 'api/fx';
import { formatMoney, formatRate } from 'lib/money';

interface LockedRateProps {
  quote: LockedQuote | undefined;
  locking: boolean;
  secondsLeft: number;
  onRelock: () => void;
}

const asClock = (seconds: number) =>
  `${Math.floor(seconds / 60)}:${String(seconds % 60).padStart(2, '0')}`;

/** The rate held for this transfer, with the time left to confirm; "Get a new rate" once it has run out. */
const LockedRate = ({ quote, locking, secondsLeft, onRelock }: LockedRateProps) => {
  if (locking) {
    return <Alert severity="info">Locking the exchange rate…</Alert>;
  }
  if (!quote) {
    return (
      <Box>
        <Button variant="outlined" onClick={onRelock}>
          Get a rate
        </Button>
      </Box>
    );
  }
  if (secondsLeft === 0) {
    return (
      <Alert
        severity="warning"
        action={
          <Button color="inherit" size="small" onClick={onRelock}>
            Get a new rate
          </Button>
        }
      >
        The locked rate has expired.
      </Alert>
    );
  }
  return (
    <Alert severity="success" icon={false}>
      <Stack direction="row" justifyContent="space-between" gap={2} flexWrap="wrap">
        <Box>
          <Typography fontWeight={600}>
            Rate locked: 1 {quote.currency} = RM {formatRate(quote.exchangeRate)}
          </Typography>
          <Typography variant="body2">
            {formatMoney(quote.myrAmount)} will leave your account.
          </Typography>
        </Box>
        <Typography fontWeight={600} aria-label="Time left to confirm">
          {asClock(secondsLeft)} left
        </Typography>
      </Stack>
    </Alert>
  );
};

export default LockedRate;
