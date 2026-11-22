import {
  Alert,
  Button,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  InputAdornment,
  TextField,
  Typography,
} from '@mui/material';
import { useTopUp } from 'api/accounts';
import { NetworkError } from 'api/client';
import type { Account } from 'api/types';
import { errorMessage } from 'lib/errors';
import { normalizeAmountInput } from 'lib/money';
import { formatAccountNumber } from 'lib/transactions';
import { FormEvent, useState } from 'react';

interface TopUpDialogProps {
  account: Account;
  open: boolean;
  onClose: () => void;
  /** Called with the deposited amount ("100.00") and the account as the backend returned it. */
  onDeposited: (amount: string, account: Account) => void;
}

/** Simulated cash deposit (the backend records it as a "Cash Deposit Machine" transaction). */
const TopUpDialog = ({ account, open, onClose, onDeposited }: TopUpDialogProps) => {
  const [amount, setAmount] = useState('');
  const [fieldError, setFieldError] = useState<string>();
  const [problem, setProblem] = useState<{ severity: 'error' | 'warning'; text: string }>();
  const topUp = useTopUp();

  const close = () => {
    if (topUp.isPending) return;
    setAmount('');
    setFieldError(undefined);
    setProblem(undefined);
    onClose();
  };

  const handleSubmit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setProblem(undefined);
    const normalized = normalizeAmountInput(amount);
    if (!normalized) {
      setFieldError('Enter an amount, e.g. 100 or 50.50.');
      return;
    }
    topUp.mutate(
      { accountId: account.accountId, amount: normalized },
      {
        onSuccess: (updated) => {
          onDeposited(normalized, updated);
          setAmount('');
          onClose();
        },
        onError: (error) => {
          if (error instanceof NetworkError) {
            // No idempotency key on this endpoint: the deposit may have gone through. Don't invite a blind retry.
            setProblem({
              severity: 'warning',
              text: "We couldn't confirm the deposit. Check your balance before trying again, so it isn't deposited twice.",
            });
          } else {
            setProblem({ severity: 'error', text: errorMessage(error) });
          }
        },
      },
    );
  };

  return (
    <Dialog open={open} onClose={close} fullWidth maxWidth="xs">
      <form noValidate onSubmit={handleSubmit}>
        <DialogTitle>Cash deposit</DialogTitle>
        <DialogContent>
          <Typography sx={{ color: 'text.secondary', mb: 2.5 }}>
            Into {formatAccountNumber(account.accountNumber)}. This simulates depositing cash at a
            machine (demo only).
          </Typography>
          <TextField
            label="Amount"
            value={amount}
            onChange={(e) => {
              setAmount(e.target.value);
              setFieldError(undefined);
            }}
            error={Boolean(fieldError)}
            helperText={fieldError}
            InputProps={{ startAdornment: <InputAdornment position="start">RM</InputAdornment> }}
            inputProps={{ inputMode: 'decimal', autoComplete: 'off' }}
            disabled={topUp.isPending}
            autoFocus
            fullWidth
          />
          {problem && (
            <Alert severity={problem.severity} sx={{ mt: 2 }}>
              {problem.text}
            </Alert>
          )}
        </DialogContent>
        <DialogActions sx={{ px: 3, pb: 2.5 }}>
          <Button onClick={close} disabled={topUp.isPending}>
            Cancel
          </Button>
          <Button type="submit" variant="contained" disabled={topUp.isPending}>
            {topUp.isPending ? 'Depositing…' : 'Deposit'}
          </Button>
        </DialogActions>
      </form>
    </Dialog>
  );
};

export default TopUpDialog;
