import { Button, Chip, Link, Tooltip, Typography } from '@mui/material';
import { DataGrid, GridColDef, GridPaginationModel } from '@mui/x-data-grid';
import type { AlertRule, Page, TransactionAlert } from 'api/types';
import { formatDateTime } from 'lib/dates';
import { formatMoney } from 'lib/money';
import { formatAccountNumber } from 'lib/transactions';
import { useMemo } from 'react';
import { Link as RouterLink } from 'react-router-dom';
import { adminAccountHistoryPath } from 'routes/path';

export const ALERT_PAGE_SIZES = [10, 20, 50];

export const ALERT_RULE_LABEL: Record<AlertRule, string> = {
  LARGE_TRANSFER: 'Large transfer',
  RAPID_TRANSFERS: 'Rapid transfers',
};

interface AlertsTableProps {
  data: Page<TransactionAlert> | undefined;
  loading: boolean;
  pagination: GridPaginationModel;
  onPaginationChange: (model: GridPaginationModel) => void;
  onReview: (alert: TransactionAlert) => void;
  /** The alert whose review is being sent; its button is disabled meanwhile. */
  reviewingId: number | undefined;
  noRowsLabel: string;
}

/** One page of alerts, newest first; paging happens on the server. */
const AlertsTable = ({
  data,
  loading,
  pagination,
  onPaginationChange,
  onReview,
  reviewingId,
  noRowsLabel,
}: AlertsTableProps) => {
  const columns = useMemo<GridColDef<TransactionAlert>[]>(
    () => [
      {
        field: 'createdAt',
        headerName: 'Time',
        minWidth: 170,
        sortable: false,
        valueFormatter: (value: string) => formatDateTime(value),
      },
      {
        field: 'rule',
        headerName: 'Rule',
        minWidth: 150,
        sortable: false,
        renderCell: ({ row }) => (
          <Chip
            size="small"
            label={ALERT_RULE_LABEL[row.rule] ?? row.rule}
            color={row.rule === 'LARGE_TRANSFER' ? 'warning' : 'error'}
            variant="outlined"
          />
        ),
      },
      {
        field: 'accountNumber',
        headerName: 'From account',
        minWidth: 150,
        sortable: false,
        renderCell: ({ row }) => (
          <Link
            component={RouterLink}
            to={adminAccountHistoryPath(row.accountNumber)}
            underline="hover"
          >
            {formatAccountNumber(row.accountNumber)}
          </Link>
        ),
      },
      {
        field: 'detail',
        headerName: 'Why',
        minWidth: 260,
        flex: 2,
        sortable: false,
        renderCell: ({ row }) => (
          <Typography
            noWrap
            title={`${row.detail} · ${row.reference}`}
            sx={{ lineHeight: 'inherit' }}
          >
            {row.detail}
          </Typography>
        ),
      },
      {
        field: 'status',
        headerName: 'Status',
        minWidth: 170,
        sortable: false,
        renderCell: ({ row }) =>
          row.status === 'OPEN' ? (
            <Button
              size="small"
              variant="outlined"
              onClick={() => onReview(row)}
              disabled={reviewingId === row.id}
              aria-label={`Mark alert ${row.reference} as reviewed`}
            >
              Mark as reviewed
            </Button>
          ) : (
            <Tooltip title={`Reviewed ${formatDateTime(row.reviewedAt)}`}>
              <Chip size="small" label="Reviewed" color="success" variant="outlined" />
            </Tooltip>
          ),
      },
      // Last, so the right-aligned amounts don't run into the next column.
      {
        field: 'amount',
        headerName: 'Amount',
        type: 'number',
        minWidth: 140,
        flex: 1,
        sortable: false,
        valueFormatter: (value: number) => formatMoney(value),
      },
    ],
    [onReview, reviewingId],
  );

  return (
    <DataGrid
      rows={data?.content ?? []}
      columns={columns}
      rowCount={data?.totalElements ?? 0}
      loading={loading}
      paginationMode="server"
      paginationModel={pagination}
      onPaginationModelChange={onPaginationChange}
      pageSizeOptions={ALERT_PAGE_SIZES}
      disableColumnFilter
      disableColumnMenu
      disableRowSelectionOnClick
      disableVirtualization
      autoHeight
      localeText={{ noRowsLabel }}
    />
  );
};

export default AlertsTable;
