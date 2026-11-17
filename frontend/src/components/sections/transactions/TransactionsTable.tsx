import { Chip, Stack, Typography } from '@mui/material';
import { DataGrid, GridColDef, GridPaginationModel } from '@mui/x-data-grid';
import type { Page, Transaction } from 'api/types';
import { formatDateTime } from 'lib/dates';
import { formatMoney, formatRate, formatSignedMoney } from 'lib/money';
import {
  describeTransaction,
  TRANSACTION_STATUS_LABEL,
  TRANSACTION_TYPE_LABEL,
} from 'lib/transactions';
import { useMemo } from 'react';

export const PAGE_SIZE_OPTIONS = [10, 25, 50];

interface TransactionsTableProps {
  accountNumber: string;
  data: Page<Transaction> | undefined;
  loading: boolean;
  paginationModel: GridPaginationModel;
  onPaginationModelChange: (model: GridPaginationModel) => void;
  onRowClick: (transaction: Transaction) => void;
}

/** One page of an account's history. Paging happens on the server (the backend sorts newest first). */
const TransactionsTable = ({
  accountNumber,
  data,
  loading,
  paginationModel,
  onPaginationModelChange,
  onRowClick,
}: TransactionsTableProps) => {
  const columns = useMemo<GridColDef<Transaction>[]>(
    () => [
      {
        field: 'createdAt',
        headerName: 'Date',
        minWidth: 170,
        flex: 1,
        valueFormatter: (value: string) => formatDateTime(value),
      },
      {
        field: 'description',
        headerName: 'Description',
        minWidth: 220,
        flex: 2,
        renderCell: ({ row }) => {
          const { title, counterparty } = describeTransaction(row, accountNumber);
          return (
            <Stack justifyContent="center" sx={{ height: 1, minWidth: 0 }}>
              <Typography noWrap>{title}</Typography>
              {title !== counterparty && (
                <Typography noWrap variant="caption" sx={{ color: 'text.secondary' }}>
                  {counterparty}
                </Typography>
              )}
            </Stack>
          );
        },
      },
      {
        field: 'type',
        headerName: 'Type',
        minWidth: 150,
        flex: 1,
        valueFormatter: (value: Transaction['type']) => TRANSACTION_TYPE_LABEL[value],
      },
      {
        field: 'amount',
        headerName: 'Amount',
        minWidth: 160,
        flex: 1,
        align: 'right',
        headerAlign: 'right',
        renderCell: ({ row }) => {
          const { direction } = describeTransaction(row, accountNumber);
          return (
            <Stack justifyContent="center" alignItems="flex-end" sx={{ height: 1 }}>
              <Typography
                sx={{ fontWeight: 600, color: direction === 'in' ? 'success.main' : 'error.main' }}
              >
                {formatSignedMoney(row.amount, direction, row.currency)}
              </Typography>
              {row.originalCurrency && row.originalAmount !== null && (
                <Typography variant="caption" sx={{ color: 'text.secondary' }}>
                  {formatMoney(row.originalAmount, row.originalCurrency)} @{' '}
                  {formatRate(row.exchangeRate)}
                </Typography>
              )}
            </Stack>
          );
        },
      },
      {
        field: 'status',
        headerName: 'Status',
        minWidth: 120,
        renderCell: ({ row }) => (
          <Chip
            size="small"
            label={TRANSACTION_STATUS_LABEL[row.status]}
            color={
              row.status === 'SUCCESS' ? 'success' : row.status === 'PENDING' ? 'warning' : 'error'
            }
            variant={row.status === 'SUCCESS' ? 'outlined' : 'filled'}
          />
        ),
      },
    ],
    [accountNumber],
  );

  return (
    <DataGrid
      rows={data?.content ?? []}
      columns={columns}
      getRowId={(row) => row.reference}
      rowCount={data?.totalElements ?? 0}
      loading={loading}
      paginationMode="server"
      paginationModel={paginationModel}
      onPaginationModelChange={onPaginationModelChange}
      pageSizeOptions={PAGE_SIZE_OPTIONS}
      onRowClick={({ row }) => onRowClick(row)}
      disableColumnSorting
      disableColumnFilter
      disableRowSelectionOnClick
      // At most 50 rows per page: render them all (also lets tests see every row in jsdom).
      disableVirtualization
      autoHeight
      localeText={{ noRowsLabel: 'No transactions yet' }}
      sx={{ '& .MuiDataGrid-row': { cursor: 'pointer' } }}
    />
  );
};

export default TransactionsTable;
