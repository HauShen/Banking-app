import { Box, Card, Container, Link, Stack, Typography } from '@mui/material';
import DemoLogins from 'components/sections/authentication/DemoLogins';
import LoginForm from 'components/sections/authentication/LoginForm';
import { Link as RouterLink } from 'react-router-dom';
import paths from 'routes/path';

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
              Don’t have an account?
              <Link
                component={RouterLink}
                to={paths.signup}
                variant="subtitle2"
                sx={{ ml: 0.75, '&:hover': { color: 'primary.light' } }}
              >
                Create One Now!
              </Link>
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
