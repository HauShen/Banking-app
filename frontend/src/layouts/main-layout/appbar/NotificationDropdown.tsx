import { Divider, Popover, Stack, Typography } from '@mui/material';
import IconifyIcon from 'components/base/IconifyIcon';

interface NotificationDropdownProps {
  onClose: () => void;
  open: null | HTMLElement;
}

/**
 * Placeholder until the backend sends notifications (planned with Kafka).
 * The template's sample notifications (loans, credit cards, ...) were removed: no backend for them.
 */
const NotificationDropdown = ({ open, onClose }: NotificationDropdownProps) => {
  return (
    <Popover
      open={Boolean(open)}
      anchorEl={open}
      onClose={onClose}
      anchorOrigin={{ vertical: 'bottom', horizontal: 'right' }}
      transformOrigin={{ vertical: 'top', horizontal: 'right' }}
      slotProps={{
        paper: {
          sx: { mt: 1.5, ml: 0.75, width: 300, bgcolor: 'common.white', borderRadius: '3%' },
        },
      }}
    >
      <Stack sx={{ py: 2, px: 2.5 }}>
        <Typography variant="subtitle1">Notifications</Typography>
      </Stack>
      <Divider sx={{ borderStyle: 'dashed' }} />
      <Stack alignItems="center" spacing={1} sx={{ py: 4, px: 2.5, color: 'text.secondary' }}>
        <IconifyIcon icon="majesticons:bell-line" sx={{ width: 32, height: 32 }} />
        <Typography variant="body2">No notifications yet</Typography>
      </Stack>
    </Popover>
  );
};

export default NotificationDropdown;
