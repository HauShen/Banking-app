import { Badge, IconButton } from '@mui/material';
import { useQueryClient } from '@tanstack/react-query';
import { NOTIFICATION_POLL_MS, useUnreadNotificationCount } from 'api/notifications';
import { queryKeys } from 'api/queryKeys';
import IconifyIcon from 'components/base/IconifyIcon';
import NotificationDropdown from 'layouts/main-layout/appbar/NotificationDropdown';
import { MouseEvent, useEffect, useRef, useState } from 'react';

interface NotificationBellProps {
  /** How often to ask for the unread count; only tests change it. */
  pollMs?: number;
}

/**
 * A new notification means money just moved in one of the user's accounts — possibly sent by someone else, so this
 * browser knows nothing about it yet. When the unread count goes up, re-fetch balances and histories too, and the
 * dashboard shows the new balance without a reload.
 */
const useRefreshOnNewNotifications = (unread: number | undefined) => {
  const queryClient = useQueryClient();
  const previous = useRef<number | undefined>(undefined);

  useEffect(() => {
    if (unread === undefined) return;
    if (previous.current !== undefined && unread > previous.current) {
      void queryClient.invalidateQueries({ queryKey: queryKeys.accounts.all });
      void queryClient.invalidateQueries({ queryKey: queryKeys.transactions.all });
      void queryClient.invalidateQueries({ queryKey: ['notifications', 'recent'] });
    }
    previous.current = unread;
  }, [unread, queryClient]);
};

/** The bell in the app bar: unread count as a badge, the latest notifications in a dropdown. */
const NotificationBell = ({ pollMs = NOTIFICATION_POLL_MS }: NotificationBellProps) => {
  const [anchor, setAnchor] = useState<HTMLElement | null>(null);
  const { data: unreadCount } = useUnreadNotificationCount(pollMs);
  useRefreshOnNewNotifications(unreadCount);
  const unread = unreadCount ?? 0;

  return (
    <>
      <IconButton
        aria-label={unread > 0 ? `Notifications, ${unread} unread` : 'Notifications'}
        aria-haspopup="true"
        sx={{ bgcolor: 'background.paper' }}
        onClick={(event: MouseEvent<HTMLElement>) => setAnchor(event.currentTarget)}
      >
        <Badge color="error" badgeContent={unread} max={99}>
          <IconifyIcon
            icon="lucide:bell"
            color="text.secondary"
            sx={{ width: { xs: 18, md: 20, xl: 25 }, height: { xs: 18, md: 20, xl: 25 } }}
          />
        </Badge>
      </IconButton>
      <NotificationDropdown anchor={anchor} unread={unread} onClose={() => setAnchor(null)} />
    </>
  );
};

export default NotificationBell;
