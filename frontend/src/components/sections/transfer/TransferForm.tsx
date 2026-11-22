import { Box, Button, InputAdornment, MenuItem, Stack, TextField, Typography } from '@mui/material';
import { useQuote } from 'api/fx';
import type { Account } from 'api/types';
import AccountSelect from 'components/sections/transactions/AccountSelect';
import { errorMessage } from 'lib/errors';
import { DEFAULT_CURRENCY, formatMoney, formatRate, normalizeAmountInput } from 'lib/money';
import { useDebouncedValue } from 'lib/useDebouncedValue';
import { FormEvent } from 'react';
import {
  DESCRIPTION_MAX,
  MINIMUM_BALANCE_TEXT,
  TransferErrors,
  TransferField,
  TransferValues,
} from './transferValidation';

interface TransferFormProps {
  accounts: Account[];
  /** MYR first, then the currencies the bank offers. */
  currencies: string[];
  values: TransferValues;
  errors: TransferErrors;
  onChange: (field: TransferField, value: string) => void;
  onSubmit: () => void;
}

/** "≈ RM 423.50 · 1 USD = RM 4.2350" while the user types a foreign amount (informational). */
const IndicativeRate = ({ currency, amount }: { currency: string; amount: string }) => {
  const normalized = useDebouncedValue(normalizeAmountInput(amount, currency));
  const quote = useQuote(normalized ? { currency, amount: normalized } : undefined);

  if (!normalized) return null;
  let text: string;
  if (quote.isLoading) text = 'Getting the rate…';
  else if (quote.error) text = errorMessage(quote.error);
  else if (quote.data) {
    text = `≈ ${formatMoney(quote.data.myrAmount)} from your account · 1 ${currency} = RM ${formatRate(
      quote.data.exchangeRate,
    )}. The rate is locked on the next step.`;
  } else return null;

  return (
    <Typography
      variant="body2"
      sx={{ color: quote.error ? 'error.main' : 'text.secondary', mt: -1.5 }}
    >
      {text}
    </Typography>
  );
};

const TransferForm = ({
  accounts,
  currencies,
  values,
  errors,
  onChange,
  onSubmit,
}: TransferFormProps) => {
  const handleSubmit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    onSubmit();
  };
  const foreign = values.currency !== DEFAULT_CURRENCY;

  return (
    <Box component="form" noValidate onSubmit={handleSubmit}>
      <Stack gap={3}>
        <AccountSelect
          label="From"
          accounts={accounts}
          value={values.fromAccountNumber}
          onChange={(value) => onChange('fromAccountNumber', value)}
        />
        <TextField
          label="To account number"
          value={values.toAccountNumber}
          onChange={(e) => onChange('toAccountNumber', e.target.value)}
          error={Boolean(errors.toAccountNumber)}
          helperText={errors.toAccountNumber ?? '10 digits, e.g. 1234 567 890'}
          inputProps={{ inputMode: 'numeric', autoComplete: 'off' }}
          required
        />
        <Stack direction="row" gap={2} alignItems="flex-start">
          <TextField
            select
            label="Currency"
            value={values.currency}
            onChange={(e) => onChange('currency', e.target.value)}
            error={Boolean(errors.currency)}
            helperText={errors.currency}
            sx={{ width: 130, flexShrink: 0 }}
          >
            {currencies.map((code) => (
              <MenuItem key={code} value={code}>
                {code}
              </MenuItem>
            ))}
          </TextField>
          <TextField
            label="Amount"
            value={values.amount}
            onChange={(e) => onChange('amount', e.target.value)}
            error={Boolean(errors.amount)}
            helperText={
              errors.amount ??
              (foreign
                ? `Amount in ${values.currency}; the MYR equivalent leaves your account`
                : `At least ${MINIMUM_BALANCE_TEXT} must stay in the account`)
            }
            InputProps={{
              startAdornment: (
                <InputAdornment position="start">{foreign ? values.currency : 'RM'}</InputAdornment>
              ),
            }}
            inputProps={{ inputMode: 'decimal', autoComplete: 'off' }}
            required
            fullWidth
          />
        </Stack>
        {foreign && !errors.amount && (
          <IndicativeRate currency={values.currency} amount={values.amount} />
        )}
        <TextField
          label="Description"
          value={values.description}
          onChange={(e) => onChange('description', e.target.value)}
          error={Boolean(errors.description)}
          helperText={
            errors.description ?? `Shown in both histories (max ${DESCRIPTION_MAX} characters)`
          }
          inputProps={{ maxLength: DESCRIPTION_MAX }}
          required
        />
        <Box>
          <Button type="submit" variant="contained" size="large">
            Review transfer
          </Button>
        </Box>
      </Stack>
    </Box>
  );
};

export default TransferForm;
