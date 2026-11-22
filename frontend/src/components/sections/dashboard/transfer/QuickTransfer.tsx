import { Box, Button, InputAdornment, Stack, TextField, Typography } from '@mui/material';
import type { Account } from 'api/types';
import CardContainer from 'components/common/CardContainter';
import { formatAccountNumber } from 'lib/transactions';
import type { TransferPrefill } from 'pages/transfer';
import { FormEvent, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import paths from 'routes/path';

/** Starts a transfer from the selected card; the transfer page checks the details. */
const QuickTransfer = ({ account }: { account: Account }) => {
  const navigate = useNavigate();
  const [toAccountNumber, setToAccountNumber] = useState('');
  const [amount, setAmount] = useState('');
  const locked = account.accountStatus === 'LOCKED';

  const handleSubmit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const prefill: TransferPrefill = {
      fromAccountNumber: account.accountNumber,
      toAccountNumber,
      amount,
    };
    navigate(paths.transfer, { state: prefill });
  };

  return (
    <CardContainer title="Quick Transfer">
      <Box component="form" noValidate onSubmit={handleSubmit} sx={{ pb: 3 }}>
        <Typography sx={{ color: 'text.secondary', mb: 2.5 }}>
          From {formatAccountNumber(account.accountNumber)}
        </Typography>
        {locked ? (
          <Typography sx={{ color: 'error.main' }}>
            This account is locked, so it can't send money.
          </Typography>
        ) : (
          <Stack direction={{ xs: 'column', md: 'row' }} gap={2} alignItems={{ md: 'flex-start' }}>
            <TextField
              label="To account number"
              value={toAccountNumber}
              onChange={(e) => setToAccountNumber(e.target.value)}
              inputProps={{ inputMode: 'numeric', autoComplete: 'off' }}
              sx={{ flex: 2 }}
            />
            <TextField
              label="Amount"
              value={amount}
              onChange={(e) => setAmount(e.target.value)}
              InputProps={{ startAdornment: <InputAdornment position="start">RM</InputAdornment> }}
              inputProps={{ inputMode: 'decimal', autoComplete: 'off' }}
              sx={{ flex: 1 }}
            />
            <Button type="submit" variant="contained" size="large" sx={{ height: 56 }}>
              Continue
            </Button>
          </Stack>
        )}
      </Box>
    </CardContainer>
  );
};

export default QuickTransfer;
