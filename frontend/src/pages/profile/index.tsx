import { Card, Divider, Grid, Stack, Typography } from '@mui/material';
import ChangePasswordForm from 'components/sections/profile/ChangePasswordForm';
import ProfileForm from 'components/sections/profile/ProfileForm';
import { formatDate } from 'lib/dates';
import { useAuth } from 'providers/AuthProvider';
import { ReactNode } from 'react';

const ROLE_LABEL = { CUSTOMER: 'Customer', ADMIN: 'Administrator' } as const;

const Detail = ({ label, children }: { label: string; children: ReactNode }) => (
  <Stack direction="row" justifyContent="space-between" gap={2} sx={{ py: 1.25 }}>
    <Typography sx={{ color: 'text.secondary' }}>{label}</Typography>
    <Typography sx={{ fontWeight: 500, textAlign: 'right' }}>{children}</Typography>
  </Stack>
);

const ProfilePage = () => {
  const { user } = useAuth();
  if (!user) return null; // RequireAuth renders this page only for a signed-in user

  return (
    <Grid container spacing={3} sx={{ mb: 3 }}>
      <Grid item xs={12} md={7}>
        <Card sx={{ p: { xs: 3, md: 4 } }}>
          <Typography variant="h5" sx={{ mb: 3 }}>
            Personal details
          </Typography>
          <ProfileForm key={user.id} user={user} />
        </Card>
        <Card sx={{ p: { xs: 3, md: 4 }, mt: 3 }}>
          <Typography variant="h5" sx={{ mb: 1 }}>
            Password
          </Typography>
          <Typography variant="body2" sx={{ color: 'text.secondary', mb: 3 }}>
            Changing it signs you out on every other device.
          </Typography>
          <ChangePasswordForm key={user.id} />
        </Card>
      </Grid>
      <Grid item xs={12} md={5}>
        <Card sx={{ p: { xs: 3, md: 4 } }}>
          <Typography variant="h5" sx={{ mb: 2 }}>
            Account
          </Typography>
          <Detail label="Username">{user.username}</Detail>
          <Divider />
          <Detail label="Role">{ROLE_LABEL[user.role]}</Detail>
          <Divider />
          <Detail label="Member since">{formatDate(user.createdAt)}</Detail>
          <Typography variant="body2" sx={{ color: 'text.secondary', mt: 2 }}>
            Your username can't be changed.
          </Typography>
        </Card>
      </Grid>
    </Grid>
  );
};

export default ProfilePage;
