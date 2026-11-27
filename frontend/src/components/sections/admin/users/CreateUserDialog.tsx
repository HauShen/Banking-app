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
import { useCreateAdmin, useCreateCustomer } from 'api/adminUsers';
import type { Role } from 'api/types';
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

const TEXT: Record<Role, { title: string; submit: string; passwordHint: string }> = {
  CUSTOMER: {
    title: 'New customer',
    submit: 'Create customer',
    passwordHint: `At least ${PASSWORD_MIN} characters. Give it to the customer; they can change it on their Profile page.`,
  },
  ADMIN: {
    title: 'New administrator',
    submit: 'Create administrator',
    passwordHint: `At least ${PASSWORD_MIN} characters`,
  },
};

interface CreateUserDialogProps {
  /** CUSTOMER → POST /api/admin/users/customers, ADMIN → POST /api/admin/users. */
  role: Role;
  open: boolean;
  onClose: () => void;
  onCreated: (username: string) => void;
}

/**
 * An admin creates a customer or another administrator. Customers are only created here: there is no
 * public sign-up. Both use the same fields and rules; the backend sets the role from the endpoint.
 */
const CreateUserDialog = ({ role, open, onClose, onCreated }: CreateUserDialogProps) => {
  const [values, setValues] = useState<SignupValues>(EMPTY);
  const [errors, setErrors] = useState<SignupErrors>({});
  const [problem, setProblem] = useState<string | null>(null);
  const createAdmin = useCreateAdmin();
  const createCustomer = useCreateCustomer();
  const create = role === 'ADMIN' ? createAdmin : createCustomer;
  const text = TEXT[role];

  const close = () => {
    if (create.isPending) return;
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
    create.mutate(
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
    disabled: create.isPending,
    required: true,
    fullWidth: true,
    ...extra,
  });

  return (
    <Dialog open={open} onClose={close} fullWidth maxWidth="sm">
      <form noValidate onSubmit={handleSubmit}>
        <DialogTitle>{text.title}</DialogTitle>
        <DialogContent>
          <Stack gap={2.5} sx={{ pt: 1 }}>
            <TextField {...field('fullName', 'Full name')} autoFocus />
            <TextField {...field('username', 'Username')} />
            <TextField {...field('email', 'Email address', { type: 'email' })} />
            <TextField
              {...field('password', 'Password', {
                type: 'password',
                autoComplete: 'new-password',
                helperText: errors.password ?? text.passwordHint,
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
          <Button onClick={close} disabled={create.isPending}>
            Cancel
          </Button>
          <Button type="submit" variant="contained" disabled={create.isPending}>
            {create.isPending ? 'Creating…' : text.submit}
          </Button>
        </DialogActions>
      </form>
    </Dialog>
  );
};

export default CreateUserDialog;
