import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { apiFetch } from './client';
import { queryKeys } from './queryKeys';
import type { AppNotification, Page } from './types';

/**
 * How often the header bell asks for the unread count. Notifications are created a second or two after the money
 * moves (Kafka), so polling every 20 s is plenty for a demo; push (SSE / WebSocket) would be the next step.
 */
export const NOTIFICATION_POLL_MS = 20_000;

/** GET /api/notifications?page&size: the signed-in user's own notifications, newest first. */
export const getNotifications = (page: number, size: number): Promise<Page<AppNotification>> =>
  apiFetch<Page<AppNotification>>(`/api/notifications?page=${page}&size=${size}`);

/** GET /api/notifications/unread-count */
export const getUnreadNotificationCount = async (): Promise<number> =>
  (await apiFetch<{ unread: number }>('/api/notifications/unread-count')).unread;

/** PATCH /api/notifications/{id}/read → 204. */
export const markNotificationRead = (id: number): Promise<void> =>
  apiFetch<void>(`/api/notifications/${id}/read`, { method: 'PATCH' });

/** POST /api/notifications/read-all → how many were unread. */
export const markAllNotificationsRead = async (): Promise<number> =>
  (await apiFetch<{ marked: number }>('/api/notifications/read-all', { method: 'POST' })).marked;

/**
 * The number on the bell, polled while the page is visible (React Query pauses polling in a background tab).
 * `pollMs` is only changed by tests.
 */
export const useUnreadNotificationCount = (pollMs: number = NOTIFICATION_POLL_MS) =>
  useQuery({
    queryKey: queryKeys.notifications.unreadCount(),
    queryFn: getUnreadNotificationCount,
    refetchInterval: pollMs,
  });

/** The latest notifications for the dropdown; loaded only while it is open, fresh every time it opens. */
export const useRecentNotifications = (enabled: boolean, size = 10) =>
  useQuery({
    queryKey: queryKeys.notifications.recent(size),
    queryFn: () => getNotifications(0, size),
    enabled,
    staleTime: 0,
  });

/** Both mark-as-read mutations refresh the count and the list. */
const useInvalidateNotifications = () => {
  const queryClient = useQueryClient();
  return () => queryClient.invalidateQueries({ queryKey: queryKeys.notifications.all });
};

export const useMarkNotificationRead = () => {
  const invalidate = useInvalidateNotifications();
  return useMutation({ mutationFn: markNotificationRead, onSuccess: invalidate });
};

export const useMarkAllNotificationsRead = () => {
  const invalidate = useInvalidateNotifications();
  return useMutation({ mutationFn: markAllNotificationsRead, onSuccess: invalidate });
};
