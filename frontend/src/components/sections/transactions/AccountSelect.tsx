import { MenuItem, TextField } from '@mui/material';
import type { Account } from 'api/types';
import { formatMoney } from 'lib/money';
import { formatAccountNumber } from 'lib/transactions';

const TYPE_LABEL: Record<Account['accountType'], string> = {
  SAVINGS: 'Savings',
  CURRENT: 'Current',
};

interface AccountSelectProps {
  accounts: Account[];
  value: string;
  onChange: (accountNumber: string) => void;
  label?: string;
}

/** Picks one of the user's accounts: "Savings · 1234 567 890 · RM 1,500.50". */
const AccountSelect = ({ accounts, value, onChange, label = 'Account' }: AccountSelectProps) => (
  <TextField
    select
    label={label}
    value={value}
    onChange={(event) => onChange(event.target.value)}
    sx={{ minWidth: { xs: 1, sm: 360 } }}
  >
    {accounts.map((account) => (
      <MenuItem key={account.accountNumber} value={account.accountNumber}>
        {TYPE_LABEL[account.accountType]} · {formatAccountNumber(account.accountNumber)} ·{' '}
        {formatMoney(account.balance, account.accountCurrency)}
        {account.accountStatus === 'LOCKED' ? ' · Locked' : ''}
      </MenuItem>
    ))}
  </TextField>
);

export default AccountSelect;
