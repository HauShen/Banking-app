import {
  Alert,
  Button,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  MenuItem,
  TextField,
  Typography,
} from '@mui/material';
import { useOpenAccount } from 'api/adminAccounts';
import type { Account, AccountType, UserProfile } from 'api/types';
import { errorMessage } from 'lib/errors';
import { FormEvent, useState } from 'react';

interface OpenAccountDialogProps {
  owner: UserProfile;
  open: boolean;
  onClose: () => void;
  onOpened: (account: Account) => void;
}

/** POST /api/accounts/create for one user: a new MYR account, ACTIVE, with the opening balance. */
const OpenAccountDialog = ({ owner, open, onClose, onOpened }: OpenAccountDialogProps) => {
  const [accountType, setAccountType] = useState<AccountType>('SAVINGS');
  const [problem, setProblem] = useState<string | null>(null);
  const openAccount = useOpenAccount();

  const close = () => {
    if (openAccount.isPending) return;
    setProblem(null);
    onClose();
  };

  const handleSubmit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setProblem(null);
    openAccount.mutate(
      { userId: owner.id, accountType },
      {
        onSuccess: (account) => {
          onOpened(account);
          onClose();
        },
        onError: (error) => setProblem(errorMessage(error)),
      },
    );
  };

  return (
    <Dialog open={open} onClose={close} fullWidth maxWidth="xs">
      <form noValidate onSubmit={handleSubmit}>
        <DialogTitle>Open account</DialogTitle>
        <DialogContent>
          <Typography sx={{ color: 'text.secondary', mb: 2.5 }}>
            For {owner.fullName} ({owner.username}). New accounts are in MYR and start with the
            opening balance of RM 20.00.
          </Typography>
          <TextField
            select
            label="Account type"
            value={accountType}
            onChange={(e) => setAccountType(e.target.value as AccountType)}
            disabled={openAccount.isPending}
            fullWidth
          >
            <MenuItem value="SAVINGS">Savings</MenuItem>
            <MenuItem value="CURRENT">Current</MenuItem>
          </TextField>
          {problem && (
            <Alert severity="error" sx={{ mt: 2 }}>
              {problem}
            </Alert>
          )}
        </DialogContent>
        <DialogActions sx={{ px: 3, pb: 2.5 }}>
          <Button onClick={close} disabled={openAccount.isPending}>
            Cancel
          </Button>
          <Button type="submit" variant="contained" disabled={openAccount.isPending}>
            {openAccount.isPending ? 'Opening…' : 'Open account'}
          </Button>
        </DialogActions>
      </form>
    </Dialog>
  );
};

export default OpenAccountDialog;
