import {
  Avatar,
  Box,
  Button,
  Divider,
  IconButton,
  Link,
  Menu,
  Stack,
  Typography,
} from '@mui/material';
import IconifyIcon from 'components/base/IconifyIcon';
import { useAuth } from 'providers/AuthProvider';
import { MouseEvent, useState } from 'react';
import { Link as RouterLink } from 'react-router-dom';
import { Fragment } from 'react/jsx-runtime';

/* ------------------------Profile dropdown Data --------------------------- */
const profileData = [
  {
    href: '/profile',
    title: 'My Profile',
    subtitle: 'Account Settings',
    icon: 'fa:user-circle-o',
    color: 'primary.light',
  },
];
/* -------------------------------------------------------------------------- */
const initials = (name?: string): string =>
  (name ?? '')
    .split(/\s+/)
    .filter(Boolean)
    .slice(0, 2)
    .map((part) => part[0].toUpperCase())
    .join('');

const ProfileDropdown = () => {
  const [anchorEl, setAnchorEl] = useState<null | HTMLElement>(null);
  const { user, logout } = useAuth();

  const handleOpenDropdown = (event: MouseEvent<HTMLElement>) => {
    setAnchorEl(event.currentTarget);
  };
  const handleClose = () => {
    setAnchorEl(null);
  };
  const handleLogout = () => {
    handleClose();
    void logout(); // RequireAuth sends the user to the login page once signed out
  };
  return (
    <Fragment>
      <IconButton sx={{ p: 0, position: 'relative' }} onClick={handleOpenDropdown}>
        <Avatar
          alt={user?.fullName}
          sx={{
            width: { xs: 40, md: 45, xl: 60 },
            height: { xs: 40, md: 45, xl: 60 },
            bgcolor: 'primary.main',
          }}
        >
          {initials(user?.fullName)}
        </Avatar>
      </IconButton>
      {/* Profile Menu Dropdown*/}
      <Menu
        id="profile-menu"
        anchorEl={anchorEl}
        keepMounted
        open={Boolean(anchorEl)}
        onClose={handleClose}
        anchorOrigin={{ horizontal: 'right', vertical: 'bottom' }}
        transformOrigin={{ horizontal: 'right', vertical: 'top' }}
        sx={{
          '& .MuiMenu-paper': {
            width: 280,
            bgcolor: 'common.white',
          },
        }}
      >
        <Box p={3}>
          <Typography variant="subtitle1" color="text.primary">
            User Profile
          </Typography>
          <Stack direction="row" py={2.5} spacing={1.5} alignItems="center">
            <Avatar alt={user?.fullName} sx={{ width: 65, height: 65, bgcolor: 'primary.main' }}>
              {initials(user?.fullName)}
            </Avatar>
            <Box>
              <Typography variant="subtitle2" color="text.primary" fontWeight={600}>
                {user?.fullName}
              </Typography>
              <Typography variant="caption" color="textSecondary">
                {user?.role}
              </Typography>
              <Typography
                variant="subtitle2"
                color="textSecondary"
                display="flex"
                alignItems="center"
                gap={0.5}
              >
                <IconifyIcon icon="majesticons:mail-line" />
                {user?.email}
              </Typography>
            </Box>
          </Stack>
          <Divider />
          {profileData.map((profileItem) => (
            <Box key={profileItem.title} sx={{ py: 1.5, px: 0 }}>
              <Link component={RouterLink} to={profileItem.href} onClick={handleClose}>
                <Stack direction="row" spacing={1.5}>
                  <Stack
                    direction="row"
                    sx={{
                      width: 45,
                      height: 45,
                      bgcolor: 'neutral.light',
                      alignItems: 'center',
                      justifyContent: 'center',
                      borderRadius: 1.5,
                    }}
                  >
                    <Avatar
                      variant="rounded"
                      sx={{
                        minwidth: 24,
                        height: 24,
                        bgcolor: 'transparent',
                      }}
                    >
                      <IconifyIcon icon={profileItem.icon} color={profileItem.color} />
                    </Avatar>
                  </Stack>
                  <div>
                    <Typography
                      variant="subtitle2"
                      fontWeight={600}
                      noWrap
                      sx={{
                        width: 150,
                      }}
                    >
                      {profileItem.title}
                    </Typography>
                    <Typography
                      variant="subtitle2"
                      sx={{
                        width: 150,
                      }}
                      noWrap
                    >
                      {profileItem.subtitle}
                    </Typography>
                  </div>
                </Stack>
              </Link>
            </Box>
          ))}
          <Box mt={1.25}>
            <Button onClick={handleLogout} variant="outlined" color="error" fullWidth>
              Logout
            </Button>
          </Box>
        </Box>
      </Menu>
    </Fragment>
  );
};

export default ProfileDropdown;
