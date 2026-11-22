import {
  Alert,
  Button,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  Stack,
  TextField,
  TextFieldProps,
} from '@mui/material';
import { useCreateAdmin } from 'api/adminUsers';
import {
  PASSWORD_MIN,
  SignupErrors,
  SignupField,
  signupServerErrors,
  SignupValues,
  validateSignup,
} from 'components/sections/authentication/signup';
import { errorMessage } from 'lib/errors';
import { FormEvent, useState } from 'react';

const EMPTY: SignupValues = {
  fullName: '',
  username: '',
  email: '',
  password: '',
  confirmPassword: '',
};

interface CreateAdminDialogProps {
  open: boolean;
  onClose: () => void;
  onCreated: (username: string) => void;
}

/** POST /api/admin/users: a new administrator. Same field rules as sign-up. */
const CreateAdminDialog = ({ open, onClose, onCreated }: CreateAdminDialogProps) => {
  const [values, setValues] = useState<SignupValues>(EMPTY);
  const [errors, setErrors] = useState<SignupErrors>({});
  const [problem, setProblem] = useState<string | null>(null);
  const createAdmin = useCreateAdmin();

  const close = () => {
    if (createAdmin.isPending) return;
    setValues(EMPTY);
    setErrors({});
    setProblem(null);
    onClose();
  };

  const handleSubmit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setProblem(null);
    const clientErrors = validateSignup(values);
    if (Object.keys(clientErrors).length > 0) {
      setErrors(clientErrors);
      return;
    }
    const username = values.username.trim();
    createAdmin.mutate(
      {
        username,
        fullName: values.fullName.trim(),
        email: values.email.trim(),
        password: values.password,
      },
      {
        onSuccess: () => {
          setValues(EMPTY);
          onCreated(username);
          onClose();
        },
        onError: (error) => {
          const fieldErrors = signupServerErrors(error);
          if (fieldErrors) setErrors(fieldErrors);
          else setProblem(errorMessage(error));
        },
      },
    );
  };

  const field = (name: SignupField, label: string, extra: TextFieldProps = {}): TextFieldProps => ({
    label,
    value: values[name],
    onChange: (e) => {
      setValues((current) => ({ ...current, [name]: e.target.value }));
      setErrors((current) => ({ ...current, [name]: undefined }));
    },
    error: Boolean(errors[name]),
    helperText: errors[name],
    disabled: createAdmin.isPending,
    required: true,
    fullWidth: true,
    ...extra,
  });

  return (
    <Dialog open={open} onClose={close} fullWidth maxWidth="sm">
      <form noValidate onSubmit={handleSubmit}>
        <DialogTitle>New administrator</DialogTitle>
        <DialogContent>
          <Stack gap={2.5} sx={{ pt: 1 }}>
            <TextField {...field('fullName', 'Full name')} autoFocus />
            <TextField {...field('username', 'Username')} />
            <TextField {...field('email', 'Email address', { type: 'email' })} />
            <TextField
              {...field('password', 'Password', {
                type: 'password',
                autoComplete: 'new-password',
                helperText: errors.password ?? `At least ${PASSWORD_MIN} characters`,
              })}
            />
            <TextField
              {...field('confirmPassword', 'Confirm password', {
                type: 'password',
                autoComplete: 'new-password',
              })}
            />
            {problem && <Alert severity="error">{problem}</Alert>}
          </Stack>
        </DialogContent>
        <DialogActions sx={{ px: 3, pb: 2.5 }}>
          <Button onClick={close} disabled={createAdmin.isPending}>
            Cancel
          </Button>
          <Button type="submit" variant="contained" disabled={createAdmin.isPending}>
            {createAdmin.isPending ? 'Creating…' : 'Create administrator'}
          </Button>
        </DialogActions>
      </form>
    </Dialog>
  );
};

export default CreateAdminDialog;
