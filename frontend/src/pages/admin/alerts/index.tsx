import {
  Alert,
  Card,
  Snackbar,
  Stack,
  ToggleButton,
  ToggleButtonGroup,
  Typography,
} from '@mui/material';
import { GridPaginationModel } from '@mui/x-data-grid';
import { useAlerts, useOpenAlertCount, useReviewAlert } from 'api/adminAlerts';
import type { AlertStatus, TransactionAlert } from 'api/types';
import AlertsTable, { ALERT_PAGE_SIZES } from 'components/sections/admin/alerts/AlertsTable';
import { errorMessage } from 'lib/errors';
import { useCallback, useState } from 'react';

type Filter = AlertStatus | 'ALL';

const NO_ROWS: Record<Filter, string> = {
  OPEN: 'No open alerts',
  REVIEWED: 'No reviewed alerts',
  ALL: 'No alerts',
};

/**
 * Admin: transfers flagged by the transaction watch (the alert module reads the transfer events from Kafka):
 * large transfers and bursts of transfers from one account. Open ones first by default; "Mark as reviewed" when
 * an admin has looked at one.
 */
const AdminAlertsPage = () => {
  const [filter, setFilter] = useState<Filter>('OPEN');
  const [pagination, setPagination] = useState<GridPaginationModel>({
    page: 0,
    pageSize: ALERT_PAGE_SIZES[1],
  });
  const [notice, setNotice] = useState<string>();
  const [reviewError, setReviewError] = useState<string>();

  const alerts = useAlerts({
    status: filter === 'ALL' ? undefined : filter,
    page: pagination.page,
    size: pagination.pageSize,
  });
  const openCount = useOpenAlertCount();
  const review = useReviewAlert();

  const onReview = useCallback(
    (alert: TransactionAlert) => {
      setReviewError(undefined);
      review.mutate(alert.id, {
        onSuccess: () => setNotice(`Alert for ${alert.reference} marked as reviewed.`),
        onError: (error) => setReviewError(errorMessage(error)),
      });
    },
    [review],
  );

  const open = openCount.data;

  return (
    <Stack gap={3} sx={{ mb: 3 }}>
      <Stack
        direction={{ xs: 'column', sm: 'row' }}
        justifyContent="space-between"
        alignItems={{ xs: 'flex-start', sm: 'center' }}
        gap={2}
      >
        <Typography sx={{ color: 'text.secondary' }}>
          {open === undefined ? ' ' : open === 1 ? '1 open alert' : `${open} open alerts`}
        </Typography>
        <ToggleButtonGroup
          exclusive
          size="small"
          value={filter}
          onChange={(_, value: Filter | null) => {
            if (!value) return;
            setFilter(value);
            setPagination((current) => ({ ...current, page: 0 }));
          }}
          aria-label="Show alerts"
        >
          <ToggleButton value="OPEN">Open</ToggleButton>
          <ToggleButton value="REVIEWED">Reviewed</ToggleButton>
          <ToggleButton value="ALL">All</ToggleButton>
        </ToggleButtonGroup>
      </Stack>
      {reviewError && (
        <Alert severity="error" onClose={() => setReviewError(undefined)}>
          {reviewError}
        </Alert>
      )}
      {alerts.error ? (
        <Alert severity="error">{errorMessage(alerts.error)}</Alert>
      ) : (
        <Card sx={{ p: { xs: 1, md: 2 } }}>
          <AlertsTable
            data={alerts.data}
            loading={alerts.isFetching}
            pagination={pagination}
            onPaginationChange={setPagination}
            onReview={onReview}
            reviewingId={review.isPending ? review.variables : undefined}
            noRowsLabel={NO_ROWS[filter]}
          />
        </Card>
      )}
      <Snackbar
        open={Boolean(notice)}
        autoHideDuration={5000}
        onClose={() => setNotice(undefined)}
        anchorOrigin={{ vertical: 'bottom', horizontal: 'center' }}
      >
        <Alert severity="success" variant="filled" onClose={() => setNotice(undefined)}>
          {notice}
        </Alert>
      </Snackbar>
    </Stack>
  );
};

export default AdminAlertsPage;
