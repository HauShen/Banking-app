import { Box, Card, Container, Stack, Typography } from '@mui/material';
import DemoLogins from 'components/sections/authentication/DemoLogins';
import LoginForm from 'components/sections/authentication/LoginForm';

const LoginPage = () => {
  return (
    <Box
      sx={{
        width: 1,
        position: 'relative',
        zIndex: 100,
      }}
    >
      <Stack alignItems="center" justifyContent="center" sx={{ height: 1 }}>
        <Container maxWidth="sm">
          <Card
            sx={{
              p: { xs: 3, sm: 5 },
              width: 1,
            }}
          >
            <Typography variant="h4">Sign In</Typography>

            <Typography
              variant="body2"
              sx={{
                mt: 2,
                mb: { xs: 3, sm: 5 },
                fontSize: { xs: 'subtitle1.fontSize', sm: 'body2.fontSize' },
              }}
            >
              {/* No sign-up: like at a bank's branch, an admin creates the customer and hands over the
                  first password (POST /api/admin/users/customers). */}
              Use the username and password the bank gave you.
            </Typography>

            <LoginForm />
            <DemoLogins />
          </Card>
        </Container>
      </Stack>
    </Box>
  );
};

export default LoginPage;
