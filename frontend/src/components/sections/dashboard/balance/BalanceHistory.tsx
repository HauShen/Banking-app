import {
  Alert,
  Box,
  Button,
  Skeleton,
  Stack,
  ToggleButton,
  ToggleButtonGroup,
  Typography,
  useTheme,
} from '@mui/material';
import { LineChart } from '@mui/x-charts/LineChart';
import { type DailyBalance, useBalanceHistory } from 'api/balanceHistory';
import CardContainer from 'components/common/CardContainter';
import { formatDay, formatDayLong } from 'lib/dates';
import { errorMessage } from 'lib/errors';
import { formatMoney } from 'lib/money';
import { formatAccountNumber } from 'lib/transactions';
import { useState } from 'react';

export const RANGES = [7, 30, 90] as const;
type Range = (typeof RANGES)[number];

const CHART_HEIGHT = 260;

/** Highest and lowest closing balance: comparisons only, no arithmetic on money. */
const extremes = (days: DailyBalance[]) =>
  days.reduce(
    (acc, day) => ({
      highest: day.closingBalance > acc.highest.closingBalance ? day : acc.highest,
      lowest: day.closingBalance < acc.lowest.closingBalance ? day : acc.lowest,
    }),
    { highest: days[0], lowest: days[0] },
  );

/** The selected account's closing balance per day over the last 7, 30 or 90 days. */
const BalanceHistory = ({ accountNumber }: { accountNumber: string }) => {
  const theme = useTheme();
  const [range, setRange] = useState<Range>(30);
  const { data, isLoading, isPlaceholderData, error, refetch } = useBalanceHistory(
    accountNumber,
    range,
  );

  const rangePicker = (
    <ToggleButtonGroup
      size="small"
      exclusive
      value={range}
      onChange={(_, value: Range | null) => value && setRange(value)}
      aria-label="Period"
    >
      {RANGES.map((days) => (
        <ToggleButton key={days} value={days} sx={{ px: 1.5, py: 0.25 }}>
          {days} days
        </ToggleButton>
      ))}
    </ToggleButtonGroup>
  );

  const renderBody = () => {
    if (isLoading) {
      return (
        <Skeleton variant="rounded" height={CHART_HEIGHT} aria-label="Loading balance history" />
      );
    }
    if (error || !data) {
      return (
        <Alert
          severity="error"
          action={
            <Button color="inherit" size="small" onClick={() => void refetch()}>
              Retry
            </Button>
          }
        >
          {errorMessage(error, { 404: 'This account no longer exists.' })}
        </Alert>
      );
    }
    if (data.days.length === 0) {
      return (
        <Typography sx={{ color: 'text.secondary', py: 4 }}>No balance history yet.</Typography>
      );
    }

    const { highest, lowest } = extremes(data.days);
    const today = data.days[data.days.length - 1];
    return (
      <Stack gap={1} sx={{ opacity: isPlaceholderData ? 0.5 : 1, transition: 'opacity 0.2s' }}>
        <Stack direction="row" flexWrap="wrap" columnGap={3} rowGap={0.5}>
          <Typography sx={{ color: 'text.secondary', fontSize: 'body2.fontSize' }}>
            Today{' '}
            <Box component="strong" sx={{ color: 'text.primary' }}>
              {formatMoney(today.closingBalance, data.currency)}
            </Box>
          </Typography>
          <Typography sx={{ color: 'text.secondary', fontSize: 'body2.fontSize' }}>
            Highest {formatMoney(highest.closingBalance, data.currency)} on{' '}
            {formatDayLong(highest.date)}
          </Typography>
          <Typography sx={{ color: 'text.secondary', fontSize: 'body2.fontSize' }}>
            Lowest {formatMoney(lowest.closingBalance, data.currency)} on{' '}
            {formatDayLong(lowest.date)}
          </Typography>
        </Stack>
        {data.days.length === 1 && (
          <Typography sx={{ color: 'text.secondary', fontSize: 'caption.fontSize' }}>
            The account was opened today, so the line starts tomorrow.
          </Typography>
        )}
        <Box
          role="img"
          aria-label={`Closing balance of ${formatAccountNumber(accountNumber)} per day, ${formatDayLong(
            data.days[0].date,
          )} to ${formatDayLong(today.date)}`}
        >
          <LineChart
            height={CHART_HEIGHT}
            margin={{ top: 16, right: 16, bottom: 28, left: 88 }}
            dataset={data.days.map((day) => ({ ...day }))}
            xAxis={[
              {
                scaleType: 'point',
                dataKey: 'date',
                valueFormatter: (value: string, context) =>
                  context.location === 'tick' ? formatDay(value) : formatDayLong(value),
              },
            ]}
            yAxis={[{ valueFormatter: (value: number) => formatMoney(value, data.currency) }]}
            series={[
              {
                dataKey: 'closingBalance',
                label: 'Closing balance',
                area: true,
                // Fill down to the chart's bottom, not to RM 0: the axis then fits the balances, so changes show.
                baseline: 'min',
                color: theme.palette.primary.main,
                showMark: data.days.length <= 31,
                curve: 'linear',
                valueFormatter: (value) => formatMoney(value, data.currency),
              },
            ]}
            slotProps={{ legend: { hidden: true } }}
            grid={{ horizontal: true }}
            sx={{ '& .MuiAreaElement-root': { fillOpacity: 0.12 } }}
          />
        </Box>
      </Stack>
    );
  };

  return (
    <CardContainer title="Balance History" action={rangePicker}>
      <Box sx={{ pb: 2 }}>{renderBody()}</Box>
    </CardContainer>
  );
};

export default BalanceHistory;
