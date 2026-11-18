import { Box, Button, InputAdornment, Stack, TextField } from '@mui/material';
import type { Account } from 'api/types';
import AccountSelect from 'components/sections/transactions/AccountSelect';
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
  values: TransferValues;
  errors: TransferErrors;
  onChange: (field: TransferField, value: string) => void;
  onSubmit: () => void;
}

const TransferForm = ({ accounts, values, errors, onChange, onSubmit }: TransferFormProps) => {
  const handleSubmit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    onSubmit();
  };

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
        <TextField
          label="Amount"
          value={values.amount}
          onChange={(e) => onChange('amount', e.target.value)}
          error={Boolean(errors.amount)}
          helperText={errors.amount ?? `At least ${MINIMUM_BALANCE_TEXT} must stay in the account`}
          InputProps={{ startAdornment: <InputAdornment position="start">RM</InputAdornment> }}
          inputProps={{ inputMode: 'decimal', autoComplete: 'off' }}
          required
        />
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
