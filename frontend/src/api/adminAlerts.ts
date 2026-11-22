import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { apiFetch } from './client';
import { queryKeys } from './queryKeys';
import type { AlertStatus, Page, TransactionAlert } from './types';

export interface AlertListParams {
  /** Absent = all alerts. */
  status?: AlertStatus;
  page: number;
  /** The backend allows 1–100. */
  size: number;
}

/** GET /api/admin/alerts?status&page&size (ADMIN only): newest first. */
export const listAlerts = ({
  status,
  page,
  size,
}: AlertListParams): Promise<Page<TransactionAlert>> =>
  apiFetch<Page<TransactionAlert>>(
    `/api/admin/alerts?page=${page}&size=${size}${status ? `&status=${status}` : ''}`,
  );

/** GET /api/admin/alerts/open-count */
export const getOpenAlertCount = async (): Promise<number> =>
  (await apiFetch<{ open: number }>('/api/admin/alerts/open-count')).open;

/** PATCH /api/admin/alerts/{id}/review: reviewed by the signed-in admin; already reviewed → unchanged. */
export const reviewAlert = (id: number): Promise<TransactionAlert> =>
  apiFetch<TransactionAlert>(`/api/admin/alerts/${id}/review`, { method: 'PATCH' });

/** Alerts are raised a moment after a transfer (Kafka), so the list is re-fetched every 30 s while open. */
export const ALERTS_REFRESH_MS = 30_000;

export const useAlerts = (params: AlertListParams) =>
  useQuery({
    queryKey: queryKeys.admin.alerts.list(params),
    queryFn: () => listAlerts(params),
    placeholderData: keepPreviousData,
    refetchInterval: ALERTS_REFRESH_MS,
  });

export const useOpenAlertCount = () =>
  useQuery({
    queryKey: queryKeys.admin.alerts.openCount(),
    queryFn: getOpenAlertCount,
    refetchInterval: ALERTS_REFRESH_MS,
  });

/** Marks one alert as reviewed, then re-fetches the lists and the open count. */
export const useReviewAlert = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: reviewAlert,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: queryKeys.admin.alerts.all }),
  });
};
