import { AppBar, IconButton, Link, Stack, Toolbar, Typography } from '@mui/material';
import IconifyIcon from 'components/base/IconifyIcon';
import Image from 'components/base/Image';
import NotificationBell from 'layouts/main-layout/appbar/NotificationBell';
import ProfileDropdown from 'layouts/main-layout/appbar/ProfileDropdown';
import SettingsDropdown from 'layouts/main-layout/appbar/SettingsDropdown';
import { useLocation, Link as RouterLink } from 'react-router-dom';
interface NavbarProps {
  onDrawerToggle: () => void;
}
const MainNavbar = ({ onDrawerToggle }: NavbarProps) => {
  const location = useLocation();

  // Extract the route name from the pathname
  const pathSegments = location.pathname.split('/').filter((segment) => segment.trim() !== '');
  const routeName = pathSegments.length > 0 ? pathSegments.pop() : 'Overview';

  return (
    <>
      <AppBar position="sticky" sx={{ bgcolor: 'common.white' }}>
        <Toolbar
          sx={{
            justifyContent: 'space-between',
            flexWrap: 'wrap',
            gap: { xs: 0, lg: 2 },
          }}
        >
          <Typography
            sx={{
              display: { xs: 'none', md: 'block' },
              fontSize: { sm: 'h2.fontSize', xl: 'h1.fontSize' },
              fontWeight: 600,
              color: 'primary.darker',
              flex: 1,
              textAlign: { xs: 'center', md: 'left' },
              textTransform: 'capitalize',
            }}
          >
            {routeName}
          </Typography>
          <Stack direction="row" gap={1} sx={{ display: { xs: 'flex', md: 'none' } }}>
            <Link component={RouterLink} to="/" sx={{ display: 'flex', p: 0.5 }}>
              <Image src="/bankdash.svg" alt="Logo" sx={{ width: 25 }} />
            </Link>
            <IconButton onClick={onDrawerToggle} sx={{ display: { md: 'none' } }}>
              <IconifyIcon icon="mingcute:menu-line" color="primary.darker" width={25} />
            </IconButton>
          </Stack>

          <Stack direction="row" sx={{ alignItems: 'center', gap: { xs: 2.5, xl: 3.75 } }}>
            <Stack direction="row" sx={{ gap: { xs: 2.5, xl: 3.75 } }}>
              <SettingsDropdown />
              <NotificationBell />
            </Stack>
            <ProfileDropdown />
          </Stack>
        </Toolbar>
      </AppBar>
    </>
  );
};

export default MainNavbar;
