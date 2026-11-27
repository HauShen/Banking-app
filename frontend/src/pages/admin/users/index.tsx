import { Alert, Button, Card, Snackbar, Stack, Typography } from '@mui/material';
import { GridPaginationModel } from '@mui/x-data-grid';
import { useUsers } from 'api/adminUsers';
import type { Role, UserProfile } from 'api/types';
import CreateUserDialog from 'components/sections/admin/users/CreateUserDialog';
import UserDrawer from 'components/sections/admin/users/UserDrawer';
import UsersTable, { USER_PAGE_SIZES, UserSort } from 'components/sections/admin/users/UsersTable';
import { errorMessage } from 'lib/errors';
import { useAuth } from 'providers/AuthProvider';
import { useState } from 'react';

/**
 * Admin: every user, page by page (GET /api/admin/users), with details, role change, delete, and new
 * customers (the only way a customer is created: no public sign-up) and new admins.
 */
const AdminUsersPage = () => {
  const { user: me } = useAuth();
  const [pagination, setPagination] = useState<GridPaginationModel>({
    page: 0,
    pageSize: USER_PAGE_SIZES[1],
  });
  const [sort, setSort] = useState<UserSort>({ sortBy: 'USERNAME', direction: 'ASC' });
  const [selected, setSelected] = useState<UserProfile | null>(null);
  const [creating, setCreating] = useState<Role | null>(null);
  const [notice, setNotice] = useState<string>();

  const users = useUsers({
    page: pagination.page,
    size: pagination.pageSize,
    sortBy: sort.sortBy,
    direction: sort.direction,
  });

  return (
    <Stack gap={3} sx={{ mb: 3 }}>
      <Stack direction="row" justifyContent="space-between" alignItems="center" gap={2}>
        <Typography sx={{ color: 'text.secondary' }}>
          {users.data ? `${users.data.totalElements} users` : ' '}
        </Typography>
        <Stack direction="row" gap={1.5}>
          <Button variant="outlined" onClick={() => setCreating('ADMIN')}>
            New administrator
          </Button>
          <Button variant="contained" onClick={() => setCreating('CUSTOMER')}>
            New customer
          </Button>
        </Stack>
      </Stack>
      {users.error ? (
        <Alert severity="error">{errorMessage(users.error)}</Alert>
      ) : (
        <Card sx={{ p: { xs: 1, md: 2 } }}>
          <UsersTable
            data={users.data}
            loading={users.isFetching}
            currentUserId={me?.id}
            pagination={pagination}
            onPaginationChange={setPagination}
            sort={sort}
            onSortChange={(next) => {
              setSort(next);
              setPagination((current) => ({ ...current, page: 0 }));
            }}
            onRowClick={setSelected}
          />
        </Card>
      )}
      <UserDrawer
        user={selected}
        currentUserId={me?.id}
        onClose={() => setSelected(null)}
        onChanged={(updated, message) => {
          setSelected(updated);
          setNotice(message);
        }}
      />
      {/* One dialog per role, so the title doesn't change while a dialog's closing animation runs. */}
      <CreateUserDialog
        role="CUSTOMER"
        open={creating === 'CUSTOMER'}
        onClose={() => setCreating(null)}
        onCreated={(username) =>
          setNotice(
            `Customer ${username} created. To open an account, click them in the list, then "Accounts of this user".`,
          )
        }
      />
      <CreateUserDialog
        role="ADMIN"
        open={creating === 'ADMIN'}
        onClose={() => setCreating(null)}
        onCreated={(username) => setNotice(`Administrator ${username} created.`)}
      />
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

export default AdminUsersPage;
