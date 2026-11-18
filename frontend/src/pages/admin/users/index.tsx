import { Alert, Button, Card, Snackbar, Stack, Typography } from '@mui/material';
import { GridPaginationModel } from '@mui/x-data-grid';
import { useUsers } from 'api/adminUsers';
import type { UserProfile } from 'api/types';
import CreateAdminDialog from 'components/sections/admin/users/CreateAdminDialog';
import UserDrawer from 'components/sections/admin/users/UserDrawer';
import UsersTable, { USER_PAGE_SIZES, UserSort } from 'components/sections/admin/users/UsersTable';
import { errorMessage } from 'lib/errors';
import { useAuth } from 'providers/AuthProvider';
import { useState } from 'react';

/** Admin: every user, page by page (GET /api/admin/users), with details, role change, delete, new admins. */
const AdminUsersPage = () => {
  const { user: me } = useAuth();
  const [pagination, setPagination] = useState<GridPaginationModel>({
    page: 0,
    pageSize: USER_PAGE_SIZES[1],
  });
  const [sort, setSort] = useState<UserSort>({ sortBy: 'USERNAME', direction: 'ASC' });
  const [selected, setSelected] = useState<UserProfile | null>(null);
  const [creating, setCreating] = useState(false);
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
        <Button variant="contained" onClick={() => setCreating(true)}>
          New administrator
        </Button>
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
        onChanged={(updated) => {
          setSelected(updated);
          setNotice(
            `${updated.username} is now ${updated.role === 'ADMIN' ? 'an administrator' : 'a customer'}.`,
          );
        }}
      />
      <CreateAdminDialog
        open={creating}
        onClose={() => setCreating(false)}
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
