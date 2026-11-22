import {
  Alert,
  Button,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  Stack,
  TextField,
  Typography,
} from '@mui/material';
import { useQueryClient } from '@tanstack/react-query';
import { useUpdateUser } from 'api/adminUsers';
import { queryKeys } from 'api/queryKeys';
import type { UserProfile } from 'api/types';
import {
  ProfileErrors,
  profileServerErrors,
  ProfileValues,
  validateProfile,
} from 'components/sections/profile/profileValidation';
import { errorMessage } from 'lib/errors';
import { useAuth } from 'providers/AuthProvider';
import { FormEvent, useState } from 'react';

interface EditUserDialogProps {
  user: UserProfile;
  open: boolean;
  onClose: () => void;
  onSaved: (user: UserProfile) => void;
}

/**
 * Edit a user's full name and email (same rules as the profile page). Another user's entry goes through
 * PUT /api/admin/users/{id} (audited); your own through PUT /api/users/me, so the signed-in name updates too.
 */
const EditUserDialog = ({ user, open, onClose, onSaved }: EditUserDialogProps) => {
  const { user: me, updateProfile } = useAuth();
  const queryClient = useQueryClient();
  const updateUser = useUpdateUser();
  const isSelf = user.id === me?.id;

  const [values, setValues] = useState<ProfileValues>({
    fullName: user.fullName,
    email: user.email,
  });
  const [errors, setErrors] = useState<ProfileErrors>({});
  const [problem, setProblem] = useState<string | null>(null);
  const [savingSelf, setSavingSelf] = useState(false);
  const saving = updateUser.isPending || savingSelf;

  const trimmed = { fullName: values.fullName.trim(), email: values.email.trim() };
  const unchanged = trimmed.fullName === user.fullName && trimmed.email === user.email;

  const close = () => {
    if (saving) return;
    setValues({ fullName: user.fullName, email: user.email });
    setErrors({});
    setProblem(null);
    onClose();
  };

  const fail = (error: unknown) => {
    const fieldErrors = profileServerErrors(error);
    if (fieldErrors) setErrors(fieldErrors);
    else setProblem(errorMessage(error));
  };

  const saved = (updated: UserProfile) => {
    setValues({ fullName: updated.fullName, email: updated.email });
    onSaved(updated);
    onClose();
  };

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setProblem(null);
    const clientErrors = validateProfile(values);
    if (Object.keys(clientErrors).length > 0) {
      setErrors(clientErrors);
      return;
    }
    if (isSelf) {
      setSavingSelf(true);
      try {
        const updated = await updateProfile(trimmed);
        void queryClient.invalidateQueries({ queryKey: queryKeys.admin.users.all });
        saved(updated);
      } catch (error) {
        fail(error);
      } finally {
        setSavingSelf(false);
      }
      return;
    }
    updateUser.mutate({ id: user.id, ...trimmed }, { onSuccess: saved, onError: fail });
  };

  const change = (field: keyof ProfileValues, value: string) => {
    setValues((current) => ({ ...current, [field]: value }));
    setErrors((current) => ({ ...current, [field]: undefined }));
  };

  return (
    <Dialog open={open} onClose={close} fullWidth maxWidth="xs">
      <form noValidate onSubmit={handleSubmit}>
        <DialogTitle>Edit {user.username}</DialogTitle>
        <DialogContent>
          <Stack gap={2.5} sx={{ pt: 1 }}>
            <TextField
              label="Full name"
              value={values.fullName}
              onChange={(e) => change('fullName', e.target.value)}
              error={Boolean(errors.fullName)}
              helperText={errors.fullName}
              disabled={saving}
              required
              autoFocus
            />
            <TextField
              label="Email address"
              type="email"
              value={values.email}
              onChange={(e) => change('email', e.target.value)}
              error={Boolean(errors.email)}
              helperText={errors.email}
              disabled={saving}
              required
            />
            <Typography variant="body2" sx={{ color: 'text.secondary' }}>
              {isSelf
                ? 'This is your own account: the change shows everywhere at once.'
                : 'The change is recorded in the audit log as an edit by you.'}{' '}
              The username can't be changed.
            </Typography>
            {problem && <Alert severity="error">{problem}</Alert>}
          </Stack>
        </DialogContent>
        <DialogActions sx={{ px: 3, pb: 2.5 }}>
          <Button onClick={close} disabled={saving}>
            Cancel
          </Button>
          <Button type="submit" variant="contained" disabled={saving || unchanged}>
            {saving ? 'Saving…' : 'Save changes'}
          </Button>
        </DialogActions>
      </form>
    </Dialog>
  );
};

export default EditUserDialog;
