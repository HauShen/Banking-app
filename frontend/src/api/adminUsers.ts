import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { apiFetch } from './client';
import { queryKeys } from './queryKeys';
import type { PageResult, Role, UserProfile } from './types';

/** UserSortField.java */
export type UserSortField = 'USERNAME' | 'FULL_NAME' | 'EMAIL' | 'ROLE' | 'CREATED_AT';
export type SortDirection = 'ASC' | 'DESC';

export interface UserListParams {
  page: number;
  /** The backend caps it at 100. */
  size: number;
  sortBy: UserSortField;
  direction: SortDirection;
}

/** GET /api/admin/users (ADMIN only). */
export const listUsers = ({ page, size, sortBy, direction }: UserListParams) =>
  apiFetch<PageResult<UserProfile>>(
    `/api/admin/users?page=${page}&size=${size}&sortBy=${sortBy}&direction=${direction}`,
  );

export const useUsers = (params: UserListParams) =>
  useQuery({
    queryKey: queryKeys.admin.users.list(params),
    queryFn: () => listUsers(params),
    placeholderData: keepPreviousData,
  });

/** UserProfileRequestBody.java */
export interface NewAdmin {
  username: string;
  fullName: string;
  email: string;
  password: string;
}

/** POST /api/admin/users → 201: a new ADMIN account. */
export const createAdmin = (admin: NewAdmin) =>
  apiFetch<UserProfile>('/api/admin/users', { method: 'POST', body: admin });

/** PATCH /api/admin/users/{id}/role. 409 for your own role or the last admin. */
export const changeRole = ({ id, role }: { id: string; role: Role }) =>
  apiFetch<UserProfile>(`/api/admin/users/${encodeURIComponent(id)}/role`, {
    method: 'PATCH',
    body: { role },
  });

/** DELETE /api/admin/users/{id} → 204. 409 for yourself or the last admin. */
export const deleteUser = (id: string) =>
  apiFetch<void>(`/api/admin/users/${encodeURIComponent(id)}`, { method: 'DELETE' });

/** Mutations that change the user list re-fetch every page of it afterwards. */
const useUserMutation = <TArgs, TResult>(mutationFn: (args: TArgs) => Promise<TResult>) => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn,
    onSuccess: () => void queryClient.invalidateQueries({ queryKey: queryKeys.admin.users.all }),
  });
};

export const useCreateAdmin = () => useUserMutation(createAdmin);
export const useChangeRole = () => useUserMutation(changeRole);
export const useDeleteUser = () => useUserMutation(deleteUser);
