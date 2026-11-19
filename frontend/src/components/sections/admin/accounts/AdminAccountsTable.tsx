import { Chip, Typography } from '@mui/material';
import { DataGrid, GridColDef } from '@mui/x-data-grid';
import type { Account, UserProfile } from 'api/types';
import { formatDate } from 'lib/dates';
import { formatMoney } from 'lib/money';
import { formatAccountNumber } from 'lib/transactions';
import { useMemo } from 'react';

export const ACCOUNT_TYPE_LABEL: Record<Account['accountType'], string> = {
  SAVINGS: 'Savings',
  CURRENT: 'Current',
};

interface AdminAccountsTableProps {
  accounts: Account[];
  loading: boolean;
  /** userId → user, to show owners by name (as far as they are known). */
  owners: Map<string, UserProfile>;
  onRowClick: (account: Account) => void;
}

/** All accounts of the current filter; paging and sorting in the browser (the backend returns a plain list). */
const AdminAccountsTable = ({ accounts, loading, owners, onRowClick }: AdminAccountsTableProps) => {
  const columns = useMemo<GridColDef<Account>[]>(
    () => [
      {
        field: 'accountNumber',
        headerName: 'Account',
        minWidth: 150,
        flex: 1,
        valueFormatter: (value: string) => formatAccountNumber(value),
      },
      {
        field: 'userId',
        headerName: 'Owner',
        minWidth: 160,
        flex: 1,
        valueGetter: (value: string) => owners.get(value)?.username ?? `${value.slice(0, 8)}…`,
        renderCell: ({ row, value }) => (
          <Typography noWrap title={owners.get(row.userId)?.fullName ?? row.userId}>
            {value as string}
          </Typography>
        ),
      },
      {
        field: 'accountType',
        headerName: 'Type',
        minWidth: 110,
        valueFormatter: (value: Account['accountType']) => ACCOUNT_TYPE_LABEL[value],
      },
      {
        field: 'accountStatus',
        headerName: 'Status',
        minWidth: 110,
        renderCell: ({ row }) => (
          <Chip
            size="small"
            label={row.accountStatus === 'ACTIVE' ? 'Active' : 'Locked'}
            color={row.accountStatus === 'ACTIVE' ? 'success' : 'error'}
            variant={row.accountStatus === 'ACTIVE' ? 'outlined' : 'filled'}
          />
        ),
      },
      {
        field: 'balance',
        headerName: 'Balance',
        type: 'number',
        minWidth: 140,
        flex: 1,
        valueFormatter: (value: number, row) => formatMoney(value, row.accountCurrency),
      },
      {
        field: 'createdAt',
        headerName: 'Opened',
        minWidth: 130,
        valueFormatter: (value: string) => formatDate(value),
      },
    ],
    [owners],
  );

  return (
    <DataGrid
      rows={accounts}
      columns={columns}
      getRowId={(row) => row.accountId}
      loading={loading}
      initialState={{
        pagination: { paginationModel: { page: 0, pageSize: 10 } },
        sorting: { sortModel: [{ field: 'createdAt', sort: 'desc' }] },
      }}
      pageSizeOptions={[10, 25, 50]}
      onRowClick={({ row }) => onRowClick(row)}
      disableColumnFilter
      disableRowSelectionOnClick
      disableVirtualization
      autoHeight
      localeText={{ noRowsLabel: 'No accounts' }}
      sx={{ '& .MuiDataGrid-row': { cursor: 'pointer' } }}
    />
  );
};

export default AdminAccountsTable;
