import { IconButton } from '@mui/material';
import IconifyIcon from 'components/base/IconifyIcon';
import { Link as RouterLink } from 'react-router-dom';
import paths from 'routes/path';

/** Gear button in the app bar: opens the profile page. */
const SettingsDropdown = () => {
  return (
    <IconButton
      component={RouterLink}
      to={paths.profile}
      aria-label="Profile settings"
      sx={{ bgcolor: 'background.paper' }}
    >
      <IconifyIcon
        icon="lucide:settings"
        color="text.secondary"
        sx={{ width: { xs: 18, md: 20, xl: 25 }, height: { xs: 18, md: 20, xl: 25 } }}
      />
    </IconButton>
  );
};

export default SettingsDropdown;
