import {
  Box,
  Button,
  CircularProgress,
  Divider,
  List,
  ListItemButton,
  Popover,
  Stack,
  Typography,
} from '@mui/material';
import {
  useMarkAllNotificationsRead,
  useMarkNotificationRead,
  useRecentNotifications,
} from 'api/notifications';
import type { AppNotification, NotificationType } from 'api/types';
import IconifyIcon from 'components/base/IconifyIcon';
import { formatDateTime } from 'lib/dates';

interface NotificationDropdownProps {
  anchor: HTMLElement | null;
  /** From the bell's count: "Mark all as read" is disabled when nothing is unread. */
  unread: number;
  onClose: () => void;
}

const ICONS: Record<NotificationType, { icon: string; color: string }> = {
  MONEY_SENT: { icon: 'lucide:arrow-up-right', color: 'error.main' },
  MONEY_RECEIVED: { icon: 'lucide:arrow-down-left', color: 'success.main' },
  CASH_DEPOSITED: { icon: 'lucide:banknote', color: 'success.main' },
  REMITTANCE_RECEIVED: { icon: 'lucide:globe', color: 'success.main' },
};

/** Read by screen readers, invisible on screen. */
const visuallyHidden = {
  position: 'absolute',
  width: 1,
  height: 1,
  overflow: 'hidden',
  clip: 'rect(0 0 0 0)',
  whiteSpace: 'nowrap',
} as const;

const NotificationRow = ({
  notification,
  onOpen,
}: {
  notification: AppNotification;
  onOpen: (notification: AppNotification) => void;
}) => {
  const { icon, color } = ICONS[notification.type] ?? ICONS.MONEY_RECEIVED;
  return (
    <ListItemButton
      onClick={() => onOpen(notification)}
      sx={{
        gap: 1.5,
        alignItems: 'flex-start',
        py: 1.25,
        px: 2.5,
        bgcolor: notification.read ? 'transparent' : 'action.hover',
      }}
    >
      <IconifyIcon
        icon={icon}
        color={color}
        sx={{ width: 20, height: 20, mt: 0.25, flexShrink: 0 }}
      />
      <Stack sx={{ minWidth: 0, flex: 1 }}>
        <Typography variant="body2" sx={{ fontWeight: notification.read ? 400 : 600 }}>
          {!notification.read && (
            <Box component="span" sx={visuallyHidden}>
              Unread:{' '}
            </Box>
          )}
          {notification.message}
        </Typography>
        <Typography variant="caption" color="text.secondary">
          {formatDateTime(notification.createdAt)}
        </Typography>
      </Stack>
      {!notification.read && (
        <Box
          aria-hidden
          sx={{ width: 8, height: 8, borderRadius: '50%', bgcolor: 'primary.main', mt: 0.75 }}
        />
      )}
    </ListItemButton>
  );
};

/**
 * The bell's dropdown: the 10 latest notifications, newest first. Opening an unread one marks it as read; "Mark all
 * as read" does it for all of them. The list is loaded each time the dropdown opens.
 */
const NotificationDropdown = ({ anchor, unread, onClose }: NotificationDropdownProps) => {
  const open = Boolean(anchor);
  const { data, isPending, isError, refetch } = useRecentNotifications(open);
  const markRead = useMarkNotificationRead();
  const markAllRead = useMarkAllNotificationsRead();

  const openNotification = (notification: AppNotification) => {
    if (!notification.read) markRead.mutate(notification.id);
  };

  let content;
  if (isPending) {
    content = (
      <Stack alignItems="center" sx={{ py: 4 }}>
        <CircularProgress size={24} aria-label="Loading notifications" />
      </Stack>
    );
  } else if (isError) {
    content = (
      <Stack alignItems="center" spacing={1} sx={{ py: 4, px: 2.5 }}>
        <Typography variant="body2" color="text.secondary">
          Couldn&apos;t load your notifications.
        </Typography>
        <Button size="small" onClick={() => void refetch()}>
          Try again
        </Button>
      </Stack>
    );
  } else if (data.content.length === 0) {
    content = (
      <Stack alignItems="center" spacing={1} sx={{ py: 4, px: 2.5, color: 'text.secondary' }}>
        <IconifyIcon icon="majesticons:bell-line" sx={{ width: 32, height: 32 }} />
        <Typography variant="body2">No notifications yet</Typography>
      </Stack>
    );
  } else {
    content = (
      <List disablePadding aria-label="Latest notifications">
        {data.content.map((notification) => (
          <NotificationRow
            key={notification.id}
            notification={notification}
            onOpen={openNotification}
          />
        ))}
      </List>
    );
  }

  return (
    <Popover
      open={open}
      anchorEl={anchor}
      onClose={onClose}
      anchorOrigin={{ vertical: 'bottom', horizontal: 'right' }}
      transformOrigin={{ vertical: 'top', horizontal: 'right' }}
      slotProps={{
        paper: {
          sx: {
            mt: 1.5,
            ml: 0.75,
            width: 340,
            maxWidth: 'calc(100vw - 32px)',
            maxHeight: 440,
            bgcolor: 'common.white',
            borderRadius: '3%',
          },
        },
      }}
    >
      <Stack
        direction="row"
        alignItems="center"
        justifyContent="space-between"
        sx={{ py: 2, px: 2.5 }}
      >
        <Typography variant="subtitle1">Notifications</Typography>
        <Button
          size="small"
          onClick={() => markAllRead.mutate()}
          disabled={unread === 0 || markAllRead.isPending}
        >
          Mark all as read
        </Button>
      </Stack>
      <Divider sx={{ borderStyle: 'dashed' }} />
      {content}
    </Popover>
  );
};

export default NotificationDropdown;
