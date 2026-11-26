import { Alert, Box, Button, Divider, Stack, Typography } from '@mui/material';
import { type DemoLogin, useDemoLogins } from 'api/demo';
import { errorMessage } from 'lib/errors';
import { useAuth } from 'providers/AuthProvider';
import { useState } from 'react';

/**
 * "Try the demo" box under the login form. Shown only when the backend runs the live demo (GET /api/demo/logins
 * answers); anywhere else it renders nothing. One click signs in as the demo customer or admin. The passwords are
 * shown as well, so a visitor can open a second window as another customer (the tour in the README).
 */
const DemoLogins = () => {
  const { data: demo } = useDemoLogins();
  const { login } = useAuth();
  const [signingInAs, setSigningInAs] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  if (!demo) {
    return null;
  }

  const signInAs = async ({ username, password }: DemoLogin) => {
    setError(null);
    setSigningInAs(username);
    try {
      // On success AuthProvider marks the user signed in, and GuestOnly redirects.
      await login(username, password);
    } catch (err) {
      setError(
        errorMessage(err, {
          401: 'The demo login was refused. The demo data may be resetting; try again in a minute.',
        }),
      );
      setSigningInAs(null);
    }
  };

  const button = (who: DemoLogin, role: string) => (
    <Button
      fullWidth
      variant="outlined"
      disabled={signingInAs !== null}
      onClick={() => signInAs(who)}
    >
      {signingInAs === who.username ? 'Signing in…' : `Log in as ${who.username} (${role})`}
    </Button>
  );

  return (
    <Box sx={{ mt: 4 }}>
      <Divider sx={{ mb: 3 }}>
        <Typography variant="subtitle2">Try the demo</Typography>
      </Divider>
      {error && (
        <Alert severity="error" sx={{ mb: 2 }}>
          {error}
        </Alert>
      )}
      <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2}>
        {button(demo.customer, 'customer')}
        {button(demo.admin, 'admin')}
      </Stack>
      <Typography variant="body2" color="text.secondary" sx={{ mt: 2 }}>
        Customer password: <strong>{demo.customer.password}</strong>, also for{' '}
        {demo.otherCustomers.join(', ')}. Admin password: <strong>{demo.admin.password}</strong>.
      </Typography>
      <Typography variant="body2" color="text.secondary" sx={{ mt: 1 }}>
        A demo bank: no real money moves.
        {demo.resetTime &&
          ` Everything resets every night at ${demo.resetTime} (${demo.timeZone.replace('_', ' ')}).`}
      </Typography>
    </Box>
  );
};

export default DemoLogins;
