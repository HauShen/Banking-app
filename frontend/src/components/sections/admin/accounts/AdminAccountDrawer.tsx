import { Box, Button, Chip, Divider, Drawer, IconButton, Stack, Typography } from '@mui/material';
import { useChangeAccountStatus, useDeleteAccount } from 'api/adminAccounts';
import type { Account, UserProfile } from 'api/types';
import IconifyIcon from 'components/base/IconifyIcon';
import ConfirmDialog from 'components/common/ConfirmDialog';
import { formatDateTime } from 'lib/dates';
import { errorMessage } from 'lib/errors';
import { formatMoney } from 'lib/money';
import { formatAccountNumber } from 'lib/transactions';
import { ReactNode, useState } from 'react';
import { ACCOUNT_TYPE_LABEL } from './AdminAccountsTable';
import RemittanceDialog from './RemittanceDialog';
import type { RemittanceResult } from 'api/remittances';

interface AdminAccountDrawerProps {
  account: Account | null;
  owner: UserProfile | undefined;
  onClose: () => void;
  onChanged: (account: Account) => void;
  onDeleted: (account: Account) => void;
  onRemittance: (result: RemittanceResult) => void;
}

const Row = ({ label, children }: { label: string; children: ReactNode }) => (
  <Stack direction="row" justifyContent="space-between" gap={2} sx={{ py: 1 }}>
    <Typography sx={{ color: 'text.secondary' }}>{label}</Typography>
    <Typography component="div" sx={{ textAlign: 'right', wordBreak: 'break-all' }}>
      {children}
    </Typography>
  </Stack>
);

/** One account's details, with lock / unlock and delete (both confirmed first). */
const AdminAccountDrawer = ({
  account,
  owner,
  onClose,
  onChanged,
  onDeleted,
  onRemittance,
}: AdminAccountDrawerProps) => {
  const [pending, setPending] = useState<'status' | 'delete'>();
  const [problem, setProblem] = useState<string | null>(null);
  const [receiving, setReceiving] = useState(false);
  const changeStatus = useChangeAccountStatus();
  const deleteAccount = useDeleteAccount();
  const locked = account?.accountStatus === 'LOCKED';

  const cancel = () => {
    setPending(undefined);
    setProblem(null);
  };

  const confirm = () => {
    if (!account) return;
    setProblem(null);
    const onError = (error: unknown) => setProblem(errorMessage(error));
    if (pending === 'status') {
      changeStatus.mutate(
        { accountId: account.accountId, status: locked ? 'ACTIVE' : 'LOCKED' },
        {
          onSuccess: (updated) => {
            setPending(undefined);
            onChanged(updated);
          },
          onError,
        },
      );
    } else if (pending === 'delete') {
      deleteAccount.mutate(account.accountId, {
        onSuccess: () => {
          setPending(undefined);
          onDeleted(account);
        },
        onError,
      });
    }
  };

  return (
    <Drawer anchor="right" open={Boolean(account)} onClose={onClose}>
      {account && (
        <Box
          sx={{ width: { xs: '100vw', sm: 420 }, p: 3 }}
          role="dialog"
          aria-label="Account details"
        >
          <Stack direction="row" justifyContent="space-between" alignItems="center">
            <Typography variant="h5">{formatAccountNumber(account.accountNumber)}</Typography>
            <IconButton aria-label="Close" onClick={onClose}>
              <IconifyIcon icon="mdi:close" />
            </IconButton>
          </Stack>
          <Box sx={{ my: 2 }}>
            <Chip
              size="small"
              label={locked ? 'Locked' : 'Active'}
              color={locked ? 'error' : 'success'}
            />
          </Box>
          <Divider />
          <Row label="Balance">{formatMoney(account.balance, account.accountCurrency)}</Row>
          <Row label="Type">{ACCOUNT_TYPE_LABEL[account.accountType]}</Row>
          <Row label="Owner">
            {owner ? `${owner.fullName} (${owner.username})` : account.userId}
          </Row>
          <Row label="Opened">{formatDateTime(account.createdAt)}</Row>
          <Row label="Account ID">{account.accountId}</Row>
          <Divider sx={{ mb: 3 }} />
          <Button
            variant="contained"
            onClick={() => setReceiving(true)}
            disabled={locked}
            sx={{ mb: 2 }}
          >
            Receive remittance
          </Button>
          <Stack direction="row" gap={2} flexWrap="wrap">
            <Button variant="outlined" onClick={() => setPending('status')}>
              {locked ? 'Unlock account' : 'Lock account'}
            </Button>
            <Button variant="outlined" color="error" onClick={() => setPending('delete')}>
              Delete account
            </Button>
          </Stack>
          <Typography variant="body2" sx={{ color: 'text.secondary', mt: 2 }}>
            Only accounts without transactions can be deleted. Lock an account to stop all money
            moving in or out (locked accounts can't receive remittances).
          </Typography>

          <RemittanceDialog
            key={account.accountId}
            account={account}
            open={receiving}
            onClose={() => setReceiving(false)}
            onReceived={onRemittance}
          />
          <ConfirmDialog
            open={pending === 'status'}
            title={locked ? 'Unlock account?' : 'Lock account?'}
            confirmLabel={locked ? 'Unlock' : 'Lock'}
            danger={!locked}
            busy={changeStatus.isPending}
            error={problem}
            onConfirm={confirm}
            onCancel={cancel}
          >
            {locked
              ? 'Transfers, deposits and remittances will work again for this account.'
              : 'No money can be sent from, deposited into or received by this account until it is unlocked.'}
          </ConfirmDialog>
          <ConfirmDialog
            open={pending === 'delete'}
            title="Delete account?"
            confirmLabel="Delete"
            danger
            busy={deleteAccount.isPending}
            error={problem}
            onConfirm={confirm}
            onCancel={cancel}
          >
            {`Account ${formatAccountNumber(account.accountNumber)} will be deleted. This can't be undone.`}
          </ConfirmDialog>
        </Box>
      )}
    </Drawer>
  );
};

export default AdminAccountDrawer;
