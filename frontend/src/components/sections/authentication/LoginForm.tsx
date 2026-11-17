import { Alert, Box, Button, Grid, IconButton, InputAdornment, TextField } from '@mui/material';
import IconifyIcon from 'components/base/IconifyIcon';
import { errorMessage, formatWait } from 'lib/errors';
import { useAuth } from 'providers/AuthProvider';
import { useBreakpoints } from 'providers/useBreakpoints';
import { FormEvent, useState } from 'react';

const loginErrorMessage = (err: unknown): string =>
  errorMessage(err, {
    400: 'Please enter your username and password.',
    401: 'Invalid username or password.',
    429: (e) =>
      e.retryAfterSeconds
        ? `Too many failed attempts. Try again in ${formatWait(e.retryAfterSeconds)}.`
        : 'Too many failed attempts. Try again later.',
  });

const LoginForm = () => {
  const { login } = useAuth();
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const { up } = useBreakpoints();
  const upSM = up('sm');

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setError(null);
    setSubmitting(true);
    try {
      // On success AuthProvider marks the user signed in, and GuestOnly redirects.
      await login(username.trim(), password);
    } catch (err) {
      setError(loginErrorMessage(err));
      setSubmitting(false);
    }
  };

  return (
    <Box component="form" noValidate onSubmit={handleSubmit}>
      {error && (
        <Alert severity="error" sx={{ mb: 3 }}>
          {error}
        </Alert>
      )}
      <Grid container spacing={3} sx={{ mb: 5 }}>
        <Grid item xs={12}>
          <TextField
            fullWidth
            size={upSM ? 'medium' : 'small'}
            name="username"
            label="Username"
            autoComplete="username"
            autoFocus
            required
            value={username}
            onChange={(e) => setUsername(e.target.value)}
            disabled={submitting}
          />
        </Grid>
        <Grid item xs={12}>
          <TextField
            fullWidth
            size={upSM ? 'medium' : 'small'}
            name="password"
            label="Password"
            type={showPassword ? 'text' : 'password'}
            autoComplete="current-password"
            required
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            disabled={submitting}
            InputProps={{
              endAdornment: (
                <InputAdornment position="end">
                  <IconButton
                    aria-label={showPassword ? 'Hide password' : 'Show password'}
                    onClick={() => setShowPassword(!showPassword)}
                    edge="end"
                  >
                    <IconifyIcon icon={showPassword ? 'majesticons:eye' : 'majesticons:eye-off'} />
                  </IconButton>
                </InputAdornment>
              ),
            }}
          />
        </Grid>
      </Grid>
      <Button
        fullWidth
        size={upSM ? 'large' : 'medium'}
        type="submit"
        variant="contained"
        color="primary"
        disabled={submitting || !username.trim() || !password}
      >
        {submitting ? 'Signing in…' : 'Login'}
      </Button>
    </Box>
  );
};

export default LoginForm;
