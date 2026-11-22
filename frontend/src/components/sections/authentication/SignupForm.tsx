import {
  Alert,
  Box,
  Button,
  Grid,
  IconButton,
  InputAdornment,
  TextField,
  TextFieldProps,
} from '@mui/material';
import IconifyIcon from 'components/base/IconifyIcon';
import { errorMessage } from 'lib/errors';
import { useAuth } from 'providers/AuthProvider';
import { useBreakpoints } from 'providers/useBreakpoints';
import { ChangeEvent, FormEvent, useState } from 'react';
import {
  PASSWORD_MIN,
  SignupErrors,
  SignupField,
  signupServerErrors,
  SignupValues,
  USERNAME_MAX,
  USERNAME_MIN,
  validateSignup,
} from './signup';

const EMPTY: SignupValues = {
  fullName: '',
  username: '',
  email: '',
  password: '',
  confirmPassword: '',
};

const HINTS: Partial<Record<SignupField, string>> = {
  username: `${USERNAME_MIN}–${USERNAME_MAX} characters`,
  password: `At least ${PASSWORD_MIN} characters`,
};

const SignupForm = () => {
  const { register } = useAuth();
  const [values, setValues] = useState<SignupValues>(EMPTY);
  const [errors, setErrors] = useState<SignupErrors>({});
  const [formError, setFormError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [showPassword, setShowPassword] = useState(false);
  const { up } = useBreakpoints();
  const size = up('sm') ? 'medium' : 'small';

  const handleChange = (field: SignupField) => (event: ChangeEvent<HTMLInputElement>) => {
    setValues((current) => ({ ...current, [field]: event.target.value }));
    // Editing a field clears its error; the next submit checks it again.
    setErrors((current) => {
      if (!current[field]) return current;
      const next = { ...current };
      delete next[field];
      return next;
    });
  };

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setFormError(null);

    const clientErrors = validateSignup(values);
    if (Object.keys(clientErrors).length > 0) {
      setErrors(clientErrors);
      return;
    }

    setSubmitting(true);
    try {
      // On success AuthProvider signs the user in, and GuestOnly moves them on to the dashboard.
      await register({
        fullName: values.fullName.trim(),
        username: values.username.trim(),
        email: values.email.trim(),
        password: values.password,
      });
    } catch (err) {
      const fieldErrors = signupServerErrors(err);
      if (fieldErrors) setErrors(fieldErrors);
      else setFormError(errorMessage(err));
      setSubmitting(false);
    }
  };

  const fieldProps = (field: SignupField): TextFieldProps => ({
    name: field,
    value: values[field],
    onChange: handleChange(field),
    error: Boolean(errors[field]),
    helperText: errors[field] ?? HINTS[field],
    disabled: submitting,
    required: true,
    fullWidth: true,
    size,
  });

  const passwordToggle = (
    <InputAdornment position="end">
      <IconButton
        aria-label={showPassword ? 'Hide passwords' : 'Show passwords'}
        onClick={() => setShowPassword(!showPassword)}
        edge="end"
      >
        <IconifyIcon icon={showPassword ? 'majesticons:eye' : 'majesticons:eye-off'} />
      </IconButton>
    </InputAdornment>
  );

  return (
    <Box component="form" noValidate onSubmit={handleSubmit}>
      {formError && (
        <Alert severity="error" sx={{ mb: 3 }}>
          {formError}
        </Alert>
      )}
      <Grid container spacing={3} sx={{ mb: 5 }}>
        <Grid item xs={12}>
          <TextField {...fieldProps('fullName')} label="Full name" autoComplete="name" autoFocus />
        </Grid>
        <Grid item xs={12}>
          <TextField {...fieldProps('username')} label="Username" autoComplete="username" />
        </Grid>
        <Grid item xs={12}>
          <TextField
            {...fieldProps('email')}
            label="Email address"
            type="email"
            autoComplete="email"
          />
        </Grid>
        <Grid item xs={12}>
          <TextField
            {...fieldProps('password')}
            label="Password"
            type={showPassword ? 'text' : 'password'}
            autoComplete="new-password"
            InputProps={{ endAdornment: passwordToggle }}
          />
        </Grid>
        <Grid item xs={12}>
          <TextField
            {...fieldProps('confirmPassword')}
            label="Confirm password"
            type={showPassword ? 'text' : 'password'}
            autoComplete="new-password"
          />
        </Grid>
      </Grid>
      <Button
        fullWidth
        size={up('sm') ? 'large' : 'medium'}
        type="submit"
        variant="contained"
        color="primary"
        disabled={submitting}
      >
        {submitting ? 'Creating account…' : 'Sign Up'}
      </Button>
    </Box>
  );
};

export default SignupForm;
