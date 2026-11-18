import { Chip, Typography } from '@mui/material';
import { DataGrid, GridColDef, GridPaginationModel, GridSortModel } from '@mui/x-data-grid';
import type { SortDirection, UserSortField } from 'api/adminUsers';
import type { PageResult, UserProfile } from 'api/types';
import { formatDate } from 'lib/dates';
import { useMemo } from 'react';

export const USER_PAGE_SIZES = [10, 20, 50];

/** DataGrid column → backend sort field (UserSortField.java). */
const SORT_FIELD: Record<string, UserSortField> = {
  username: 'USERNAME',
  fullName: 'FULL_NAME',
  email: 'EMAIL',
  role: 'ROLE',
  createdAt: 'CREATED_AT',
};

export const ROLE_LABEL: Record<UserProfile['role'], string> = {
  CUSTOMER: 'Customer',
  ADMIN: 'Administrator',
};

export interface UserSort {
  sortBy: UserSortField;
  direction: SortDirection;
}

interface UsersTableProps {
  data: PageResult<UserProfile> | undefined;
  loading: boolean;
  currentUserId: string | undefined;
  pagination: GridPaginationModel;
  onPaginationChange: (model: GridPaginationModel) => void;
  sort: UserSort;
  onSortChange: (sort: UserSort) => void;
  onRowClick: (user: UserProfile) => void;
}

/** One page of users; paging and sorting happen on the server. */
const UsersTable = ({
  data,
  loading,
  currentUserId,
  pagination,
  onPaginationChange,
  sort,
  onSortChange,
  onRowClick,
}: UsersTableProps) => {
  const columns = useMemo<GridColDef<UserProfile>[]>(
    () => [
      {
        field: 'username',
        headerName: 'Username',
        minWidth: 150,
        flex: 1,
        renderCell: ({ row }) => (
          <Typography noWrap sx={{ lineHeight: 'inherit' }}>
            {row.username}
            {row.id === currentUserId && (
              <Typography component="span" sx={{ color: 'text.secondary', ml: 0.75 }}>
                (you)
              </Typography>
            )}
          </Typography>
        ),
      },
      { field: 'fullName', headerName: 'Full name', minWidth: 170, flex: 1.2 },
      { field: 'email', headerName: 'Email', minWidth: 220, flex: 1.5 },
      {
        field: 'role',
        headerName: 'Role',
        minWidth: 140,
        renderCell: ({ row }) => (
          <Chip
            size="small"
            label={ROLE_LABEL[row.role]}
            color={row.role === 'ADMIN' ? 'primary' : 'default'}
            variant={row.role === 'ADMIN' ? 'filled' : 'outlined'}
          />
        ),
      },
      {
        field: 'createdAt',
        headerName: 'Joined',
        minWidth: 130,
        valueFormatter: (value: string) => formatDate(value),
      },
    ],
    [currentUserId],
  );

  const sortField = Object.keys(SORT_FIELD).find((field) => SORT_FIELD[field] === sort.sortBy);
  const sortModel: GridSortModel = sortField
    ? [{ field: sortField, sort: sort.direction === 'ASC' ? 'asc' : 'desc' }]
    : [];

  return (
    <DataGrid
      rows={data?.content ?? []}
      columns={columns}
      rowCount={data?.totalElements ?? 0}
      loading={loading}
      paginationMode="server"
      paginationModel={pagination}
      onPaginationModelChange={onPaginationChange}
      pageSizeOptions={USER_PAGE_SIZES}
      sortingMode="server"
      sortModel={sortModel}
      sortingOrder={['asc', 'desc']}
      onSortModelChange={(model) => {
        const item = model[0];
        if (!item) return;
        onSortChange({
          sortBy: SORT_FIELD[item.field],
          direction: item.sort === 'desc' ? 'DESC' : 'ASC',
        });
      }}
      onRowClick={({ row }) => onRowClick(row)}
      disableColumnFilter
      disableRowSelectionOnClick
      disableVirtualization
      autoHeight
      localeText={{ noRowsLabel: 'No users' }}
      sx={{ '& .MuiDataGrid-row': { cursor: 'pointer' } }}
    />
  );
};

export default UsersTable;
