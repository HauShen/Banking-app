import {
  InputAdornment,
  MenuItem,
  Stack,
  TextField,
  ToggleButton,
  ToggleButtonGroup,
  Typography,
} from '@mui/material';
import { QuoteDirection, useQuote } from 'api/fx';
import { errorMessage } from 'lib/errors';
import { formatMoney, formatRate, normalizeAmountInput } from 'lib/money';
import { useDebouncedValue } from 'lib/useDebouncedValue';
import { useState } from 'react';

/** How much MYR a foreign amount is worth right now (GET /api/fx/quote). Informational only. */
const QuoteCalculator = ({ currencies }: { currencies: string[] }) => {
  const [currency, setCurrency] = useState(currencies[0] ?? 'USD');
  const [amount, setAmount] = useState('100');
  const [direction, setDirection] = useState<QuoteDirection>('PAY');
  const normalized = normalizeAmountInput(amount, currency);
  const request = useDebouncedValue(
    normalized ? { currency, amount: normalized, direction } : undefined,
  );
  const quote = useQuote(request);

  let result;
  if (!normalized) {
    result = <Typography sx={{ color: 'error.main' }}>Enter a valid {currency} amount.</Typography>;
  } else if (quote.error) {
    result = <Typography sx={{ color: 'error.main' }}>{errorMessage(quote.error)}</Typography>;
  } else if (quote.data) {
    result = (
      <>
        <Typography variant="h4">
          {formatMoney(quote.data.amount, quote.data.currency)} ={' '}
          {formatMoney(quote.data.myrAmount)}
        </Typography>
        <Typography sx={{ color: 'text.secondary' }}>
          1 {quote.data.currency} = RM {formatRate(quote.data.exchangeRate)} ·{' '}
          {direction === 'PAY' ? 'when you pay in it' : 'when you receive it'}
        </Typography>
      </>
    );
  } else {
    result = <Typography sx={{ color: 'text.secondary' }}>Calculating…</Typography>;
  }

  return (
    <Stack gap={3}>
      <ToggleButtonGroup
        exclusive
        value={direction}
        onChange={(_, value: QuoteDirection | null) => value && setDirection(value)}
        aria-label="Direction"
        size="small"
      >
        <ToggleButton value="PAY">I pay</ToggleButton>
        <ToggleButton value="RECEIVE">I receive</ToggleButton>
      </ToggleButtonGroup>
      <Stack direction="row" gap={2}>
        <TextField
          select
          label="Currency"
          value={currency}
          onChange={(e) => setCurrency(e.target.value)}
          sx={{ width: 130 }}
        >
          {currencies.map((code) => (
            <MenuItem key={code} value={code}>
              {code}
            </MenuItem>
          ))}
        </TextField>
        <TextField
          label="Amount"
          value={amount}
          onChange={(e) => setAmount(e.target.value)}
          InputProps={{
            startAdornment: <InputAdornment position="start">{currency}</InputAdornment>,
          }}
          inputProps={{ inputMode: 'decimal' }}
          fullWidth
        />
      </Stack>
      <div aria-live="polite">{result}</div>
    </Stack>
  );
};

export default QuoteCalculator;
