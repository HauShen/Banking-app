import {
  Alert,
  Box,
  Button,
  Chip,
  Divider,
  Drawer,
  IconButton,
  Stack,
  Typography,
} from '@mui/material';
import { useChangeRole, useDeleteUser } from 'api/adminUsers';
import type { Role, UserProfile } from 'api/types';
import IconifyIcon from 'components/base/IconifyIcon';
import ConfirmDialog from 'components/common/ConfirmDialog';
import { formatDateTime } from 'lib/dates';
import { errorMessage } from 'lib/errors';
import { ReactNode, useState } from 'react';
import { Link as RouterLink } from 'react-router-dom';
import paths from 'routes/path';
import EditUserDialog from './EditUserDialog';
import { ROLE_LABEL } from './UsersTable';

interface UserDrawerProps {
  user: UserProfile | null;
  currentUserId: string | undefined;
  onClose: () => void;
  /** After a role change or an edit: the user as the backend returned it, and what changed. */
  onChanged: (user: UserProfile, message: string) => void;
}

type PendingAction = { kind: 'role'; role: Role } | { kind: 'delete' };

const Row = ({ label, children }: { label: string; children: ReactNode }) => (
  <Stack direction="row" justifyContent="space-between" gap={2} sx={{ py: 1 }}>
    <Typography sx={{ color: 'text.secondary' }}>{label}</Typography>
    <Typography component="div" sx={{ textAlign: 'right', wordBreak: 'break-all' }}>
      {children}
    </Typography>
  </Stack>
);

/** One user's details, with role change and delete (both confirmed first). */
const UserDrawer = ({ user, currentUserId, onClose, onChanged }: UserDrawerProps) => {
  const [pending, setPending] = useState<PendingAction>();
  const [problem, setProblem] = useState<string | null>(null);
  const [editing, setEditing] = useState(false);
  const changeRole = useChangeRole();
  const deleteUser = useDeleteUser();
  const isSelf = user?.id === currentUserId;

  const cancel = () => {
    setPending(undefined);
    setProblem(null);
  };

  const confirm = () => {
    if (!user || !pending) return;
    setProblem(null);
    const onError = (error: unknown) => setProblem(errorMessage(error));
    if (pending.kind === 'role') {
      changeRole.mutate(
        { id: user.id, role: pending.role },
        {
          onSuccess: (updated) => {
            setPending(undefined);
            onChanged(
              updated,
              `${updated.username} is now ${updated.role === 'ADMIN' ? 'an administrator' : 'a customer'}.`,
            );
          },
          onError,
        },
      );
    } else {
      deleteUser.mutate(user.id, {
        onSuccess: () => {
          setPending(undefined);
          onClose();
        },
        onError,
      });
    }
  };

  const newRole: Role = user?.role === 'ADMIN' ? 'CUSTOMER' : 'ADMIN';

  return (
    <Drawer anchor="right" open={Boolean(user)} onClose={onClose}>
      {user && (
        <Box sx={{ width: { xs: '100vw', sm: 420 }, p: 3 }} role="dialog" aria-label="User details">
          <Stack direction="row" justifyContent="space-between" alignItems="center">
            <Typography variant="h5">{user.fullName}</Typography>
            <IconButton aria-label="Close" onClick={onClose}>
              <IconifyIcon icon="mdi:close" />
            </IconButton>
          </Stack>
          <Box sx={{ my: 2 }}>
            <Chip
              label={ROLE_LABEL[user.role]}
              color={user.role === 'ADMIN' ? 'primary' : 'default'}
              size="small"
            />
          </Box>
          <Divider />
          <Row label="Username">{user.username}</Row>
          <Row label="Email">{user.email}</Row>
          <Row label="Joined">{formatDateTime(user.createdAt)}</Row>
          <Row label="Last updated">{formatDateTime(user.updatedAt)}</Row>
          <Row label="User ID">{user.id}</Row>
          <Divider sx={{ mb: 3 }} />
          <Button
            component={RouterLink}
            to={`${paths.adminAccounts}?user=${encodeURIComponent(user.id)}`}
            variant="contained"
            sx={{ mb: 2 }}
          >
            Accounts of this user
          </Button>
          <Button variant="outlined" onClick={() => setEditing(true)} sx={{ mb: 2, ml: 2 }}>
            Edit details
          </Button>

          {isSelf ? (
            <Alert severity="info">
              This is your own account. Another administrator has to change your role or delete it.
            </Alert>
          ) : (
            <Stack direction="row" gap={2} flexWrap="wrap">
              <Button
                variant="outlined"
                onClick={() => setPending({ kind: 'role', role: newRole })}
              >
                {newRole === 'ADMIN' ? 'Make administrator' : 'Remove admin rights'}
              </Button>
              <Button
                variant="outlined"
                color="error"
                onClick={() => setPending({ kind: 'delete' })}
              >
                Delete user
              </Button>
            </Stack>
          )}

          <EditUserDialog
            key={`${user.id}-${user.updatedAt}`}
            user={user}
            open={editing}
            onClose={() => setEditing(false)}
            onSaved={(updated) => onChanged(updated, `Saved the changes to ${updated.username}.`)}
          />
          <ConfirmDialog
            open={pending?.kind === 'role'}
            title={newRole === 'ADMIN' ? 'Make administrator?' : 'Remove admin rights?'}
            confirmLabel={newRole === 'ADMIN' ? 'Make administrator' : 'Remove admin rights'}
            busy={changeRole.isPending}
            error={problem}
            onConfirm={confirm}
            onCancel={cancel}
          >
            {newRole === 'ADMIN'
              ? `${user.username} will be able to manage all users and accounts.`
              : `${user.username} will become a customer and lose access to the admin area.`}
          </ConfirmDialog>
          <ConfirmDialog
            open={pending?.kind === 'delete'}
            title="Delete user?"
            confirmLabel="Delete"
            danger
            busy={deleteUser.isPending}
            error={problem}
            onConfirm={confirm}
            onCancel={cancel}
          >
            {`${user.username} (${user.fullName}) will be deleted. This can't be undone.`}
          </ConfirmDialog>
        </Box>
      )}
    </Drawer>
  );
};

export default UserDrawer;
