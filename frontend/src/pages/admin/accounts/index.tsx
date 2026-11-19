import {
  Alert,
  Button,
  Card,
  Link,
  Snackbar,
  Stack,
  ToggleButton,
  ToggleButtonGroup,
  Typography,
} from '@mui/material';
import { useAdminAccounts } from 'api/adminAccounts';
import { useUser, useUsers } from 'api/adminUsers';
import type { Account, AccountStatus, UserProfile } from 'api/types';
import AdminAccountDrawer from 'components/sections/admin/accounts/AdminAccountDrawer';
import AdminAccountsTable, {
  ACCOUNT_TYPE_LABEL,
} from 'components/sections/admin/accounts/AdminAccountsTable';
import OpenAccountDialog from 'components/sections/admin/accounts/OpenAccountDialog';
import { errorMessage } from 'lib/errors';
import { formatMoney } from 'lib/money';
import { formatAccountNumber } from 'lib/transactions';
import { useMemo, useState } from 'react';
import { Link as RouterLink, useSearchParams } from 'react-router-dom';
import paths from 'routes/path';

type StatusFilter = AccountStatus | 'ALL';

/**
 * Admin: every account (filter by status), or one user's accounts (?user=<id>, from the user panel),
 * where new accounts are opened.
 */
const AdminAccountsPage = () => {
  const [searchParams] = useSearchParams();
  const userId = searchParams.get('user') ?? undefined;
  const [statusFilter, setStatusFilter] = useState<StatusFilter>('ALL');
  const [selectedId, setSelectedId] = useState<number>();
  const [opening, setOpening] = useState(false);
  const [notice, setNotice] = useState<string>();

  const accounts = useAdminAccounts({
    userId,
    status: !userId && statusFilter !== 'ALL' ? statusFilter : undefined,
  });
  const filterUser = useUser(userId);
  // Owners by id, to show names in the table (the first 100 users by username; the backend caps a page at 100).
  const users = useUsers({ page: 0, size: 100, sortBy: 'USERNAME', direction: 'ASC' });
  const owners = useMemo(() => {
    const map = new Map<string, UserProfile>((users.data?.content ?? []).map((u) => [u.id, u]));
    if (filterUser.data) map.set(filterUser.data.id, filterUser.data);
    return map;
  }, [users.data, filterUser.data]);

  const list = accounts.data ?? [];
  // Look the selected account up in the current list, so the drawer shows fresh data after a change.
  const selected = list.find((a) => a.accountId === selectedId) ?? null;

  let heading;
  if (userId) {
    heading = (
      <Stack
        direction="row"
        justifyContent="space-between"
        alignItems="center"
        gap={2}
        flexWrap="wrap"
      >
        <Stack gap={0.5}>
          <Typography variant="h5">
            {filterUser.data
              ? `Accounts of ${filterUser.data.fullName} (${filterUser.data.username})`
              : 'Accounts of this user'}
          </Typography>
          <Link component={RouterLink} to={paths.adminAccounts} variant="body2">
            Show all accounts
          </Link>
        </Stack>
        <Button variant="contained" onClick={() => setOpening(true)} disabled={!filterUser.data}>
          Open account
        </Button>
      </Stack>
    );
  } else {
    heading = (
      <Stack
        direction="row"
        justifyContent="space-between"
        alignItems="center"
        gap={2}
        flexWrap="wrap"
      >
        <ToggleButtonGroup
          exclusive
          size="small"
          value={statusFilter}
          onChange={(_, value: StatusFilter | null) => value && setStatusFilter(value)}
          aria-label="Status"
        >
          <ToggleButton value="ALL">All</ToggleButton>
          <ToggleButton value="ACTIVE">Active</ToggleButton>
          <ToggleButton value="LOCKED">Locked</ToggleButton>
        </ToggleButtonGroup>
        <Typography variant="body2" sx={{ color: 'text.secondary' }}>
          To open an account, choose the customer on the users page.
        </Typography>
      </Stack>
    );
  }

  return (
    <Stack gap={3} sx={{ mb: 3 }}>
      {heading}
      {accounts.error ? (
        <Alert severity="error">{errorMessage(accounts.error)}</Alert>
      ) : (
        <Card sx={{ p: { xs: 1, md: 2 } }}>
          <AdminAccountsTable
            accounts={list}
            loading={accounts.isFetching}
            owners={owners}
            onRowClick={(account) => setSelectedId(account.accountId)}
          />
        </Card>
      )}
      <AdminAccountDrawer
        account={selected}
        owner={selected ? owners.get(selected.userId) : undefined}
        onClose={() => setSelectedId(undefined)}
        onChanged={(account: Account) =>
          setNotice(
            `Account ${formatAccountNumber(account.accountNumber)} is now ${
              account.accountStatus === 'LOCKED' ? 'locked' : 'active'
            }.`,
          )
        }
        onDeleted={(account: Account) => {
          setSelectedId(undefined);
          setNotice(`Account ${formatAccountNumber(account.accountNumber)} deleted.`);
        }}
      />
      {filterUser.data && (
        <OpenAccountDialog
          owner={filterUser.data}
          open={opening}
          onClose={() => setOpening(false)}
          onOpened={(account) =>
            setNotice(
              `${ACCOUNT_TYPE_LABEL[account.accountType]} account ${formatAccountNumber(
                account.accountNumber,
              )} opened with ${formatMoney(account.balance)}.`,
            )
          }
        />
      )}
      <Snackbar
        open={Boolean(notice)}
        autoHideDuration={5000}
        onClose={() => setNotice(undefined)}
        anchorOrigin={{ vertical: 'bottom', horizontal: 'center' }}
      >
        <Alert severity="success" variant="filled" onClose={() => setNotice(undefined)}>
          {notice}
        </Alert>
      </Snackbar>
    </Stack>
  );
};

export default AdminAccountsPage;
